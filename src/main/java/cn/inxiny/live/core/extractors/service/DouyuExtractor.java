package cn.inxiny.live.core.extractors.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.inxiny.live.core.exception.NullRoomNumberException;
import cn.inxiny.live.core.extractors.Extractor;
import cn.inxiny.live.core.extractors.Live;
import cn.inxiny.live.core.extractors.Platform;
import cn.inxiny.live.utils.HttpUtils;
import cn.inxiny.live.utils.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.script.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@Service("douyuExtractor")
public class DouyuExtractor implements Extractor {

    @Autowired
    private RedisUtil redisUtil;

    public Live extract (String room) throws IOException, ScriptException, NoSuchMethodException {
        //  获取直播状态与信息
        Live live = getRoomInfo(room);
        if (live.isOnline()){
            String stream = getPre_url(room);
            if (StringUtils.isEmpty(stream)){
                String signjs = getSignJS(room);
                String sign = getSign(room,signjs);
                stream = getStream(room, sign);
            }
            if (StringUtils.isEmpty(stream)){
                stream = getStreamOnHome(room);
            }
            live.setLink(stream);
            getLink(live);
        }
        return live;
    }

    private Live getRoomInfo (String room) {
        // https://open.douyucdn.cn/api/RoomApi/room/4615502 备用接口
        Live live = new Live(Platform.DOUYU);

        String info = HttpRequest.get("https://www.douyu.com/betard/" + room).execute().body();
        if (info.contains("提示信息")){
            throw new NullRoomNumberException();
        }
        JSONObject parse = JSONUtil.parseObj(info);
        JSONObject roomInfo = parse.getJSONObject("room");
        live.setPrivateHost(roomInfo.getStr("vipId"));
        live.setRoomId(roomInfo.getStr("room_id"));
        live.setRoomName(roomInfo.getStr("room_name"));
        live.setRoomInfo(roomInfo.getStr("show_details"));
        live.setOnline(1 == roomInfo.getInt("show_status"));
        live.setOwnerName(roomInfo.getStr("nickname"));
        live.setLastTime(new Date(Long.parseLong(roomInfo.getStr("show_time") + "000")));
        live.setOwnerAvatar(roomInfo.getStr("owner_avatar"));
        live.setRoomImg(roomInfo.getStr("room_pic"));

        return live;
    }

    private String getSignJS (String room) throws IOException {
        String signjs = redisUtil.get(this.getClass().getName() + ":room=" + room + ":getSign.js");
        if (StringUtils.isNotEmpty(signjs)){
            return signjs;
        }

        //  网页获取JS
        String html = Jsoup.connect("https://www.douyu.com/" + room).get().html();
        String rid = ReUtil.get("\\$ROOM\\.room_id\\s*=\\s*(\\d+)", html, 1);
        signjs = ReUtil.get("(var vdwdae325w_64we =[\\s\\S]+?)\\s*</script>", html, 1);
        if (StringUtils.isEmpty(signjs) || !signjs.contains("ub98484234(")){
            //  API获取JS
            html = HttpUtils.sendGet("https://www.douyu.com/swf_api/homeH5Enc", "rids=" + rid);
            JSONObject parse = JSONUtil.parseObj(html);
            signjs = parse.getJSONObject("data").getStr("room" + rid);
        }
        if (signjs == null){
            throw new NullRoomNumberException();
        }

        redisUtil.set(this.getClass().getName()+":room="+ room + ":getSign.js", signjs);
        return signjs;
    }

    private String getSign (String room, String signjs) throws FileNotFoundException, ScriptException, NoSuchMethodException {
        Object sign;

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
        engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/crypto-js.min.js"));
        engine.eval(signjs);
        engine.eval(new FileReader("src/main/java/cn/inxiny/live/core/expand/hello.js"));
        Invocable invocable = (Invocable) engine;
        sign = invocable.invokeFunction("getSign", room,"d063f90e47753de1b979189300081501",new Date().getTime()/1000);
        return sign.toString();
    }

    private String getStream (String room,String sign) {
        String stream = "";
        String body = HttpUtils.sendPost("https://www.douyu.com/lapi/live/getH5Play/"+room, sign);
        JSONObject parse = JSONUtil.parseObj(body);
        String error = parse.getStr("error");
        if ("0".equals(error)){
            JSONObject data = parse.getJSONObject("data");
            String rtmp_live = data.getStr("rtmp_live");
            stream = ReUtil.get("^[0-9a-zA-Z]*",rtmp_live,0);
        }
        return stream;
    }

    private String getPre_url (String room) {
        String pre_url = "";
        String request_url = "https://playweb.douyucdn.cn/lapi/live/hlsH5Preview/" + room;

        String tt = String.valueOf(new Date().getTime());
        String md5Early = room + tt;
        String auth = DigestUtils.md5DigestAsHex(md5Early.getBytes());

        String body = HttpRequest.post(request_url)
                .header(Header.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .header("rid", room)
                .header("time", tt)
                .header("auth", auth)
                .form("rid", room)
                .form("did", "10000000000000000000000000001501")
                .timeout(20000)
                .execute().body();

        JSONObject parse = JSONUtil.parseObj(body);
        String error = parse.getStr("error");
        if ("0".equals(error)){
            String rtmp_live = parse.getJSONObject("data").getStr("rtmp_live");
            if (rtmp_live.contains("mix=1")){
                //  PKing
            } else {
                pre_url = ReUtil.get("^[0-9a-zA-Z]*", rtmp_live, 0);
            }
        }

        return pre_url;
    }

    //  通过手机端获取流，备用解析
    private String getStreamOnHome (String room) throws ScriptException, NoSuchMethodException {
        String streamOnH5 = "";

        String homejs = getHomeJS(room);
        Date date = new Date();
        String tt = String.valueOf(date.getTime() / 1000);
        String post_v = DateUtil.format(date, "yyyyMMdd");
        String homeSing = getHomeSign(room, post_v, tt, homejs);
        String homeSignUrl = getHomeSignUrl(room, post_v, tt, homeSing);
        if (StringUtils.isNotEmpty(homeSignUrl)){
            streamOnH5 = homeSignUrl;
        }

        return streamOnH5;
    }

    private String getHomeJS (String room) {
        String homejs = redisUtil.get(this.getClass().getName() + ":room=" + room + ":getHome.js");
        if (StringUtils.isNotEmpty(homejs)){
            return homejs;
        }

        String html = HttpRequest.get("https://m.douyu.com/" + room).execute().body();
//        String rid = ReUtil.get("\"rid\":(\\d{1,7})",html,1);

        Pattern compile = Pattern.compile("(function ub9.*)[\\s\\S](var.*)");
        String group1 = ReUtil.get(compile,html,1);
        String group2 = ReUtil.get(compile,html,2);
        group1 = group1.replaceAll("eval.*;}", "strc;}");
        homejs = group1 + group2;

        redisUtil.set(this.getClass().getName()+":room="+ room + ":getHome.js", homejs);
        return homejs;
    }

    private String getHomeSign (String rid,String post_v,String tt,String homejs) throws ScriptException, NoSuchMethodException {
        String sign;

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

    private String getHomeSignUrl (String rid,String post_v,String tt,String sign) {
        String signurl = "";
        String request_url = "https://m.douyu.com/api/room/ratestream";

        String body = HttpRequest.post(request_url)
                .header(Header.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .header(Header.USER_AGENT, "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Mobile Safari/537.36")
                .form("v", "2501" + post_v)
                .form("did", "10000000000000000000000000001501")
                .form("tt", tt)
                .form("sign", sign)
                .form("ver", "219032101")
                .form("rid", rid)
                .form("rate", "-1")
                .timeout(20000)
                .execute().body();
        JSONObject parse = JSONUtil.parseObj(body);
        String code = parse.getStr("code");
        if ("0".equals(code)){
            String url = parse.getJSONObject("data").getStr("url");
            if (url.contains("mix=1")){
                //  PKing
            } else {
                signurl = ReUtil.get("live/(\\d{1,8}[0-9a-zA-Z]+)_?[\\d]{0,4}/playlist", url, 1);
            }
        }

        return signurl;
    }

    private Live getLink (Live live){
        String link = "http://tx2play1.douyucdn.cn/live/" + live.getLink() + ".flv?uuid=";
        live.setLink(link);
        return live;
    }

    public String getTestGraal (String live) {

        List<ScriptEngineFactory> engines = (new ScriptEngineManager()).getEngineFactories();
        for (ScriptEngineFactory f: engines) {
            System.out.println(f.getLanguageName()+" "+f.getEngineName()+" "+f.getNames().toString());
        }
        return live;
    }

}
