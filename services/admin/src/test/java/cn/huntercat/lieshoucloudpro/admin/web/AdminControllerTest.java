package cn.huntercat.lieshoucloudpro.admin.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshoucloudpro.admin.feign.UserFeignClient;
import cn.huntercat.lieshoucloudpro.admin.feign.dto.UserDTO;
import java.time.Instant;

/**
 * AdminController 集成测试（@SpringBootTest + MockMvc + Mock feign）.
 *
 * <p>admin-service 是 Feign 薄封装：验证端点路径、Feign 调用透传、熔断 fallback 契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AdminController（跨服务 Feign 端点）")
class AdminControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserFeignClient userClient;

  @Test
  @DisplayName("GET /api/admin/health → 200，含 user-service 用户数")
  void health_callsUserService() throws Exception {
    when(userClient.count()).thenReturn(42L);
    mockMvc
        .perform(get("/api/admin/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.app").value("lieshoucloud-admin"))
        .andExpect(jsonPath("$.userServiceUserCount").value(42));
  }

  @Test
  @DisplayName("GET /api/admin/users/{id} → 200，Feign 返回用户")
  void getUser_found() throws Exception {
    when(userClient.findById(7L))
        .thenReturn(new UserDTO(7L, "alice", "Alice", Instant.parse("2026-08-01T00:00:00Z")));
    mockMvc
        .perform(get("/api/admin/users/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.username").value("alice"));
  }

  @Test
  @DisplayName("GET /api/admin/users/{id} → 200，用户不存在返回 USER_NOT_FOUND")
  void getUser_notFound() throws Exception {
    when(userClient.findById(999L)).thenReturn(null);
    mockMvc
        .perform(get("/api/admin/users/999"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
  }

  @Test
  @DisplayName("GET /api/admin/users/by-username/{username} → 200，按用户名查")
  void getUserByUsername() throws Exception {
    when(userClient.findByUsername("alice")).thenReturn(new UserDTO(7L, "alice", "Alice", null));
    mockMvc
        .perform(get("/api/admin/users/by-username/alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("alice"));
  }

  @Test
  @DisplayName("熔断 fallback：user-service 不可用时 health 降级而非 5xx")
  void health_degradedFallback() throws Exception {
    when(userClient.count()).thenThrow(new RuntimeException("user-service down"));
    mockMvc
        .perform(get("/api/admin/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.degraded").value(true))
        .andExpect(jsonPath("$.status").value("UP"));
  }
}
