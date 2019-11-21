package cn.inxiny.live.core.extractors.douyu.service;

import cn.hutool.core.date.DateUtil;
import cn.inxiny.live.core.exception.NullRoomNumberException;
import cn.inxiny.live.core.extractors.Extractor;
import cn.inxiny.live.core.extractors.Live;
import cn.inxiny.live.core.extractors.Platform;
import cn.inxiny.live.utils.HttpUtils;
import cn.inxiny.live.utils.JsonUtils;
import cn.inxiny.live.utils.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("douyuExtractor")
public class DouyuExtractor implements Extractor {

    @Autowired
    private RedisUtil redisUtil;

    public Live extract (String room) throws IOException, ScriptException, NoSuchMethodException {
        Live live = new Live(Platform.DOUYU);

        String signjs = getSignJS(room);
        String sign = getSign(room,signjs);
        String stream = getStream(room, sign);
        live.setLink(stream);

        return live;
    }

    public String getSignJS (String room) throws IOException {
        String signjs = redisUtil.get(this.getClass().getName() + ":room=" + room + ":getSign.js");
        if (StringUtils.isNotEmpty(signjs)){
            return signjs;
        }

        String html = HttpUtils.sendGet("https://www.douyu.com/swf_api/homeH5Enc", "rids=" + room);
        // 获取加密 JS
        Map<String, Object> stringObjectMap = JsonUtils.readJson2Map(html, Map.class, String.class, Object.class);
        Map data = (Map) stringObjectMap.get("data");
        Object o = data.get("room" + room);
        if (o == null){
            new NullRoomNumberException();
        }
        signjs = o.toString();

        redisUtil.set(this.getClass().getName()+":room="+ room + ":getSign.js", signjs);
        return signjs;
    }

    public String getSign (String room, String signjs) throws FileNotFoundException, ScriptException, NoSuchMethodException {
        Object sign = "";

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/crypto-js.min.js"));
        engine.eval(signjs);
        engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/hello.js"));
        Invocable invocable = (Invocable) engine;
        sign = invocable.invokeFunction("getSign", room,"d063f90e47753de1b979189300081501",new Date().getTime()/1000);
        return sign.toString();
    }

    public String getStream (String room,String sign) throws IOException {
        String stream = "";
        String result = HttpUtils.sendPost("https://www.douyu.com/lapi/live/getH5Play/"+room, sign);
        Map<String, Object> stringObjectMap = JsonUtils.readJson2Map(result, Map.class, String.class, Object.class);
        if ("0".equals(stringObjectMap.get("error").toString())){
            Map data = (Map)stringObjectMap.get("data");
            String rtmp_live = data.get("rtmp_live").toString();
            Matcher matcher = Pattern.compile("^[0-9a-zA-Z]*").matcher(rtmp_live);
            if (matcher.find()) {
                String group = matcher.group(0);
                stream = "http://tx2play1.douyucdn.cn/live/" + group + ".flv?uuid=";
            }
        }
        return stream;
    }

    public String getStreamOnH5 (String room) throws IOException, ScriptException, NoSuchMethodException {
        String streamOnH5 = "";

        String pre_url = getPre_url(room);
        if (StringUtils.isNotEmpty(pre_url)){
            streamOnH5 = "http://tx2play1.douyucdn.cn/live/" + pre_url + ".flv?uuid=";
        } else {
            String[] strArray = getHomeJS(room);
            String homejs = strArray[0];
            String rid = strArray[1];
            Date date = new Date();
            String tt = String.valueOf(date.getTime() / 1000);
            String post_v = DateUtil.format(date, "yyyyMMdd");
            String homeSing = getHomeSign(rid, post_v, tt, homejs);
            String homeSignUrl = getHomeSignUrl(rid, post_v, tt, homeSing);
            if (!"0".equals(homeSignUrl)){
                streamOnH5 = "http://tx2play1.douyucdn.cn/live/" + homeSignUrl + ".flv?uuid=";
            } else {
                // 未开播
            }
        }

        return streamOnH5;
    }

    public String getPre_url (String room) throws IOException {
        String pre_url = "";

        String request_url = "https://playweb.douyucdn.cn/lapi/live/hlsH5Preview/" + room;
        String tt = String.valueOf(new Date().getTime());
        String md5Early = room + tt;
        String auth = DigestUtils.md5DigestAsHex(md5Early.getBytes());

        HashMap<String, String> headerMap = new HashMap<>();
        HashMap<String, String> contentMap = new HashMap<>();

        headerMap.put("content-type","application/x-www-form-urlencoded");
        headerMap.put("rid",room);
        headerMap.put("time",tt);
        headerMap.put("auth",auth);

        contentMap.put("rid",room);
        contentMap.put("did","10000000000000000000000000001501");

        String postResult = HttpUtils.postMap(request_url, headerMap,contentMap);
        Map<String, Object> objectObjectMap = JsonUtils.readJson2Map(postResult,Map.class,String.class,Object.class);
        String error = objectObjectMap.get("error").toString();
        if ("0".equals(error)){
            String rtmp_live = ((Map)objectObjectMap.get("data")).get("rtmp_live").toString();
            if (rtmp_live.contains("mix=1")){

            } else {
                Matcher matcher = Pattern.compile("^[0-9a-zA-Z]*").matcher(rtmp_live);
                if (matcher.find()) {
                    pre_url = matcher.group(0);
                }
            }
        }

        return postResult;
//        return pre_url;
    }

    public String[] getHomeJS (String room) throws IOException {
        String homejs = "";
        String rid = "";

        Document exam = Jsoup.connect("https://m.douyu.com/" + room).get();
        String html = exam.html();
        // 获取加密 JS
        Matcher TT_ROOM_DATA = Pattern.compile("\"rid\":(\\d{1,7})").matcher(html);
        if (TT_ROOM_DATA.find()) {
            rid = TT_ROOM_DATA.group(1);
            if (!room.equals(rid)){
                exam = Jsoup.connect("https://m.douyu.com/" + rid).get();
                html = exam.html();
            }
        }

        TT_ROOM_DATA = Pattern.compile("(function ub9.*)[\\s\\S](var.*)").matcher(html);
        if (TT_ROOM_DATA.find()) {
            String group1 = TT_ROOM_DATA.group(1);
            String group2 = TT_ROOM_DATA.group(2);
            group1 = group1.replaceAll("eval.*;}", "strc;}");
            homejs = group1 + group2;
        }

        String[] strArray= {homejs,rid};
        return strArray;
    }

    public String getHomeSign (String rid,String post_v,String tt,String homejs) throws ScriptException, NoSuchMethodException {
        String sign = "";

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.eval(homejs);
        Invocable invocable = (Invocable) engine;
        String ub98484234 = invocable.invokeFunction("ub98484234").toString();
        ub98484234 = ub98484234.replaceAll("\\(function[\\s\\S]*toString\\(\\)", "'");
        String md5Early = rid + "10000000000000000000000000001501" + tt + "2501" + post_v;
        String md5rb = DigestUtils.md5DigestAsHex(md5Early.getBytes());
        String singjs = "function get_sign(){var rb='" + md5rb + ub98484234;
        singjs = singjs.replaceAll("return rt;}[\\s\\S]*","return re;};");
        singjs = singjs.replaceAll("\"v=.*&sign=\"\\+","");
        engine.eval(singjs);
        Invocable invocableSing = (Invocable) engine;
        sign = invocableSing.invokeFunction("get_sign",rid,"10000000000000000000000000001501",tt).toString();

        return sign;
    }

    public String getHomeSignUrl (String rid,String post_v,String tt,String sign) throws IOException {
        String signurl = "";
        String request_url = "https://m.douyu.com/api/room/ratestream";

        HashMap<String, String> headerMap = new HashMap<>();
        HashMap<String, String> contentMap = new HashMap<>();

        headerMap.put("content-type","application/x-www-form-urlencoded");
        headerMap.put("user-agent","Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Mobile Safari/537.36");

        contentMap.put("v","2501" + post_v);
        contentMap.put("did","10000000000000000000000000001501");
        contentMap.put("tt",tt);
        contentMap.put("sign",sign);
        contentMap.put("ver","219032101");
        contentMap.put("rid",rid);
        contentMap.put("rate","-1");

        String postResult = HttpUtils.postMap(request_url, headerMap,contentMap);

        Map<String, Object> objectObjectMap = JsonUtils.readJson2Map(postResult,Map.class,String.class,Object.class);
        String code = objectObjectMap.get("code").toString();
        if ("0".equals(code)){
            String url = ((Map)objectObjectMap.get("data")).get("url").toString();
            if (url.contains("mix=1")){

            } else {
                Matcher matcher = Pattern.compile("live/(\\d{1,8}[0-9a-zA-Z]+)_?[\\d]{0,4}/playlist").matcher(url);
                if (matcher.find()) {
                    signurl = matcher.group(1);
                }
            }
        } else {
            signurl = "0";
        }

        return signurl;
    }


}
