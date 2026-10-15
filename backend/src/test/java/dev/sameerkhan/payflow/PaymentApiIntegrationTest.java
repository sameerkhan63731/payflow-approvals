package dev.sameerkhan.payflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentApiIntegrationTest {

    private static final String WIRE_50K = """
            {"type":"WIRE","amount":50000.00,"currency":"USD","debitAccount":"000123456789",
             "beneficiaryName":"Contoso Steel","beneficiaryAccount":"4400918273",
             "memo":"Invoice 1042","submit":true}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private static RequestPostProcessor user(String name, String... roles) {
        SimpleGrantedAuthority[] authorities = new SimpleGrantedAuthority[roles.length];
        for (int i = 0; i < roles.length; i++) {
            authorities[i] = new SimpleGrantedAuthority("ROLE_" + roles[i]);
        }
        return jwt().jwt(j -> j.subject(name)).authorities(authorities);
    }

    private long createPayment(String body) throws Exception {
        String response = mvc.perform(post("/api/payments").with(user("maya", "INITIATOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    @Test
    void fullMakerCheckerFlowWithMaskedAccounts() throws Exception {
        mvc.perform(post("/api/payments").with(user("maya", "INITIATOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(WIRE_50K))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/payments/")))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.requiredApprovals").value(2))
                .andExpect(jsonPath("$.beneficiaryAccount").value("****8273"))
                .andExpect(jsonPath("$.debitAccount").value("****6789"));

        long id = createPayment(WIRE_50K);

        // The initiator cannot approve, even if they also held the approver role.
        mvc.perform(post("/api/payments/{id}/approve", id).with(user("maya", "INITIATOR", "APPROVER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAKER_CHECKER"));

        mvc.perform(get("/api/payments/{id}", id).with(user("raj", "APPROVER")))
                .andExpect(jsonPath("$.canDecide").value(true));

        mvc.perform(post("/api/payments/{id}/approve", id).with(user("raj", "APPROVER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Matches PO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.approvalsReceived").value(1))
                .andExpect(jsonPath("$.canDecide").value(false));

        mvc.perform(post("/api/payments/{id}/approve", id).with(user("raj", "APPROVER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_DECIDED"));

        mvc.perform(post("/api/payments/{id}/approve", id).with(user("lena", "APPROVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(post("/api/payments/{id}/release", id).with(user("raj", "APPROVER")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/payments/{id}/release", id).with(user("omar", "APPROVER", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));
    }

    @Test
    void rejectionRequiresANote() throws Exception {
        long id = createPayment(WIRE_50K);

        mvc.perform(post("/api/payments/{id}/reject", id).with(user("raj", "APPROVER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NOTE_REQUIRED"));

        mvc.perform(post("/api/payments/{id}/reject", id).with(user("raj", "APPROVER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Wrong beneficiary\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.approvals[0].note").value("Wrong beneficiary"));
    }

    @Test
    void idempotencyKeyPreventsDuplicatePayments() throws Exception {
        String first = mvc.perform(post("/api/payments").with(user("maya", "INITIATOR"))
                        .header("Idempotency-Key", "retry-7f3a")
                        .contentType(MediaType.APPLICATION_JSON).content(WIRE_50K))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String retry = mvc.perform(post("/api/payments").with(user("maya", "INITIATOR"))
                        .header("Idempotency-Key", "retry-7f3a")
                        .contentType(MediaType.APPLICATION_JSON).content(WIRE_50K))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode a = json.readTree(first);
        JsonNode b = json.readTree(retry);
        org.assertj.core.api.Assertions.assertThat(b.get("id").asLong()).isEqualTo(a.get("id").asLong());
        org.assertj.core.api.Assertions.assertThat(b.get("reference").asText()).isEqualTo(a.get("reference").asText());
    }

    @Test
    void validatesInputAndEnforcesRoles() throws Exception {
        mvc.perform(post("/api/payments").with(user("maya", "INITIATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ACH\",\"amount\":-5,\"currency\":\"usd\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/payments").with(user("raj", "APPROVER"))
                        .contentType(MediaType.APPLICATION_JSON).content(WIRE_50K))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/payments/{id}", 999_999).with(user("raj", "APPROVER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void issuesSignedTokensThatTheApiAccepts() throws Exception {
        mvc.perform(post("/api/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"raj\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());

        String body = mvc.perform(post("/api/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"omar\",\"password\":\"demo-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("ADMIN")))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("accessToken").asText();

        mvc.perform(get("/api/payments/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.PENDING_APPROVAL").exists());
    }
}
