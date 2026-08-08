package cn.aryee.examples.transport.blocking.service;

import cn.aryee.transport.api.enums.TransportMode;
import cn.aryee.transport.api.model.TransportRequest;
import cn.aryee.transport.api.model.TransportResponse;
import cn.aryee.transport.api.service.TransportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Transport 出站调用演示服务（Blocking 模式）
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransportDemoService {

    private final TransportService transportService;

    /**
     * 普通出站调用
     */
    public TransportResponse callExternalService(String url) {
        TransportRequest request = TransportRequest.builder()
                .mode(TransportMode.OUTBOUND)
                .method("GET")
                .path(url)
                .build()
                .addHeader("Accept", "application/json");
        return transportService.send(request);
    }

    /**
     * 带重试的出站调用
     */
    public TransportResponse callWithRetry(String url, int retries) {
        TransportRequest request = TransportRequest.builder()
                .mode(TransportMode.OUTBOUND)
                .method("GET")
                .path(url)
                .build()
                .addHeader("Accept", "application/json");
        return transportService.sendWithRetry(request, retries);
    }

    /**
     * 带超时的出站调用
     */
    public TransportResponse callWithTimeout(String url, long timeoutMs) {
        TransportRequest request = TransportRequest.builder()
                .mode(TransportMode.OUTBOUND)
                .method("GET")
                .path(url)
                .build()
                .addHeader("Accept", "application/json");
        return transportService.sendWithTimeout(request, timeoutMs);
    }
}
