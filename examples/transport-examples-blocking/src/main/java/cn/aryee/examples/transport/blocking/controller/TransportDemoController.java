package cn.aryee.examples.transport.blocking.controller;

import cn.aryee.examples.transport.blocking.service.TransportDemoService;
import cn.aryee.transport.api.model.OutboundServiceConfig;
import cn.aryee.transport.api.model.TransportResponse;
import cn.aryee.transport.api.service.TransportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Transport 模块功能演示 Controller（Blocking 模式）
 * 演示入站请求处理 + 出站服务调用 + 服务配置管理
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/transport")
@RequiredArgsConstructor
public class TransportDemoController {

    private final TransportService transportService;
    private final TransportDemoService demoService;

    /**
     * 入站演示：接收请求并通过 InboundFilter 链处理
     */
    @GetMapping("/inbound/hello")
    public TransportResponse inboundHello(@RequestParam(defaultValue = "Aryee") String name) {
        log.info("[Inbound] hello request: name={}", name);
        return TransportResponse.ok("Hello, " + name + "! (Blocking)");
    }

    /**
     * 出站演示：通过 TransportService 发送出站请求
     */
    @GetMapping("/outbound/call")
    public TransportResponse outboundCall(@RequestParam String url) {
        log.info("[Outbound] calling external URL: {}", url);
        return demoService.callExternalService(url);
    }

    /**
     * 出站重试演示：带重试策略的出站调用
     */
    @GetMapping("/outbound/retry")
    public TransportResponse outboundRetry(@RequestParam String url, @RequestParam(defaultValue = "3") int retries) {
        log.info("[Outbound] calling with retry: url={}, retries={}", url, retries);
        return demoService.callWithRetry(url, retries);
    }

    /**
     * 出站超时演示：带超时控制的出站调用
     */
    @GetMapping("/outbound/timeout")
    public TransportResponse outboundTimeout(@RequestParam String url, @RequestParam(defaultValue = "3000") long timeoutMs) {
        log.info("[Outbound] calling with timeout: url={}, timeoutMs={}", url, timeoutMs);
        return demoService.callWithTimeout(url, timeoutMs);
    }

    /**
     * 注册出站服务配置
     */
    @PostMapping("/config/register")
    public TransportResponse registerConfig(@RequestBody OutboundServiceConfig config) {
        log.info("[Config] register service: {}", config.getServiceName());
        transportService.registerServiceConfig(config);
        return TransportResponse.ok("Service config registered: " + config.getServiceName());
    }

    /**
     * 查询所有出站服务配置
     */
    @GetMapping("/config/list")
    public TransportResponse listConfigs() {
        Map<String, OutboundServiceConfig> configs = transportService.listServiceConfigs();
        return TransportResponse.ok(configs);
    }

    /**
     * 查询已注册的入站过滤器
     */
    @GetMapping("/filters")
    public TransportResponse listFilters() {
        return TransportResponse.ok(transportService.getInboundFilters());
    }

    /**
     * 查询已注册的出站拦截器
     */
    @GetMapping("/interceptors")
    public TransportResponse listInterceptors() {
        return TransportResponse.ok(transportService.getOutboundInterceptors());
    }
}
