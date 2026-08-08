package cn.aryee.examples.ai.reactive.controller;

import cn.aryee.ai.api.model.ChatMessage;
import cn.aryee.ai.api.model.ChatResponse;
import cn.aryee.ai.api.service.ReactiveLlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI 模块功能演示 Controller（Reactive）
 * 演示响应式 LLM 对话、流式对话、上下文对话
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiReactiveDemoController {

    private final ReactiveLlmService reactiveLlmService;

    /**
     * 响应式对话
     */
    @PostMapping("/chat")
    public Mono<ChatResponse> chat(@RequestParam String message) {
        log.info("[AI Reactive Chat] message={}", message);
        ChatMessage msg = ChatMessage.builder()
                .role("user")
                .content(message)
                .build();
        return reactiveLlmService.chat(msg);
    }

    /**
     * 多轮对话
     */
    @PostMapping("/chat/multi")
    public Mono<ChatResponse> chatMulti(@RequestBody List<ChatMessage> messages) {
        log.info("[AI Reactive Multi-Chat] messageCount={}", messages.size());
        return reactiveLlmService.chat(messages);
    }

    /**
     * 流式对话（SSE）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> stream(@RequestParam String message) {
        log.info("[AI Reactive Stream] message={}", message);
        ChatMessage msg = ChatMessage.builder()
                .role("user")
                .content(message)
                .build();
        return reactiveLlmService.stream(msg);
    }

    /**
     * 带上下文的对话
     */
    @PostMapping("/chat/context")
    public Mono<ChatResponse> chatWithContext(@RequestParam String sessionId, @RequestParam String message) {
        log.info("[AI Reactive Context-Chat] session={}, message={}", sessionId, message);
        return reactiveLlmService.chatWithContext(sessionId, message);
    }

    /**
     * 流式带上下文对话（SSE）
     */
    @PostMapping(value = "/chat/context/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamWithContext(@RequestParam String sessionId, @RequestParam String message) {
        log.info("[AI Reactive Stream-Context] session={}, message={}", sessionId, message);
        return reactiveLlmService.streamWithContext(sessionId, message);
    }

    /**
     * 文本生成
     */
    @PostMapping("/generate")
    public Mono<Map<String, String>> generate(@RequestParam String prompt) {
        log.info("[AI Reactive Generate] prompt={}", prompt);
        return reactiveLlmService.generate(prompt)
                .map(result -> Map.of("result", result));
    }

    /**
     * 获取 Provider 和 Model 信息
     */
    @GetMapping("/info")
    public Mono<Map<String, String>> getInfo() {
        return reactiveLlmService.getProvider()
                .zipWith(reactiveLlmService.getModel())
                .map(tuple -> Map.of(
                        "provider", tuple.getT1(),
                        "model", tuple.getT2()
                ));
    }
}
