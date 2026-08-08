package cn.aryee.examples.transport.reactive.service;

import cn.aryee.transport.api.enums.TransportMode;
import cn.aryee.transport.api.model.TransportRequest;
import cn.aryee.transport.api.model.TransportResponse;
import cn.aryee.transport.api.service.ReactiveTransportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Transport 出站调用演示服务（Reactive 模式）
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransportReactiveDemoService {

    private final ReactiveTransportService reactiveTransportService;

    /**
     * 普通出站调用
     */
    public Mono<TransportResponse> callExternalService(String url) {
        TransportRequest request = TransportRequest.builder()
                .mode(TransportMode.OUTBOUND)
                .method("GET")
                .path(url)
                .build()
                .addHeader("Accept", "application/json");
        return reactiveTransportService.send(request);
    }

    /**
     * 带重试的出站调用
     */
    public Mono<TransportResponse> callWithRetry(String url, int retries) {
        TransportRequest request = TransportRequest.builder()
                .mode(TransportMode.OUTBOUND)
                .method("GET")
                .path(url)
                .build()
                .addHeader("Accept", "application/json");
        return reactiveTransportService.sendWithRetry(request, retries);
    }

    /**
     * 带超时的出站调用
     */
    public Mono<TransportResponse> callWithTimeout(String url, long timeoutMs) {
        TransportRequest request = TransportRequest.builder()
                .mode(TransportMode.OUTBOUND)
                .method("GET")
                .path(url)
                .build()
                .addHeader("Accept", "application/json");
        return reactiveTransportService.sendWithTimeout(request, timeoutMs);
    }
}
