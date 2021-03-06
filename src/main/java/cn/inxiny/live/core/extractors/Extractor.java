package cn.inxiny.live.core.extractors;

import javax.script.ScriptException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by huxins on 2019/11/18 13:05
 */
public interface Extractor {
    Live extract(String room) throws IOException, ScriptException, NoSuchMethodException;
}
