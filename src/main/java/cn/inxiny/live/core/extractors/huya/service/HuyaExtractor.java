package cn.inxiny.live.core.extractors.huya.service;

import cn.inxiny.live.core.Extractor;
import cn.inxiny.live.core.exception.NullRoomNumberException;
import cn.inxiny.live.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("huyaExtractor")
public class HuyaExtractor implements Extractor {

    public List extract (String room) throws IOException {
        Map map = new HashMap();
        Document exam = Jsoup.connect("https://www.huya.com/" + room).get();

        Element j_roomTitle = exam.getElementById("J_roomTitle");
//        if (j_roomTitle == null) {
//            throw new NullRoomNumberException();
//        }
        String title = j_roomTitle.text();
        String state = "";

        String html = exam.html();

        // 获取状态
        Matcher TT_ROOM_DATA = Pattern.compile("\\{\\\"type\\\"(.*)\\}").matcher(html);
        while (TT_ROOM_DATA.find()) {
            String group = TT_ROOM_DATA.group();
            String s = StringUtils.substringBefore(group, "}") + "}";
            Map<String, Object> ROOM_DATA = JsonUtils.readJson2Map(s, Map.class, String.class, Object.class);
            state = ROOM_DATA.get("state").toString();
        }

        if (state.equals("ON")){

        } else {

        }

        switch (state){
            case "ON":
                map.put("live",getStream(html));
                state = "正在直播";
                break;
            case "OFF":
                state = "未开播";
                break;
            case "REPLAY":
                state = "重播";
                break;
        }

        map.put("title",title);
        map.put("state",state);

        List list = new ArrayList();
        list.add(map);

        return list;
    }

    /**
     * 获取流
     * @param html
     */
    public Map getStream (String html) throws IOException {
        Map live = new HashMap();
        Matcher m = Pattern.compile("\\{\\\"status\\\"(.*)\\}").matcher(html);
        List<String> result = new ArrayList<String>();
        while (m.find()) {
            result.add(m.group());
        }
        String s = result.get(0);
        s = s.substring(0,s.length()-1);
        Map<String, Object> json = JsonUtils.readJson2Map(s, Map.class, String.class, Object.class);
        List<Map> vMu = (List)json.get("vMultiStreamInfo");
        List<Map> data = (List)json.get("data");
        List<Map> jarr = (List)data.get(0).get("gameStreamInfoList");
        for (Map jar : jarr) {
            List listM3U8 = new ArrayList();
            List listFLV = new ArrayList();
            Map streamInfoM3U8 = new HashMap();
            Map streamInfoFLV = new HashMap();
            String sHlsUrl = jar.get("sHlsUrl").toString();
            String sStreamName = jar.get("sStreamName").toString();
            String sHlsUrlSuffix = jar.get("sHlsUrlSuffix").toString();
            String sFlvUrl = jar.get("sFlvUrl").toString();
            String sFlvUrlSuffix = jar.get("sFlvUrlSuffix").toString();
            String sFlvAntiCode = jar.get("sFlvAntiCode").toString();
            String iLineIndex = jar.get("iLineIndex").toString();
            for (Map vM : vMu) {
                if ("0".equals(vM.get("iBitRate").toString())){
                    streamInfoM3U8.put(vM.get("sDisplayName"),sHlsUrl+"/"+sStreamName + "." + sHlsUrlSuffix);
                    streamInfoFLV.put(vM.get("sDisplayName"), sFlvUrl+"/"+sStreamName + "." + sFlvUrlSuffix + "?" + sFlvAntiCode.replace("&amp;","&"));
                } else {
                    streamInfoM3U8.put(vM.get("sDisplayName"),sHlsUrl+"/"+sStreamName+ "_" + vM.get("iBitRate") + "." + sHlsUrlSuffix);
                    streamInfoFLV.put(vM.get("sDisplayName"), sFlvUrl+"/"+sStreamName + "." + sFlvUrlSuffix + "?" + sFlvAntiCode.replace("&amp;","&") + "&ratio=" + vM.get("iBitRate"));
                }
            }
            listM3U8.add(streamInfoM3U8);
            listFLV.add(streamInfoFLV);

            Map protocol = new HashMap();
            protocol.put("M3U8",streamInfoM3U8);
            protocol.put("FLV",streamInfoFLV);
            live.put("直播线路"+iLineIndex,protocol);
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
