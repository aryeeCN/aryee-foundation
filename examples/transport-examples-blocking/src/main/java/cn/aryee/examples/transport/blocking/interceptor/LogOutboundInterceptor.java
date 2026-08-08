package cn.aryee.examples.transport.blocking.interceptor;

import cn.aryee.transport.api.model.TransportRequest;
import cn.aryee.transport.api.model.TransportResponse;
import cn.aryee.transport.api.service.OutboundInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自定义出站拦截器示例
 * 演示如何通过 OutboundInterceptor SPI 实现出站调用日志记录
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Component
public class LogOutboundInterceptor implements OutboundInterceptor {

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public boolean beforeRequest(TransportRequest request) {
        log.info("[OutboundLog] sending request: {} {}", request.getMethod(), request.getPath());
        return true;
    }

    @Override
    public void afterResponse(TransportRequest request, TransportResponse response) {
        log.info("[OutboundLog] received response: status={} durationMs={}",
                response.getStatusCode(), response.getDurationMs());
    }

    @Override
    public void afterError(TransportRequest request, TransportResponse response, Exception ex) {
        log.error("[OutboundLog] outbound call failed: {} {} error={}",
                request.getMethod(), request.getPath(), ex.getMessage());
    }
}
