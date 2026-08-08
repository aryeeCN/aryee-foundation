# ADR-004: 统一异常体系设计

## 状态
✅ 已采纳

## 背景
框架需要统一的异常处理机制，避免各模块自行定义 RuntimeException 子类导致的混乱。同时需要支持国际化、错误码、上下文扩展等企业级需求。

## 决策
采用 **单一基础异常 + 分层继承** 的异常体系：

```
RuntimeException
  └── GlobalException（唯一基础异常根类，实现 ErrorCode）
      ├── BusinessException      → 业务逻辑错误（用户可感知）
      ├── SystemException        → 系统内部错误（需告警）
      ├── AuthException          → 认证失败（401）
      ├── AuthorizationException → 权限不足（403）
      ├── ResourceNotFoundException → 资源不存在（404）
      └── ServiceException       → 通用服务调用异常
```

## 规则
- **禁止定义 BaseException**：GlobalException 本身就是顶层基础异常
- **禁止新建不继承 GlobalException 的 RuntimeException 子类**
- **每个自定义异常绑定 ErrorCode 错误码枚举**
- **错误码分段设计**：由 `ServiceErrorCodeRangeEnum` 分配范围（如 commons: 1000-1999, cache: 2000-2999）

## GlobalException 提供的能力
- `getCode()` / `getMessage()` / `getI18nKey()` — ErrorCode 接口实现
- `getLocalizedMessage()` — 国际化消息解析
- `addContext(key, value)` — 上下文扩展
- `getData()` — 附加数据
- `throwError()` / `wrap()` — 静态工具方法

## 影响
- 全项目异常体系统一，便于全局异常处理器
- 错误码可搜索、可监控
- 支持 i18n 和上下文传递
