package org.opencron.server.controller;

import org.opencron.common.utils.WebUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by benjobs on 2017/1/17.
 */

@Component
public class ExceptionHandler implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse response, Object handler, Exception exception) {
        if (exception instanceof MaxUploadSizeExceededException) {
            WebUtils.writeJson(response,"长传的文件大小超过"+((MaxUploadSizeExceededException)exception).getMaxUploadSize() + "字节限制,上传失败!");
            return null;
        }
        exception.printStackTrace();
        Map<String, Object> model = new HashMap<String, Object>(0);
        model.put("exception", exception);
        return new ModelAndView("/home/error", model);
    }

}
