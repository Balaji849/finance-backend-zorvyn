package com.finance.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dashboard.dto.LoginRequest;
import com.finance.dashboard.dto.RegisterRequest;
import com.finance.dashboard.dto.TransactionRequest;
import com.finance.dashboard.model.Transaction;
import com.finance.dashboard.model.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinanceDashboardIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    static String adminToken;
    static String analystToken;
    static String viewerToken;
    static String createdTxId;

    // ── Auth Tests ──────────────────────────────────────────────────────────

    @Test @Order(1)
    void registerAndLoginAdmin() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test Admin");
        req.setEmail("testadmin@test.com");
        req.setPassword("admin123");
        req.setRole(User.Role.ADMIN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        LoginRequest login = new LoginRequest();
        login.setEmail("testadmin@test.com");
        login.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(body).at("/data/token").asText();
    }

    @Test @Order(2)
    void registerAnalystAndViewer() throws Exception {
        RegisterRequest analystReq = new RegisterRequest();
        analystReq.setFullName("Test Analyst");
        analystReq.setEmail("testanalyst@test.com");
        analystReq.setPassword("analyst123");
        analystReq.setRole(User.Role.ANALYST);

        MvcResult analystResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analystReq)))
                .andExpect(status().isCreated())
                .andReturn();
        analystToken = objectMapper.readTree(
                analystResult.getResponse().getContentAsString()).at("/data/token").asText();

        RegisterRequest viewerReq = new RegisterRequest();
        viewerReq.setFullName("Test Viewer");
        viewerReq.setEmail("testviewer@test.com");
        viewerReq.setPassword("viewer123");
        viewerReq.setRole(User.Role.VIEWER);

        MvcResult viewerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(viewerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        viewerToken = objectMapper.readTree(
                viewerResult.getResponse().getContentAsString()).at("/data/token").asText();
    }

    @Test @Order(3)
    void loginWithWrongPasswordReturns400() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("testadmin@test.com");
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── Transaction Tests ───────────────────────────────────────────────────

    @Test @Order(4)
    void analystCanCreateTransaction() throws Exception {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("1500.00"));
        req.setType(Transaction.Type.INCOME);
        req.setCategory("Salary");
        req.setDate(LocalDate.now());
        req.setNotes("Monthly salary");

        MvcResult result = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value("Salary"))
                .andReturn();

        createdTxId = objectMapper.readTree(
                result.getResponse().getContentAsString()).at("/data/id").asText();
    }

    @Test @Order(5)
    void viewerCannotCreateTransaction() throws Exception {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("100.00"));
        req.setType(Transaction.Type.EXPENSE);
        req.setCategory("Food");
        req.setDate(LocalDate.now());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test @Order(6)
    void viewerCanListTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test @Order(7)
    void listWithFilters() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("type", "INCOME")
                        .param("category", "Salary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test @Order(8)
    void adminCanUpdateTransaction() throws Exception {
        Assumptions.assumeTrue(createdTxId != null, "Skipping: no transaction was created");

        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("2000.00"));
        req.setType(Transaction.Type.INCOME);
        req.setCategory("Salary");
        req.setDate(LocalDate.now());
        req.setNotes("Updated salary");

        mockMvc.perform(put("/api/transactions/" + createdTxId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(2000.00));
    }

    @Test @Order(9)
    void analystCannotUpdateTransaction() throws Exception {
        Assumptions.assumeTrue(createdTxId != null);

        TransactionRequest req = new TransactionRequest();
        req.setAmount(new BigDecimal("500.00"));
        req.setType(Transaction.Type.EXPENSE);
        req.setCategory("Food");
        req.setDate(LocalDate.now());

        mockMvc.perform(put("/api/transactions/" + createdTxId)
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── Dashboard Tests ─────────────────────────────────────────────────────

    @Test @Order(10)
    void analystCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome").isNumber())
                .andExpect(jsonPath("$.data.totalExpenses").isNumber())
                .andExpect(jsonPath("$.data.netBalance").isNumber());
    }

    @Test @Order(11)
    void viewerCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test @Order(12)
    void monthlyTrendsForCurrentYear() throws Exception {
        int year = LocalDate.now().getYear();
        mockMvc.perform(get("/api/dashboard/trends")
                        .param("year", String.valueOf(year))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(12));
    }

    @Test @Order(13)
    void adminCanDeleteTransaction() throws Exception {
        Assumptions.assumeTrue(createdTxId != null);

        mockMvc.perform(delete("/api/transactions/" + createdTxId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Verify it's gone (soft deleted)
        mockMvc.perform(get("/api/transactions/" + createdTxId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test @Order(14)
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(15)
    void validationFailsWithMissingFields() throws Exception {
        // Empty body → should fail validation
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isMap());
    }
}
