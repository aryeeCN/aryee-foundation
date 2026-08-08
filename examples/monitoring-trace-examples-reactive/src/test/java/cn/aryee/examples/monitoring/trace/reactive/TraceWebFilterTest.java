package cn.aryee.examples.monitoring.trace.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Reactive TraceWebFilter 测试：等价于 Blocking 的 TraceFilter 测试
 */
@SpringBootTest
@AutoConfigureWebTestClient
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TraceWebFilterTest {

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    private final WebTestClient webClient;

    @BeforeEach
    void clearContext() {
        cn.aryee.commons.context.TraceContext.clear();
    }

    @Test
    @DisplayName("场景1：W3C traceparent → 响应头与 R.extra.traceId 均等于 traceId32")
    void test_w3c_traceparent() {
        String traceId32 = "4bf92f3577b34da6a3ce929d0e0e4736";
        String traceparent = "00-" + traceId32 + "-00f067aa0ba902b7-01";

        webClient.get().uri("/api/hello")
                .header("traceparent", traceparent)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("x-trace-id", traceId32)
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data").isEqualTo("hello aryee reactive")
                .jsonPath("$.extra.traceId").isEqualTo(traceId32);
    }

    @Test
    @DisplayName("场景2：X-B3-TraceId 备用请求头")
    void test_b3_header() {
        String b3 = "8a3c40a79a1e4c72bf1e5c9a8d3b2f1e";
        webClient.get().uri("/api/hello")
                .header("X-B3-TraceId", b3)
                .header("X-B3-SpanId", "00f067aa0ba902b7")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("x-trace-id", b3)
                .expectBody().jsonPath("$.extra.traceId").isEqualTo(b3);
    }

    @Test
    @DisplayName("场景3：x-trace-id 备用头")
    void test_legacy_x_trace_id() {
        String legacy = "1234567890abcdef1234567890abcdef";
        webClient.get().uri("/api/hello")
                .header("x-trace-id", legacy)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("x-trace-id", legacy)
                .expectBody().jsonPath("$.extra.traceId").isEqualTo(legacy);
    }

    @Test
    @DisplayName("场景4：无 traceId 时自动生成 32 位 UUID")
    void test_auto_generate() {
        String headerTraceId = webClient.get().uri("/api/hello")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseHeaders().getFirst("x-trace-id");

        assertThat(headerTraceId).isNotNull().matches(TRACE_ID_PATTERN.asMatchPredicate());
    }

    @Test
    @DisplayName("场景5：/actuator/health 排除路径不注入响应头")
    void test_exclude_health() {
        webClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist("x-trace-id");
    }
}
