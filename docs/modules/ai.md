# Aryee AI 大模型基础设施模块

> **所属项目**: [Aryee Foundation](../../README.md)
> **技术栈**: Java 21、Spring Boot 4.0.7、Spring AI 2.0.0、Reactor、Caffeine、Redisson、Milvus SDK、阿里云 DashScope SDK

## 简介

Aryee AI 大模型基础设施基于 **Spring AI 2.0.0** 构建，提供统一的 AI 能力接入框架，覆盖 LLM 对话、Embedding 向量嵌入、RAG 检索增强生成、Agent 智能体、向量存储、会话管理与 Prompt 工程等核心能力，为业务层提供标准化的 Blocking / Reactive 双模式契约接口。

### 核心特性

- ✅ **多 LLM 提供商**: OpenAI / Azure OpenAI / 阿里云 DashScope（通义千问）/ Anthropic Claude / Ollama
- ℹ️ **OpenAI 兼容 API 接入**: 百度千帆（Qianfan）、MiniMax、DeepSeek、Moonshot 等支持 OpenAI 兼容 API 的提供商，可通过配置 `provider: openai` + 自定义 `base-url` 直接接入，无需单独实现
- ✅ **Embedding 向量嵌入**: 统一的 `EmbeddingService` 契约，支持批量嵌入
- ✅ **RAG 检索增强生成**: 知识库导入、相似度检索、上下文问答一体的 `RagService` / `RagEnhancementService`
- ✅ **向量存储**: Memory / Redis / Milvus / PgVector / Chroma 多后端实现，支持相似度搜索与文本检索
- ✅ **Agent 框架**: 工具注册、`executeWithTools`、`executeWorkflow` 工作流执行
- ✅ **会话上下文管理**: 基于 Caffeine 的 `SessionService`，支持超时与最大消息数控制
- ✅ **Prompt 工程**: 模板管理、版本控制、变量渲染、自动优化、效果评估
- ✅ **文本分块策略**: 递归分块（`RecursiveTextSplitter`）与句子分块（`SentenceTextSplitter`）
- ✅ **流式输出**: Reactive 模式原生支持 `Flux<ChatResponse>` 流式对话与 SSE
- ✅ **双模式隔离**: Blocking / Reactive 严格分层，独立 Starter，禁止同时引入
- ✅ **MCP 协议支持**: 基于 Model Context Protocol 的工具调用与资源访问（`McpClientService` / `ReactiveMcpClientService`）

## 架构定位

- **依赖方向**: `Starter → Autoconfigure → Infrastructure → API`
- **公共基础**: 依赖 `commons-core` / `commons-spring`
- **版本管理**: 由 `aryee-foundation-bom` 统一管理，子模块依赖禁止声明 `<version>`
- **双模式隔离**: Blocking 与 Reactive 使用独立的 Starter / Autoconfigure 模块，用户二选一引入

## 模块结构

```
aryee-foundation-ai/                        # 聚合 POM (artifactId=aryee-foundation-ai)
├── ai-api/                                 # API 契约层（Blocking + Reactive 接口 + 模型 + 配置）
│   └── cn.aryee.ai.api/
│       ├── config/AiProperties.java        # 配置属性 (prefix=aryee.ai)
│       ├── model/                          # 数据模型
│       │   ├── ChatMessage / ChatResponse / FunctionCall
│       │   ├── Embedding / VectorDocument / VectorStoreConfig
│       │   ├── PromptTemplate / PromptOptimizationResult
│       │   ├── ToolDefinition / WorkflowDefinition / AgentConfig
│       │   ├── KnowledgeBase / RetrievalResult / KnowledgeGraph
│       │   ├── AIModel / ModelInfo / ModelVersion / ModelPerformanceMetrics
│       │   └── rag/                        # RAG 子模型
│       └── service/                        # 服务契约（每个能力均含 Blocking + Reactive 双接口）
│           ├── LlmService / ReactiveLlmService
│           ├── EmbeddingService / ReactiveEmbeddingService
│           ├── VectorStoreService / ReactiveVectorStoreService
│           ├── RagService / ReactiveRagService
│           ├── RagEnhancementService
│           ├── AgentService / ReactiveAgentService
│           ├── SessionService / ReactiveSessionService
│           ├── PromptEngineeringService / ReactivePromptEngineeringService
│           ├── ModelManagementService / ModelRegistryService
│           └── TextSplitter                # 共享组件（无 Blocking/Reactive 之分）
├── ai-infrastructure/                      # 实现层（Blocking + Reactive 双实现）
│   └── cn.aryee.ai.infrastructure/
│       ├── blocking/                       # Blocking 实现
│       │   ├── llm/{openai,aliyun,anthropic,ollama}/  # LLM 提供商
│       │   ├── embedding/openai/           # OpenAI Embedding
│       │   ├── vectorstore/{memory,redis,pgvector,chroma}/  # 向量存储
│       │   ├── rag/                        # DefaultRagEnhancementService + SimpleRagService
│       │   │   └── splitter/               # RecursiveTextSplitter / SentenceTextSplitter
│       │   ├── agent/SimpleAgentService
│       │   ├── prompt/DefaultPromptEngineeringService
│       │   └── mcp/                        # MCP 客户端 Noop 实现
│       │   ├── session/CaffeineSessionService
│       │   └── model/DefaultModelManagementService
│       └── reactive/                       # Reactive 实现
│           ├── llm/{openai,azure,dashscope,anthropic,ollama}/ # LLM 提供商
│           ├── embedding/openai/           # Reactive OpenAI Embedding
│           ├── vectorstore/{memory,redis,milvus,pgvector,chroma}/ # 向量存储
│           └── mcp/                        # Reactive MCP 客户端 Noop 实现
│           ├── rag/ReactiveSimpleRagService
│           ├── agent/ReactiveSimpleAgentService
│           ├── prompt/ReactiveSimplePromptEngineeringService
│           └── session/ReactiveSimpleSessionService
├── ai-spring-boot-autoconfigure/           # Blocking 自动配置
│   └── AryeeAiAutoConfiguration            # 注册: META-INF/.../AutoConfiguration.imports
├── ai-spring-boot-starter/                 # Blocking Starter 依赖聚合
├── ai-reactive-spring-boot-autoconfigure/  # Reactive 自动配置
│   └── AryeeAiReactiveAutoConfiguration    # 注册: META-INF/.../AutoConfiguration.imports
└── ai-reactive-spring-boot-starter/        # Reactive Starter 依赖聚合
```

### 模块说明

| 模块 | artifactId | 说明 |
|------|-----------|------|
| ai-api | `ai-api` | Blocking + Reactive 契约接口、数据模型、`AiProperties` 配置 |
| ai-infrastructure | `ai-infrastructure` | Blocking + Reactive 双实现（含 Spring AI 适配） |
| ai-spring-boot-autoconfigure | `ai-spring-boot-autoconfigure` | Blocking 自动配置（`AryeeAiAutoConfiguration`） |
| ai-spring-boot-starter | `ai-spring-boot-starter` | Blocking Starter，开箱即用 |
| ai-reactive-spring-boot-autoconfigure | `ai-reactive-spring-boot-autoconfigure` | Reactive 自动配置（`AryeeAiReactiveAutoConfiguration`） |
| ai-reactive-spring-boot-starter | `ai-reactive-spring-boot-starter` | Reactive Starter，全栈响应式 |

## 使用方法

### Maven 依赖

#### Blocking 模式（默认，Servlet / WebMVC 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>ai-spring-boot-starter</artifactId>
</dependency>
```

#### Reactive 模式（WebFlux 场景）

```xml
<dependency>
    <groupId>cn.aryee.foundation</groupId>
    <artifactId>ai-reactive-spring-boot-starter</artifactId>
</dependency>
```

> - 版本由 BOM 统一管理，无需声明 `<version>`。
> - Blocking 与 Reactive Starter 禁止同时引入。
> - 可选第三方依赖（Spring AI OpenAI / Azure / DashScope / Redisson / Milvus）按需引入，自动配置通过 `@ConditionalOnClass` 装配。

### 配置选项

`AiProperties` 配置前缀：`aryee.ai`，所有字段均提供默认值，零配置可启动。

```yaml
aryee:
  ai:
    # LLM 配置
    llm:
      provider: openai          # openai / azure / aliyun / anthropic / ollama
      model: gpt-4o             # 默认模型
      temperature: 0.7
      max-tokens: 4096
      system-prompt: "You are a helpful assistant."

    # Embedding 配置
    embedding:
      provider: openai
      model: text-embedding-3-small
      dimensions: 1536

    # RAG 配置
    rag:
      top-k: 3
      similarity-threshold: 0.7
      chunk:
        size: 512
        overlap: 128
        strategy: recursive     # recursive / sentence

    # 向量存储配置
    vector-store:
      type: memory              # memory / redis / milvus / pgvector / chroma
      redis:
        index-prefix: "vector:"
        vector-dimension: 1536
        initial-capacity: 1000

    # 会话配置
    session:
      enabled: true
      timeout: 1h               # Duration 类型
      max-messages: 50
      max-total-tokens: 16384
```

### LLM 对话示例

#### Blocking 模式

```java
@Service
public class ChatService {

    private final LlmService llmService;

    public ChatService(LlmService llmService) {
        this.llmService = llmService;
    }

    /** 简单文本生成 */
    public String generate(String prompt) {
        return llmService.generate(prompt);
    }

    /** 单轮对话 */
    public ChatResponse chat(String userMessage) {
        ChatMessage message = ChatMessage.user(userMessage);
        return llmService.chat(message);
    }

    /** 多轮对话 */
    public ChatResponse chat(List<ChatMessage> messages) {
        return llmService.chat(messages);
    }

    /** 带上下文的对话 */
    public ChatResponse chatWithContext(String sessionId, String userMessage) {
        return llmService.chatWithContext(sessionId, userMessage);
    }
}
```

#### Reactive 模式（流式输出）

```java
@Service
public class ReactiveChatService {

    private final ReactiveLlmService reactiveLlmService;

    public ReactiveChatService(ReactiveLlmService reactiveLlmService) {
        this.reactiveLlmService = reactiveLlmService;
    }

    /** 响应式对话 */
    public Mono<ChatResponse> chat(ChatMessage message) {
        return reactiveLlmService.chat(message);
    }

    /** 流式对话（SSE） */
    public Flux<ChatResponse> stream(ChatMessage message) {
        return reactiveLlmService.stream(message);
    }

    /** 流式带上下文 */
    public Flux<ChatResponse> streamWithContext(String sessionId, String userMessage) {
        return reactiveLlmService.streamWithContext(sessionId, userMessage);
    }
}
```

### RAG 检索增强示例

```java
@Service
public class RagQaService {

    private final RagService ragService;

    public RagQaService(RagService ragService) {
        this.ragService = ragService;
    }

    /** 基于知识库的问答 */
    public ChatResponse query(String question, String knowledgeBaseId) {
        return ragService.query(question, knowledgeBaseId);
    }

    /** 带会话上下文的知识库问答 */
    public ChatResponse queryWithContext(String sessionId, String question, String kbId) {
        return ragService.queryWithContext(sessionId, question, kbId);
    }

    /** 导入文档到知识库 */
    public void importDocument(String content, String knowledgeBaseId, Map<String, Object> metadata) {
        ragService.importDocument(content, knowledgeBaseId, metadata);
    }

    /** 更新 / 删除知识库文档 */
    public void updateDocument(String documentId, String content, String kbId) {
        ragService.updateDocument(documentId, content, kbId);
    }

    public void deleteDocument(String documentId, String kbId) {
        ragService.deleteDocument(documentId, kbId);
    }
}
```

### 向量存储示例

```java
@Service
public class VectorStoreExampleService {

    private final VectorStoreService vectorStoreService;
    private final EmbeddingService embeddingService;

    public VectorStoreExampleService(VectorStoreService vectorStoreService,
                                     EmbeddingService embeddingService) {
        this.vectorStoreService = vectorStoreService;
        this.embeddingService = embeddingService;
    }

    /** 添加文档 */
    public void add(VectorDocument document) {
        vectorStoreService.add(document);
    }

    /** 批量添加 */
    public void addAll(List<VectorDocument> documents) {
        vectorStoreService.addAll(documents);
    }

    /** 基于文本的相似度搜索（自动 embedding） */
    public List<VectorDocument> search(String text, int topK, double threshold) {
        return vectorStoreService.similaritySearchByText(text, topK, threshold);
    }

    /** 基于向量的相似度搜索 */
    public List<VectorDocument> search(Embedding query, int topK) {
        return vectorStoreService.similaritySearch(query, topK);
    }

    /** 文档总数 */
    public long count() {
        return vectorStoreService.count();
    }
}
```

### Agent 智能体示例

```java
@Service
public class AgentExampleService {

    private final AgentService agentService;

    public AgentExampleService(AgentService agentService) {
        this.agentService = agentService;
    }

    /** 注册工具 */
    public void registerTool(ToolDefinition tool) {
        agentService.registerTool(tool);
    }

    /** 查询可用工具 */
    public List<ToolDefinition> availableTools() {
        return agentService.getAvailableTools();
    }

    /** 执行 Agent */
    public ChatResponse execute(ChatMessage message) {
        return agentService.execute(message);
    }

    /** 指定工具集执行 */
    public ChatResponse executeWithTools(ChatMessage message, List<String> toolNames) {
        return agentService.executeWithTools(message, toolNames);
    }

    /** 执行工作流 */
    public ChatResponse executeWorkflow(String workflowName, Map<String, Object> inputs) {
        return agentService.executeWorkflow(workflowName, inputs);
    }
}
```

### 会话管理示例

```java
@Service
public class SessionExampleService {

    private final SessionService sessionService;

    public SessionExampleService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /** 创建会话 */
    public String createSession() {
        return sessionService.createSession();
    }

    /** 创建指定 ID 的会话 */
    public String createSession(String sessionId) {
        return sessionService.createSession(sessionId);
    }

    /** 追加消息 */
    public void addMessage(String sessionId, ChatMessage message) {
        sessionService.addMessage(sessionId, message);
    }

    /** 获取历史消息 */
    public List<ChatMessage> getMessages(String sessionId) {
        return sessionService.getMessages(sessionId);
    }

    /** 清空 / 移除会话 */
    public void clear(String sessionId) {
        sessionService.clearSession(sessionId);
    }
}
```

### Prompt 工程示例

```java
@Service
public class PromptExampleService {

    private final PromptEngineeringService promptService;

    public PromptExampleService(PromptEngineeringService promptService) {
        this.promptService = promptService;
    }

    /** 创建模板并渲染 */
    public String createAndRender(String name, String content, Map<String, Object> variables) {
        PromptTemplate template = new PromptTemplate();
        template.setName(name);
        template.setContent(content);
        PromptTemplate saved = promptService.createTemplate(template);
        return promptService.renderTemplate(saved.getId(), variables);
    }

    /** 验证模板变量 */
    public PromptEngineeringService.ValidationResult validate(String templateId, Map<String, Object> variables) {
        return promptService.validateTemplateVariables(templateId, variables);
    }

    /** 自动优化 Prompt */
    public PromptOptimizationResult optimize(String templateId) {
        return promptService.optimizePrompt(templateId);
    }

    /** 版本回滚 */
    public void rollback(String templateId, String version) {
        promptService.rollbackToVersion(templateId, version);
    }
}
```

## 安全管控

AI 模块支持可选的安全管控能力，委托 [security 模块](security.md) 进行操作权限检查和审计日志，遵循 [security-governance.md](https://github.com/aryeecn/aryee-foundation)（内部规范：security-governance） 规则。

### 工作原理

```
调用方 → SecuredLlmService/SecuredAgentService/SecuredSessionService（装饰器） → 原始服务
              ↓                              ↓
     AiSecurityService              AI 操作（chat/agent/session）
      ├─ checkPermission()           （委托 security 模块）
      └─ audit()
```

### 安全风险等级

| 操作 | 风险等级 | 权限常量 |
|------|---------|---------|
| `chat()` / `chatWithContext()` / `generate()` | 🔴 高 | `ai:chat` |
| Agent `execute()` / `executeWithTools()` / `executeWorkflow()` | 🔴 高 | `ai:agent:execute` |
| Agent `registerTool()` | 🟠 中 | `ai:agent:manage` |
| Session `createSession()` | 🟠 中 | `ai:session:create` |
| Session `clearSession()` / `removeSession()` | 🟠 中 | `ai:session:delete` |
| `getMessages()` / `getAvailableTools()` | 🟢 低 | 仅查询，不检查写权限 |

### 配置示例

```yaml
aryee:
  ai:
    security:
      enabled: true      # 启用 AI 安全管控
      audit-enabled: true # 启用 AI 操作审计日志
```

### 使用方式

```java
// 在调用 AI 操作前设置当前用户
SecurityContextHolder.setUserId(currentUserId);
try {
    // 自动权限检查 + 审计日志
    ChatResponse response = llmService.chat(userMessage);
    agentService.execute(message);
} finally {
    SecurityContextHolder.clear();
}
```

### 条件装配

| Bean | 条件 | 说明 |
|------|------|------|
| `DefaultAiSecurityService` | `DynamicPermissionService` + `SecurityAuditService` Bean 存在 + `aryee.ai.security.enabled=true` | 委托 security 模块 |
| `NoopAiSecurityService` | `aryee.ai.security.enabled=true` 但 security 模块未引入 | 降级方案 |
| `SecuredSessionService` | `AiSecurityService` Bean 存在 | `@Primary` 装饰器 |
| `SecuredLlmService` | 同上 | `@Primary` 装饰器 |
| `SecuredAgentService` | 同上 | `@Primary` 装饰器 |

## 兼容性

### 运行环境

| 项 | 版本 |
|----|------|
| Java | 21+ |
| Spring Boot | 4.0.7 |
| Spring AI | 2.0.0 |

### 支持的 LLM 提供商

| 提供商 | 配置值 | Blocking 实现 | Reactive 实现 | 流式输出 |
|--------|--------|--------------|--------------|---------|
| OpenAI | `openai` | `OpenAiLlmService` | `ReactiveOpenAiLlmService` | ✅ |
| Azure OpenAI | `azure` | — | `ReactiveAzureOpenAiLlmService` | ✅ |
| 阿里云 DashScope | `aliyun` | `AliyunAiLlmService` | `ReactiveDashScopeLlmService` | ✅ |
| Anthropic Claude | `anthropic` | `AnthropicLlmService` | `ReactiveAnthropicLlmService` | ✅ |
| Ollama（本地） | `ollama` | `OllamaLlmService` | `ReactiveOllamaLlmService` | ✅ |

> 配置项：`aryee.ai.llm.provider`（默认 `openai`）
>
> ⚠️ Spring AI 2.0 已移除智谱（ZhiPu）官方支持，本模块同步删除智谱实现；Azure OpenAI 通过 `spring-ai-starter-model-openai` 配置 Azure 专属 endpoint 接入。

### 支持的向量存储

| 存储类型 | 配置值 | Blocking 实现 | Reactive 实现 | 适用场景 |
|---------|--------|--------------|--------------|---------|
| Memory | `memory` | `MemoryVectorStore` | `ReactiveMemoryVectorStoreService` | 开发测试、小规模数据 |
| Redis | `redis` | `RedisVectorStore` | `ReactiveRedisVectorStoreService` | 生产环境、中等规模 |
| Milvus | `milvus` | — | `ReactiveMilvusVectorStoreService` | 大规模向量检索 |
| PgVector | `pgvector` | `PgVectorVectorStoreService` | `ReactivePgVectorVectorStoreService` | PostgreSQL 生态、事务一致性 |
| Chroma | `chroma` | `ChromaVectorStoreService` | `ReactiveChromaVectorStoreService` | AI 原生、开发友好 |

> 配置项：`aryee.ai.vector-store.type`（默认 `memory`）

### MCP（Model Context Protocol）支持

| 模式 | 接口 | 默认实现 | 说明 |
|------|------|---------|------|
| Blocking | `McpClientService` | `SpringAiMcpClientService`（Noop） | 工具调用、资源访问 |
| Reactive | `ReactiveMcpClientService` | `ReactiveSpringAiMcpClientService`（Noop） | 响应式工具调用 |

> 默认提供 Noop 实现（返回空结果），业务项目可通过 `@Bean` 覆盖接入实际 MCP 服务端。
> 当 `spring-ai-starter-mcp-client` 在类路径上时，Spring AI 自动配置 McpSyncClient。
