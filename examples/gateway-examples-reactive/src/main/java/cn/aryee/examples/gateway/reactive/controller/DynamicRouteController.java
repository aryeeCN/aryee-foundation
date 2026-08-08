package cn.aryee.examples.gateway.reactive.controller;

import cn.aryee.gateway.api.route.DynamicRouteService;
import cn.aryee.gateway.api.route.RouteInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态路由管理 Controller
 * 使用 foundation-gateway 模块提供的 {@code DynamicRouteService} 实现路由 CRUD
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/gateway/routes")
@RequiredArgsConstructor
public class DynamicRouteController {

    private final DynamicRouteService dynamicRouteService;

    /**
     * 获取所有路由
     */
    @GetMapping
    public Mono<List<RouteInfo>> listAll() {
        log.info("查询所有路由");
        return Mono.fromCallable(dynamicRouteService::listRoutes)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取路由数量
     */
    @GetMapping("/count")
    public Mono<Map<String, Object>> count() {
        return Mono.fromCallable(() -> {
            Map<String, Object> map = new HashMap<>();
            map.put("total", dynamicRouteService.listRoutes().size());
            return map;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 添加路由
     */
    @PostMapping
    public Mono<Map<String, Object>> add(@RequestBody RouteInfo routeInfo) {
        log.info("添加路由: {}", routeInfo.getRouteId());
        return Mono.fromCallable(() -> dynamicRouteService.addRoute(routeInfo))
                .map(success -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("success", success);
                    map.put("message", success ? "路由添加成功" : "路由ID已存在");
                    return map;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新路由
     */
    @PutMapping
    public Mono<Map<String, Object>> update(@RequestBody RouteInfo routeInfo) {
        log.info("更新路由: {}", routeInfo.getRouteId());
        return Mono.fromCallable(() -> dynamicRouteService.updateRoute(routeInfo))
                .map(success -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("success", success);
                    map.put("message", success ? "路由更新成功" : "路由更新失败");
                    return map;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除路由
     */
    @DeleteMapping("/{routeId}")
    public Mono<Map<String, Object>> delete(@PathVariable String routeId) {
        log.info("删除路由: {}", routeId);
        return Mono.fromCallable(() -> dynamicRouteService.deleteRoute(routeId))
                .map(success -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("success", success);
                    map.put("message", success ? "路由删除成功" : "路由不存在");
                    return map;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 刷新路由表
     */
    @PostMapping("/refresh")
    public Mono<Map<String, Object>> refresh() {
        log.info("刷新路由表");
        return Mono.fromCallable(() -> {
            dynamicRouteService.refreshRoutes();
            Map<String, Object> map = new HashMap<>();
            map.put("success", true);
            map.put("message", "路由表已刷新");
            return map;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
