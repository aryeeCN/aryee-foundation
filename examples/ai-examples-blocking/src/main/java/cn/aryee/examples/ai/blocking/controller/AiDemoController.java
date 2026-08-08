package cn.aryee.examples.ai.blocking.controller;

import cn.aryee.ai.api.model.ChatMessage;
import cn.aryee.ai.api.model.ChatResponse;
import cn.aryee.ai.api.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 模块功能演示 Controller（Blocking）
 * 演示 LLM 对话、多轮对话、上下文对话、文本生成
 *
 * @author Aryee
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiDemoController {

    private final LlmService llmService;

    /**
     * 简单对话
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestParam String message) {
        log.info("[AI Chat] message={}", message);
        ChatMessage msg = ChatMessage.builder()
                .role("user")
                .content(message)
                .build();
        return llmService.chat(msg);
    }

    /**
     * 多轮对话
     */
    @PostMapping("/chat/multi")
    public ChatResponse chatMulti(@RequestBody List<ChatMessage> messages) {
        log.info("[AI Multi-Chat] messageCount={}", messages.size());
        return llmService.chat(messages);
    }

    /**
     * 带上下文的对话
     */
    @PostMapping("/chat/context")
    public ChatResponse chatWithContext(@RequestParam String sessionId, @RequestParam String message) {
        log.info("[AI Context-Chat] session={}, message={}", sessionId, message);
        return llmService.chatWithContext(sessionId, message);
    }

    /**
     * 文本生成
     */
    @PostMapping("/generate")
    public Map<String, String> generate(@RequestParam String prompt) {
        log.info("[AI Generate] prompt={}", prompt);
        String result = llmService.generate(prompt);
        return Map.of("result", result);
    }

    /**
     * 获取 Provider 和 Model 信息
     */
    @GetMapping("/info")
    public Map<String, String> getInfo() {
        return Map.of(
                "provider", llmService.getProvider(),
                "model", llmService.getModel()
        );
    }
}
