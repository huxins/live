package cn.inxiny.live;

import cn.inxiny.live.core.Extractor;
import cn.inxiny.live.core.Platform;
import cn.inxiny.live.core.extractors.douyu.service.DouyuExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.Map;

@RestController
public class LiveBox {

    @Resource
    private Extractor huyaExtractor;

    @Resource
    private Extractor douyuExtractor;

    @GetMapping(value = "/huyaLive")
    public String say ( ) throws IOException {
//        return huyaExtractor.gainHuYaJson();
        return null;
    }

    @RequestMapping(value = "/douyuLive/{room}",method = RequestMethod.GET)
    public String send (@PathVariable("room") String room) throws IOException {
        DouyuExtractor douyuExtractor = new DouyuExtractor();
        return douyuExtractor.hlsH5Preview(room);
    }

    @RequestMapping(value = "/live/{item}/{room}",method = RequestMethod.GET)
    public Map extractLive (@PathVariable("item") String item, @PathVariable("room") String room) throws IOException, ScriptException, NoSuchMethodException {
        Platform platform = Platform.valueOf(item.toUpperCase());
        if (platform != null) {
            switch (platform) {
                case HUYA:
                    return huyaExtractor.extract(room);
                case DOUYU:
                    return douyuExtractor.extract(room);
            }
        }

        return null;
    }
}
