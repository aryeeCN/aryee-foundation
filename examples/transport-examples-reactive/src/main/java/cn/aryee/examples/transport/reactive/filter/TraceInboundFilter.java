package cn.aryee.examples.transport.reactive.filter;

import cn.aryee.transport.api.exception.TransportException;
import cn.aryee.transport.api.model.TransportRequest;
import cn.aryee.transport.api.model.TransportResponse;
import cn.aryee.transport.api.service.InboundFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 自定义入站过滤器示例（Reactive 模式）
 * 演示如何通过 InboundFilter SPI 实现请求追踪
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Component
public class TraceInboundFilter implements InboundFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public void preHandle(TransportRequest request) throws TransportException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
            request.addHeader(TRACE_ID_HEADER, traceId);
        }
        log.info("[TraceFilter] inbound request: {} {} traceId={}",
                request.getMethod(), request.getPath(), traceId);
    }

    @Override
    public void postHandle(TransportRequest request, TransportResponse response) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        response.addHeader(TRACE_ID_HEADER, traceId);
        log.info("[TraceFilter] inbound response: {} traceId={}",
                response.getStatusCode(), traceId);
    }
}
