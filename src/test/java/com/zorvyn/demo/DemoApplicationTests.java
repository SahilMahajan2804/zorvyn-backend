package com.zorvyn.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zorvyn.demo.dto.*;
import com.zorvyn.demo.entity.*;
import com.zorvyn.demo.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DemoApplicationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;


    private String registerAndLogin(String email, String password, Role role) throws Exception {
        // Create user directly in DB for exact role control
        User user = User.builder()
                .name("Test User")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .active(true)
                .build();
        userRepository.save(user);

        LoginRequest login = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return auth.getToken();
    }


    @Test
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "alice@test.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role", is("VIEWER")));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest("Bob", "bob@test.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andReturn();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_missingEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("Eve", "", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        RegisterRequest reg = new RegisterRequest("Dan", "dan@test.com", "secret");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg))).andReturn();

        LoginRequest login = new LoginRequest("dan@test.com", "wrong-password");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void createRecord_asAdmin_success() throws Exception {
        String token = registerAndLogin("admin3@test.com", "pass123", Role.ADMIN);

        CreateRecordRequest req = new CreateRecordRequest(
                new BigDecimal("1500.00"), RecordType.INCOME,
                "Salary", LocalDate.now(), "Monthly salary", null);

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type", is("INCOME")))
                .andExpect(jsonPath("$.amount", is(1500.00)));
    }

    @Test
    void createRecord_asViewer_returns403() throws Exception {
        String token = registerAndLogin("viewer@test.com", "pass123", Role.VIEWER);

        CreateRecordRequest req = new CreateRecordRequest(
                new BigDecimal("100.00"), RecordType.EXPENSE,
                "Food", LocalDate.now(), null, null);

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listRecords_asViewer_success() throws Exception {
        String token = registerAndLogin("viewer2@test.com", "pass123", Role.VIEWER);

        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deleteRecord_asAnalyst_returns403() throws Exception {
        // Create record as admin first
        String adminToken = registerAndLogin("admin@test.com", "pass123", Role.ADMIN);
        CreateRecordRequest req = new CreateRecordRequest(
                new BigDecimal("500.00"), RecordType.EXPENSE,
                "Rent", LocalDate.now(), null, null);

        MvcResult cr = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        RecordDto created = objectMapper.readValue(
                cr.getResponse().getContentAsString(), RecordDto.class);

        // Analyst tries to delete
        String analystToken = registerAndLogin("analyst2@test.com", "pass123", Role.ANALYST);
        mockMvc.perform(delete("/api/records/" + created.getId())
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isForbidden());
    }


    @Test
    void dashboard_summary_asViewer_success() throws Exception {
        String token = registerAndLogin("view3@test.com", "pass123", Role.VIEWER);
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").exists())
                .andExpect(jsonPath("$.netBalance").exists());
    }

    @Test
    void dashboard_categories_asViewer_returns403() throws Exception {
        String token = registerAndLogin("view4@test.com", "pass123", Role.VIEWER);
        mockMvc.perform(get("/api/dashboard/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboard_categories_asAnalyst_success() throws Exception {
        String token = registerAndLogin("analyst3@test.com", "pass123", Role.ANALYST);
        mockMvc.perform(get("/api/dashboard/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }


    @Test
    void listUsers_asAdmin_success() throws Exception {
        String token = registerAndLogin("admin2@test.com", "pass123", Role.ADMIN);
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listUsers_asViewer_returns403() throws Exception {
        String token = registerAndLogin("view5@test.com", "pass123", Role.VIEWER);
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isUnauthorized());
    }
}
