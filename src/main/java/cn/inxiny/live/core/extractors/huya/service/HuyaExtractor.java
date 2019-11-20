package cn.inxiny.live.core.extractors.huya.service;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.inxiny.live.core.extractors.Extractor;
import cn.inxiny.live.core.exception.NullRoomNumberException;
import cn.inxiny.live.core.extractors.Live;
import cn.inxiny.live.core.extractors.Platform;
import cn.inxiny.live.utils.JsonUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
        String html = Jsoup.connect("https://www.huya.com/" + room).get().html();
        if (!html.contains("roomTitle")) {
            throw new NullRoomNumberException();
        }

        // 获取状态
        if (getState(html)){
            //  获取流
            live = getStream(html);
        }

        return live;
    }

    /**
     * 获取状态
     * @param html
     * @return
     */
    public boolean getState (String html) {
        boolean state = false;
        Matcher TT_ROOM_DATA = Pattern.compile("\"state\":\"(\\w{2,6})").matcher(html);
        if (TT_ROOM_DATA.find()) {
            if ("ON".equals(TT_ROOM_DATA.group(1))){
                state = true;
            }
        }
        return state;
    }

    /**
     * 获取流
     * @param html
     */
    public Live getStream (String html) throws IOException {
        Live live = new Live(Platform.HUYA);
        Matcher streamMatcher = Pattern.compile("\"stream\":(.*)(\\s\\W)").matcher(html);
        String result = "";
        if (streamMatcher.find()) {
            result = streamMatcher.group(1);
        }
        Map<String, Object> json = JsonUtils.readJson2Map(result, Map.class, String.class, Object.class);
        List<Map> definitions = (List)json.get("vMultiStreamInfo");     // 清晰度
        List<Map> data = (List)json.get("data");                // 线路和房间基本信息
        List<Map> line = (List)data.get(0).get("gameStreamInfoList");   //  线路
        Map info = (Map)data.get(0).get("gameLiveInfo");   //  基本信息
        Map path = line.get(0);

        String sStreamName = path.get("sStreamName").toString();
        String sHlsUrlSuffix = path.get("sHlsUrlSuffix").toString();     // m3u8
        String sHlsUrl = path.get("sHlsUrl").toString();
        String sFlvUrlSuffix = path.get("sFlvUrlSuffix").toString();     // flv
        String sFlvUrl = path.get("sFlvUrl").toString();

        String sFlvAntiCode = path.get("sFlvAntiCode").toString();  //  flv验证
        String iLineIndex = path.get("iLineIndex").toString();      //  当前线路

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

        Matcher roomInfoMatcher = Pattern.compile(">公告.*>(.*)(</s)").matcher(html);
        if (roomInfoMatcher.find()) {
            live.setRoomInfo(roomInfoMatcher.group(1));
        }

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
