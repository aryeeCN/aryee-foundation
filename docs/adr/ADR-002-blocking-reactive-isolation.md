# ADR-002: Blocking/Reactive 双模式隔离设计

## 状态
✅ 已采纳

## 背景
现代应用同时存在传统 Servlet 栈（Spring MVC）和响应式栈（Spring WebFlux）的需求。框架需要同时支持两种模式，并保证用户在同一应用中不会因混用而引发问题。

## 决策
采用 **安全隔离原则**，Blocking 和 Reactive 双模式严格隔离：

- **接口隔离**：每个功能模块定义独立的 Blocking 和 Reactive 接口
  - Blocking：`{Module}Service`（如 `CacheService`）
  - Reactive：`Reactive{Module}Service`（如 `ReactiveCacheService`）
- **实现隔离**：实现放在不同包中
  - Blocking：`blocking/{impl-type}/`
  - Reactive：`reactive/{impl-type}/`
- **Starter 隔离**：独立的 Starter 模块
  - `{module}-spring-boot-starter`（Blocking）
  - `{module}-reactive-spring-boot-starter`（Reactive）
- 用户必须二选一引入，禁止同时引入

## 关键约束
- Blocking 实现禁止依赖 Reactor 类型（Mono/Flux）
- Reactive 实现禁止使用阻塞 API（`InputStream`/`OutputStream` 直接读写、`Mono.block()`）
- 方法对齐：Blocking 返回 `T` → Reactive 返回 `Mono<T>`

## 影响
- 增加了约 2 倍的实现工作量，但换来清晰的隔离和零运行时冲突
- 用户根据技术栈选择对应的 Starter，不会引入不必要的依赖
- 两种模式功能等价，每种存储类型必须同时提供 Blocking 和 Reactive 实现
