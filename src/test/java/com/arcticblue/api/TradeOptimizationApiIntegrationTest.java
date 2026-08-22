package com.arcticblue.api;

import com.arcticblue.persistence.OptimizationRunRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TradeOptimizationApiIntegrationTest {

    private static final String EXAMPLE_REQUEST = """
            {
              "maxMargin": 15,
              "candidateTrades": [
                { "tradeName": "Trade Alpha", "marginRequired": 5, "expectedPnl": 120 },
                { "tradeName": "Trade Beta", "marginRequired": 10, "expectedPnl": 200 },
                { "tradeName": "Trade Gamma", "marginRequired": 3, "expectedPnl": 80 },
                { "tradeName": "Trade Delta", "marginRequired": 8, "expectedPnl": 160 }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OptimizationRunRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsPersistsAndRetrievesOptimization() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/trades/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXAMPLE_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.selectedTrades", hasSize(2)))
                .andExpect(jsonPath("$.selectedTrades[0].tradeName").value("Trade Alpha"))
                .andExpect(jsonPath("$.selectedTrades[1].tradeName").value("Trade Beta"))
                .andExpect(jsonPath("$.totalMarginRequired").value(15))
                .andExpect(jsonPath("$.totalExpectedPnl").value(320))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        String requestId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.requestId");

        mockMvc.perform(get("/api/v1/trades/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.totalExpectedPnl").value(320));

        mockMvc.perform(get("/api/v1/trades").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].requestId").value(requestId))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void returnsEmptySelectionWhenNoTradeFits() throws Exception {
        mockMvc.perform(post("/api/v1/trades/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"maxMargin":2,"candidateTrades":[
                                  {"tradeName":"Large","marginRequired":3,"expectedPnl":100}
                                ]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.selectedTrades", hasSize(0)))
                .andExpect(jsonPath("$.totalMarginRequired").value(0))
                .andExpect(jsonPath("$.totalExpectedPnl").value(0));
    }

    @Test
    void rejectsInvalidInputWithDescriptiveErrors() throws Exception {
        mockMvc.perform(post("/api/v1/trades/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"maxMargin":-1,"candidateTrades":[
                                  {"tradeName":"","marginRequired":0,"expectedPnl":10}
                                ]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.maxMargin").exists())
                .andExpect(jsonPath("$.validationErrors['candidateTrades[0].tradeName']").exists())
                .andExpect(jsonPath("$.validationErrors['candidateTrades[0].marginRequired']").exists());
    }

    @Test
    void returnsNotFoundForUnknownRequest() throws Exception {
        mockMvc.perform(get("/api/v1/trades/{requestId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Optimization request not found:")));
    }
}
