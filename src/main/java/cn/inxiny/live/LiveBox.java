package cn.inxiny.live;

import cn.inxiny.live.core.Platform;
import cn.inxiny.live.core.extractors.DouyuExtractor;
import cn.inxiny.live.core.extractors.HuyaExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.Map;

@RestController
public class LiveBox {

    @Autowired
    private HuyaExtractor huyaExtractor;

    @Autowired
    private DouyuExtractor douyuExtractor;

    @GetMapping(value = "/huyaLive")
    public String say ( ) throws IOException {
        return huyaExtractor.gainHuYaJson();
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
