package cn.inxiny.live.utils;

public class StringUtils {

    /**
     * 判断str是否为空
     *
     * @param str
     * @return
     * @return boolean
     */
    public static boolean isEmpty(Object str) {
        if (str == null || "null".equalsIgnoreCase(str.toString())) {
            return true;
        } else if (str instanceof String) {
            if (((String) str).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
