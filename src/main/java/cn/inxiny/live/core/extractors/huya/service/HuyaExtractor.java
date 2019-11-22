package cn.inxiny.live.core.extractors.huya.service;

import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.inxiny.live.core.extractors.Extractor;
import cn.inxiny.live.core.exception.NullRoomNumberException;
import cn.inxiny.live.core.extractors.Live;
import cn.inxiny.live.core.extractors.Platform;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("huyaExtractor")
public class HuyaExtractor implements Extractor {

    public Live extract (String room) throws IOException {
        Live live = new Live(Platform.HUYA);
        String html = HttpRequest.get("https://www.huya.com/" + room).execute().body();

        if (getState(html)){
            getStream(html,live);
        }

        return live;
    }

    private boolean getState (String html) {
        if (!html.contains("roomTitle")) {
            throw new NullRoomNumberException();
        }
        String re = ReUtil.get("\"state\":\"(\\w{2,6})", html, 1);
        boolean state = "ON".equals(re);
        return state;
    }

    /**
     * 获取流
     * @param html
     */
    public Live getStream (String html,Live live) throws IOException {
        String result = ReUtil.get("\"stream\":(.*)(\\s\\W)",html,1);

        JSONObject parse = JSONUtil.parseObj(result);
//        parse.getJSONArray("vMultiStreamInfo");     // 清晰度
        String data = parse.getJSONArray("data").get(0).toString();
        JSONObject dataInfo = JSONUtil.parseObj(data);
        JSONArray lineList = dataInfo.getJSONArray("gameStreamInfoList");  // 线路
        JSONObject path = lineList.getJSONObject(0);
        JSONObject info = dataInfo.getJSONObject("gameLiveInfo");   //  基本信息

        String sStreamName = path.getStr("sStreamName");
        String sHlsUrlSuffix = path.getStr("sHlsUrlSuffix");     // m3u8
        String sHlsUrl = path.getStr("sHlsUrl");
        String sFlvUrlSuffix = path.getStr("sFlvUrlSuffix");     // flv
        String sFlvUrl = path.getStr("sFlvUrl");

        String sFlvAntiCode = path.getStr("sFlvAntiCode");  //  flv验证
        String iLineIndex = path.getStr("iLineIndex");      //  当前线路

        // 获取最高清晰度
        String m3u8 = sHlsUrl + "/" + sStreamName + "." + sHlsUrlSuffix;    // _iBitRate
        String flv = sFlvUrl + "/" + sStreamName + "." + sFlvUrlSuffix + "?" + sFlvAntiCode.replace("&amp;", "&");  // &ratio=iBitRate

        live.setLink(m3u8);
        live.setRoomId(info.get("profileRoom").toString());
        live.setRoomName(info.get("introduction").toString());
        live.setRoomImg(info.get("screenshot").toString());
        live.setOnline(true);
        live.setLastTime(new Date(Long.parseLong(info.get("startTime").toString() + "000")));

        live.setOwnerName(info.get("nick").toString());
        live.setOwnerAvatar(info.get("avatar180").toString());
        live.setPrivateHost(info.get("privateHost").toString());

        Pattern compile = Pattern.compile(">公告.*>([\\s\\S]*)(</s.*\\n</)");
        String roomInfo = ReUtil.get(compile, html, 1);
        live.setRoomInfo(roomInfo);

        return live;
    }

    /**
     * 虎牙所有直播
     * @return
     */
    public String gainHuYaJson ( ) throws IOException {
        
        int page = 0;//页数
        // 获取页数
        Document exam = Jsoup.connect("https://www.huya.com/l").get();
        Elements links = exam.getElementsByClass("list-page");//可以遍历到人数和主播昵称
        page = Integer.parseInt(links.attr("data-pages"));

        //  获取详情
        String text = "";
        String url="https://www.huya.com/";
        Connection connect = Jsoup.connect(url).timeout(20000);
        Map header = new HashMap();
        header.put("m", "LiveList");
        header.put("do", "getLiveListByPage");
        header.put("tagAll", "0");
        for (int i = 0; i < 1; i++) {
            header.put("page", 1+i+"");
            Connection data = connect.data(header);
            Document doc = data.get(); // 获取json字符串
            text = doc.text();
        }

        return text;
    }


}
