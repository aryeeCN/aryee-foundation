package cn.aryee.examples.monitoring.trace.blocking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TraceFilter 单元测试：验证 3 种 traceId 提取来源 + 自动注入 R 响应
 *
 * <p>覆盖场景：</p>
 * <ol>
 *   <li>W3C traceparent 请求头（标准格式 00-traceId32-spanId16-01）</li>
 *   <li>X-B3-TraceId 备用请求头（Zipkin 兼容）</li>
 *   <li>x-trace-id 备用请求头</li>
 *   <li>无任何请求头时，自动生成 32 位 UUID traceId</li>
 *   <li>R<T>.extra.traceId 与响应头 x-trace-id 一致</li>
 *   <li>排除路径 /actuator/** 不注入 x-trace-id</li>
 * </ol>
 *
 * @author Aryee
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@RequiredArgsConstructor
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TraceFilterTest {

    /** 32 位十六进制 traceId 正则 */
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[0-9a-f]{32}$");

    private final MockMvc mvc;

    /** Boot 4 默认 JSON 切换为 Jackson 3，不再自动装配 Jackson 2 ObjectMapper；测试仅用于解析响应体，本地实例化即可 */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clearContext() {
        // 每次执行前手动清理（Filter 正常情况下会在请求完成后清，但这里兜底）
        cn.aryee.commons.context.TraceContext.clear();
    }

    // =========================================================================
    // 场景 1：W3C traceparent 请求头
    // =========================================================================
    @Test
    @DisplayName("场景1：传入 traceparent 头，应解析出 W3C 标准 traceId32，并写入响应头 x-trace-id 与 R.extra.traceId")
    void test_w3c_traceparent_header() throws Exception {
        String traceId32 = "4bf92f3577b34da6a3ce929d0e0e4736";
        String traceparent = "00-" + traceId32 + "-00f067aa0ba902b7-01";

        MvcResult result = mvc.perform(get("/api/hello")
                        .header("traceparent", traceparent))
                .andExpect(status().isOk())
                .andExpect(header().string("x-trace-id", traceId32))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("hello aryee"))
                .andExpect(jsonPath("$.extra.traceId").value(traceId32))
                .andReturn();

        printIfDebug(result);
    }

    // =========================================================================
    // 场景 2：X-B3-TraceId 请求头（Zipkin 兼容）
    // =========================================================================
    @Test
    @DisplayName("场景2：传入 X-B3-TraceId + X-B3-SpanId，应提取 B3 traceId 并写入响应")
    void test_b3_traceid_header() throws Exception {
        String b3TraceId = "8a3c40a79a1e4c72bf1e5c9a8d3b2f1e";
        String b3SpanId  = "00f067aa0ba902b7";

        mvc.perform(get("/api/hello")
                        .header("X-B3-TraceId", b3TraceId)
                        .header("X-B3-SpanId", b3SpanId))
                .andExpect(status().isOk())
                .andExpect(header().string("x-trace-id", b3TraceId))
                .andExpect(jsonPath("$.extra.traceId").value(b3TraceId));
    }

    // =========================================================================
    // 场景 3：x-trace-id 请求头（项目历史兼容）
    // =========================================================================
    @Test
    @DisplayName("场景3：仅传 x-trace-id 头，应作为 fallback 生效")
    void test_legacy_x_trace_id_header() throws Exception {
        String legacy = "1234567890abcdef1234567890abcdef";
        mvc.perform(get("/api/hello").header("x-trace-id", legacy))
                .andExpect(status().isOk())
                .andExpect(header().string("x-trace-id", legacy))
                .andExpect(jsonPath("$.extra.traceId").value(legacy));
    }

    // =========================================================================
    // 场景 4：优先级：traceparent 优先于 X-B3-* 优先于 x-trace-id
    // =========================================================================
    @Test
    @DisplayName("场景4：同时传 traceparent + x-trace-id，应以 traceparent 为准")
    void test_traceparent_has_highest_priority() throws Exception {
        String w3c =    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String legacy = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

        mvc.perform(get("/api/hello")
                        .header("traceparent", "00-" + w3c + "-00f067aa0ba902b7-01")
                        .header("x-trace-id", legacy))
                .andExpect(status().isOk())
                .andExpect(header().string("x-trace-id", w3c))
                .andExpect(jsonPath("$.extra.traceId").value(w3c));
    }

    // =========================================================================
    // 场景 5：无任何请求头，自动生成符合长度要求的 UUID traceId32
    // =========================================================================
    @Test
    @DisplayName("场景5：请求头完全没有 traceId 信息，应自动生成 32 位十六进制 traceId")
    void test_auto_generate_traceid_when_missing() throws Exception {
        MvcResult result = mvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String responseTraceId = result.getResponse().getHeader("x-trace-id");
        assertThat(responseTraceId).isNotNull()
                .matches(TRACE_ID_PATTERN.asMatchPredicate());

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String extraTraceId = json.at("/extra/traceId").asText();
        assertThat(extraTraceId).isEqualTo(responseTraceId)
                .matches(TRACE_ID_PATTERN.asMatchPredicate());
    }

    // =========================================================================
    // 场景 6：R.extra.traceId 与响应头 x-trace-id 一致（等价于上面各场景内嵌断言，这里再单独验 /api/trace-id）
    // =========================================================================
    @Test
    @DisplayName("场景6：/api/trace-id 返回值 data 与 extra.traceId 与响应头三者一致")
    void test_traceid_consistency_between_body_and_header() throws Exception {
        String traceId = "00000000000000000000000000000001";
        MvcResult result = mvc.perform(get("/api/trace-id").header("x-trace-id", traceId))
                .andExpect(status().isOk())
                .andExpect(header().string("x-trace-id", traceId))
                .andExpect(jsonPath("$.extra.traceId").value(traceId))
                .andExpect(jsonPath("$.data").value(traceId))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    // =========================================================================
    // 场景 7：/actuator/** 等排除路径不注入 x-trace-id 响应头（TraceFilter 被 shouldNotFilter 跳过）
    // =========================================================================
    @Test
    @DisplayName("场景7：/actuator/health 属于 exclude-patterns，响应头无 x-trace-id")
    void test_exclude_pattern_skip_filter() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("x-trace-id"));
    }

    private static void printIfDebug(MvcResult result) throws Exception {
        // System.out.println("响应头 x-trace-id=" + result.getResponse().getHeader("x-trace-id"));
        // System.out.println("响应体=" + result.getResponse().getContentAsString());
    }
}
