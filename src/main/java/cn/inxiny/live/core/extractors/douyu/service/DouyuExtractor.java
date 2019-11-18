package cn.inxiny.live.core.extractors.douyu.service;

import cn.inxiny.live.core.Extractor;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("douyuExtractor")
public class DouyuExtractor implements Extractor {

    @Autowired
    private RedisUtil redisUtil;

    public Map extract (String room) throws IOException, ScriptException, NoSuchMethodException {
        String result = "";

        String signjs = redisUtil.get(this.getClass().getName() + ":room=" + room + ":getSign.js");
        if (StringUtils.isEmpty(signjs)){
            signjs = getSignJS(room);
            redisUtil.set(this.getClass().getName()+":room="+ room + ":getSign.js", signjs);
        }
        String sign = getSign(room,signjs);

        result = HttpUtils.sendPost("https://www.douyu.com/lapi/live/getH5Play/"+room, sign);

        Map map = new HashMap();
        map.put("state","Hello");
        Map<String, Object> stringObjectMap = JsonUtils.readJson2Map(result, Map.class, String.class, Object.class);
        map.put("result",stringObjectMap);
        return map;
    }

    public String getSign (String room, String signjs) throws FileNotFoundException, ScriptException, NoSuchMethodException {
        Object sign = "";

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/core.min.js"));
        engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/md5.min.js"));
        engine.eval(signjs);
        engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/hello.js"));
        Invocable invocable = (Invocable) engine;
        sign = invocable.invokeFunction("getSign", room,"d063f90e47753de1b979189300081501",new Date().getTime()/1000);
        return sign.toString();
    }

    public String getSignJS (String room) throws IOException, ScriptException, NoSuchMethodException {
        String signjs = "";

        Document exam = Jsoup.connect("https://www.douyu.com/" + room).get();
        String html = exam.html();
        // 获取加密 JS
        Matcher TT_ROOM_DATA = Pattern.compile("var vdw(.*)").matcher(html);
        while (TT_ROOM_DATA.find()) {
            String group = TT_ROOM_DATA.group();
            signjs = StringUtils.substringBefore(group, "</script>");
        }
        return signjs;
    }

    public String hlsH5Preview (String room) throws IOException {
        String result = "";
        String real_url = "";

        String request_url = "https://playweb.douyucdn.cn/lapi/live/hlsH5Preview/" + room;
        String tt = String.valueOf(new Date().getTime());
        String md5Early = room + tt;
        String auth = DigestUtils.md5DigestAsHex(md5Early.getBytes());

        System.out.println(md5Early);
        System.out.println(auth);
        System.out.println(room);
        System.out.println(tt);

        HashMap<String, String> headerMap = new HashMap<>();
        HashMap<String, String> contentMap = new HashMap<>();

        headerMap.put("content-type","application/x-www-form-urlencoded");
        headerMap.put("rid",room);
        headerMap.put("time",tt);
        headerMap.put("auth",auth);

        contentMap.put("rid",room);
        contentMap.put("did","10000000000000000000000000001501");


        result = HttpUtils.postMap(request_url, headerMap,contentMap);

        Map<String, Object> objectObjectMap = JsonUtils.readJson2Map(result,Map.class,String.class,Object.class);
        String error = objectObjectMap.get("error").toString();
        if ("0".equals(error)){
            real_url = ((Map)objectObjectMap.get("data")).get("rtmp_live").toString();
            if (real_url.contains("mix=1")){

            } else {

            }
        }


        return result;
    }
}
