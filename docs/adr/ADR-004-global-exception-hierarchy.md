# ADR-004: GlobalException 统一异常体系

## 状态
已接受（Accepted）

## 日期
2026-01-01

## 背景
框架需要统一的异常处理体系，避免各模块自定义 RuntimeException 导致异常处理混乱。

## 决策
以 `GlobalException` 为**唯一基础异常根类**，禁止 `BaseException` 中间层：

```
RuntimeException
  └── GlobalException（唯一根类，实现 ErrorCode 接口）
        ├── BusinessException       // 业务逻辑错误（用户可感知）
        ├── SystemException          // 系统内部错误（需告警）
        ├── AuthException            // 认证失败（401）
        ├── AuthorizationException   // 权限不足（403）
        ├── ResourceNotFoundException // 资源不存在（404）
        └── 各模块自定义异常         // 继承 BusinessException 或 SystemException

Exception（受检分支）
  └── GlobalCheckedException（对称设计）
```

## 理由
1. **统一接口**：所有异常实现 `ErrorCode`，自动绑定错误码、支持 i18n
2. **上下文扩展**：`GlobalException` 提供 `addContext(key, value)` 和 `getData()`
3. **对称设计**：`GlobalCheckedException` 提供受检异常版本，功能一致
4. **简化继承**：不需要 `BaseException` 中间层，`GlobalException` 本身就是 Base

## 影响
- 禁止定义不继承 `GlobalException` 的独立 `RuntimeException` 子类
- 每个自定义异常必须绑定 `ErrorCode` 枚举
- 错误码按 `ServiceErrorCodeRangeEnum` 分段管理
