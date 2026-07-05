package com.middle.wcs.hander;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * @classDesc: 功能描述:(业务异常-需要捕获并处理为error，需要业务方关心的异常)
 * @author: 作者
 * @date: 2020/4/7 11:19
 * @copyright 作者
 */
@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {
    public static final int MAX_DEEP = 4;

    /**
     * 处理业务异常
     *
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler(value = BusinessException.class)
    public ResponseResult handleBusinessException(HttpServletRequest request, BusinessException e) {
        log.warn("业务异常(BusinessException),{}", e);
        ResponseResult result = new ResponseResult();
        result.setMessage(e.getMessage());
        result.setCode(e.getCode());
        return result;
    }


    public BusinessException getNestException(Exception e) {
        Throwable throwable = e;
        int deep = 0;
        while (!(throwable instanceof BusinessException)) {
            throwable = throwable.getCause();
            deep++;
            if (deep > MAX_DEEP) {
                return null;
            }
            if (throwable == null) {
                return null;
            }
        }
        return (BusinessException) throwable;
    }

    /**
     * 处理内存溢出异常
     *
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler(value = OutOfMemoryError.class)
    public ResponseResult handleOutOfMemoryError(HttpServletRequest request, OutOfMemoryError e) {
        // 记录详细的内存信息
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        log.error("内存溢出异常！堆内存使用情况：{}/{} MB，非堆内存使用情况：{}/{} MB",
                heapMemoryUsage.getUsed() / 1024 / 1024,
                heapMemoryUsage.getMax() / 1024 / 1024,
                nonHeapMemoryUsage.getUsed() / 1024 / 1024,
                nonHeapMemoryUsage.getMax() / 1024 / 1024);

        log.error("内存溢出异常 stack info={}", getExceptionStack(e));

        ResponseResult result = new ResponseResult();
        result.setMessage("系统内存不足，请联系管理员");
        result.setCode("500");
        return result;
    }


    /**
     * 处理客户端提前断开连接（Broken pipe）。
     * 连接已断开，响应无法再写回，因此只记录简要日志、不返回响应体，避免二次写异常与刷大堆栈。
     */
    @ExceptionHandler(value = ClientAbortException.class)
    public void handleClientAbort(ClientAbortException e) {
        log.warn("客户端连接已断开(Broken pipe)，忽略该请求响应: {}", e.getMessage());
    }

    /**
     * 处理异常
     *
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public ResponseResult handleException(HttpServletRequest request, Exception e) {
        BusinessException businessException = getNestException(e);
        if (businessException != null) {
            return handleBusinessException(request, businessException);
        }
        log.error("通用异常stack info={}", getExceptionStack(e));
        ResponseResult result = new ResponseResult();
        result.setMessage(e.getMessage());
        return result;
    }

    public static String getExceptionStack(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }


}
