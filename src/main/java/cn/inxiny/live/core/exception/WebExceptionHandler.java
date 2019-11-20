package cn.inxiny.live.core.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by huxins on 2019/11/19 22:40
 */
@ControllerAdvice
@ResponseBody
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);


    @ExceptionHandler
    public Map<String, Object> testError(ArithmeticException e, HttpServletRequest request) {
        log.error("出现了除零异常", e);
        return generateErrorInfo(1, "出现了除零异常", HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler
    public Map<String, Object> methodArgumentNotValid(BindException e) {
        log.error("参数校验失败", e);
        List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        StringBuilder errorMessage = new StringBuilder();
        for (int i = 0; i < allErrors.size(); i++) {
            ObjectError error = allErrors.get(i);
            errorMessage.append(error.getDefaultMessage());
            if (i != allErrors.size() - 1) {
                errorMessage.append(", ");
            }
        }
        // do something
        return generateErrorInfo(1, errorMessage.toString(), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler
    public Map<String, Object> nullRoomNumberException(NullRoomNumberException e) {
        log.error("查询房间号为空: ", e);
        return generateErrorInfo(-1, "没有这个房间号!");
    }

    @ExceptionHandler
    public Map<String, Object> unknownException(Exception e) {
        log.error("发生了未知异常: ", e);
        return generateErrorInfo(-99, "系统故障, 请稍后再试!");
    }


    /**
     * 生成错误信息, 放到 request 域中.
     * @param code          错误码
     * @param message       错误信息
     * @param httpStatus    HTTP 状态码
     * @return              SpringBoot 默认提供的 /error Controller 处理器
     */
    private Map<String, Object> generateErrorInfo(int code, String message, int httpStatus) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("message", message);
        return map;
    }

    private Map<String, Object> generateErrorInfo(int code, String message) {
        return generateErrorInfo(code, message, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}