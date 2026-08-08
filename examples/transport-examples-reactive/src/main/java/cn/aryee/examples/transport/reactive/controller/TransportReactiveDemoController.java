package cn.aryee.examples.transport.reactive.controller;

import cn.aryee.examples.transport.reactive.service.TransportReactiveDemoService;
import cn.aryee.transport.api.model.OutboundServiceConfig;
import cn.aryee.transport.api.model.TransportResponse;
import cn.aryee.transport.api.service.ReactiveTransportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Transport 模块功能演示 Controller（Reactive 模式）
 * 演示入站请求处理 + 出站服务调用 + 服务配置管理
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/transport")
@RequiredArgsConstructor
public class TransportReactiveDemoController {

    private final ReactiveTransportService reactiveTransportService;
    private final TransportReactiveDemoService demoService;

    /**
     * 入站演示：接收请求并通过 InboundFilter 链处理
     */
    @GetMapping("/inbound/hello")
    public Mono<TransportResponse> inboundHello(@RequestParam(defaultValue = "Aryee") String name) {
        log.info("[Inbound] hello request: name={}", name);
        return Mono.just(TransportResponse.ok("Hello, " + name + "! (Reactive)"));
    }

    /**
     * 出站演示：通过 ReactiveTransportService 发送出站请求
     */
    @GetMapping("/outbound/call")
    public Mono<TransportResponse> outboundCall(@RequestParam String url) {
        log.info("[Outbound] calling external URL: {}", url);
        return demoService.callExternalService(url);
    }

    /**
     * 出站重试演示：带重试策略的出站调用
     */
    @GetMapping("/outbound/retry")
    public Mono<TransportResponse> outboundRetry(@RequestParam String url,
                                                 @RequestParam(defaultValue = "3") int retries) {
        log.info("[Outbound] calling with retry: url={}, retries={}", url, retries);
        return demoService.callWithRetry(url, retries);
    }

    /**
     * 出站超时演示：带超时控制的出站调用
     */
    @GetMapping("/outbound/timeout")
    public Mono<TransportResponse> outboundTimeout(@RequestParam String url,
                                                    @RequestParam(defaultValue = "3000") long timeoutMs) {
        log.info("[Outbound] calling with timeout: url={}, timeoutMs={}", url, timeoutMs);
        return demoService.callWithTimeout(url, timeoutMs);
    }

    /**
     * 注册出站服务配置
     */
    @PostMapping("/config/register")
    public Mono<TransportResponse> registerConfig(@RequestBody OutboundServiceConfig config) {
        log.info("[Config] register service: {}", config.getServiceName());
        return reactiveTransportService.registerServiceConfig(config)
                .thenReturn(TransportResponse.ok("Service config registered: " + config.getServiceName()));
    }

    /**
     * 查询所有出站服务配置
     */
    @GetMapping("/config/list")
    public Mono<TransportResponse> listConfigs() {
        return reactiveTransportService.listServiceConfigs()
                .map(configs -> TransportResponse.ok(configs));
    }
}
