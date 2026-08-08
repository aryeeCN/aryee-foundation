package cn.aryee.examples.architecture.cloudnative.controller;

import cn.aryee.commons.spring.config.spi.ConfigurationSourceProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 配置展示 REST API（云原生架构示例）
 * <p>
 * 演示通过 {@link ConfigurationSourceProvider} SPI 统一读取配置，
 * 配置源可能是 K8s ConfigMap（有 K8s Client 时）或 Spring Environment（兜底）。
 *
 * @author Aryee
 * @since 1.2.0
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final List<ConfigurationSourceProvider> configProviders;

    public ConfigController(List<ConfigurationSourceProvider> configProviders) {
        this.configProviders = configProviders;
    }

    /**
     * 获取配置源信息
     */
    @GetMapping("/sources")
    public List<Map<String, Object>> listConfigSources() {
        return configProviders.stream()
                .map(p -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", p.getName());
                    info.put("order", p.getOrder());
                    info.put("available", p.isAvailable());
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    @GetMapping("/value")
    public ResponseEntity<Map<String, Object>> getConfigValue(@RequestParam String key) {
        for (ConfigurationSourceProvider provider : configProviders) {
            String value = provider.getProperty(key);
            if (value != null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("key", key);
                result.put("value", value);
                result.put("source", provider.getName());
                return ResponseEntity.ok(result);
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 健康检查 + 配置源摘要
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("configSources", configProviders.size());
        configProviders.forEach(p -> status.put(p.getName(), p.isAvailable() ? "UP" : "DOWN"));
        return status;
    }
}