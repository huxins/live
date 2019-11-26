package cn.inxiny.live;

import cn.inxiny.live.core.extractors.Extractor;
import cn.inxiny.live.core.extractors.Live;
import cn.inxiny.live.core.extractors.Platform;
import cn.inxiny.live.core.extractors.service.DouyuExtractor;
import cn.inxiny.live.utils.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.script.ScriptException;
import java.io.IOException;

@RestController
@RequestMapping(value = "/live")
public class LiveBox {

    @Resource
    private Extractor huyaExtractor;

    @Resource
    private Extractor douyuExtractor;

    @Autowired
    private DouyuExtractor douyuExtractorS;

    @GetMapping(value = "/huyaLive")
    public String say ( ) throws IOException {
//        return huyaExtractor.gainHuYaJson();
        return null;
    }

    @RequestMapping(value = "/douyuLive/{room}",method = RequestMethod.GET)
    public String send (@PathVariable("room") String room) throws IOException, ScriptException, NoSuchMethodException {
        return douyuExtractorS.getTestGraal(room);
    }

    @RequestMapping(value = "/{item}/{room}",method = RequestMethod.GET)
    public ResultBean extractLive (@PathVariable("item") String item, @PathVariable("room") String room) throws IOException, ScriptException, NoSuchMethodException {
        Live live;
        Platform platform = Platform.valueOf(item.toUpperCase());
        if (platform != null) {
            switch (platform) {
                case HUYA:
                    live = huyaExtractor.extract(room);
                    return ResultBean.success(live);
                case DOUYU:
                    live = douyuExtractor.extract(room);
                    return ResultBean.success(live);
            }
        }

        return null;
    }
}
