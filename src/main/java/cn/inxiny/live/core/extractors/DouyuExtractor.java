package cn.inxiny.live.core.extractors;

import cn.inxiny.live.utils.HttpUtils;
import cn.inxiny.live.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DouyuExtractor {

    public Map extract (String room) throws IOException, ScriptException, NoSuchMethodException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
//        engine.eval("print('Hello World!');");
//        engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/hello.js"));
        engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/core.min.js"));
        engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/md5.min.js"));

        Document exam = Jsoup.connect("https://www.douyu.com/" + room).get();
        String html = exam.html();
        // 获取加密 JS
        Matcher TT_ROOM_DATA = Pattern.compile("var vdw(.*)").matcher(html);
        while (TT_ROOM_DATA.find()) {
            String group = TT_ROOM_DATA.group();
            String s = StringUtils.substringBefore(group, "</script>");
            engine.eval(s);
            engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/hello.js"));
            Invocable invocable = (Invocable) engine;
            Object sign = invocable.invokeFunction("getSign", room,"d063f90e47753de1b979189300081501",new Date().getTime()/1000);
            System.out.println(sign);
//            String result = HttpUtils.sendPost("https://www.douyu.com/lapi/live/getH5Play/"+room, sign.toString());
            Connection con = Jsoup.connect("https://www.douyu.com/lapi/live/getH5Play/"+room);
            String[] split = sign.toString().split("&");
            for (String str: split) {
                String[] param = str.split("=");
                if (param.length > 1){
                    con.data(param[0], param[1]);
                } else {
                    con.data(param[0], "");
                }
            }
            con.header("Accept", "application/json, text/plain, */*");
            con.header("Content-Type", "application/x-www-form-urlencoded");
            con.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36");
//            Document doc = con.post();
//            System.out.println(doc.html());
        }

        Map map = new HashMap();
        map.put("state","Hello");
        return map;
    }
}
