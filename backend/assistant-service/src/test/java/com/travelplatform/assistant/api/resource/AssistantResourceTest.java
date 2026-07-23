package com.travelplatform.assistant.api.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.travelplatform.assistant.domain.assistant.AssistantUnavailableException;
import com.travelplatform.assistant.infrastructure.llm.OllamaLlmAdapter;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The LLM call is mocked at the concrete bean class (QuarkusMock requires this - a mock of just the
 * LlmPort interface isn't assignable to the registered bean type in this Quarkus version) - this
 * test verifies routing, auth and the response shape, not Ollama itself. A real end-to-end run
 * against Ollama is documented separately in docs/adr/0018-engineering-assistant.md, not repeated
 * here as an automated test (network/model-dependent, would make CI flaky and slow).
 */
@QuarkusTest
@DisplayName("AssistantResource")
class AssistantResourceTest {

    OllamaLlmAdapter llmPort;

    @BeforeEach
    void installMock() {
        llmPort = Mockito.mock(OllamaLlmAdapter.class);
        QuarkusMock.installMockForType(llmPort, OllamaLlmAdapter.class);
    }

    private String tokenWithRole(String role) {
        return Jwt.issuer("travel-platform-identity")
                .subject(UUID.randomUUID().toString())
                .groups(role)
                .sign("privateKey.pem");
    }

    @Test
    @DisplayName("answers a question for an allowed role, grounded in the whole corpus")
    void answersForAnAllowedRole() {
        when(llmPort.complete(anyString(), anyString())).thenReturn("This platform is...");

        given().header("Authorization", "Bearer " + tokenWithRole("MANAGER"))
                .contentType("application/json")
                .body(
                        """
                {"question": "What is this platform?"}
                """)
                .when()
                .post("/api/v1/assistant/ask")
                .then()
                .statusCode(200)
                .body("answer", equalTo("This platform is..."))
                .body("sourcesUsed", not(org.hamcrest.Matchers.empty()));
    }

    @Test
    @DisplayName("rejects a request with no token")
    void withoutTokenIsRejected() {
        given().contentType("application/json")
                .body(
                        """
                {"question": "What is this platform?"}
                """)
                .when()
                .post("/api/v1/assistant/ask")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("rejects a plain USER role")
    void insufficientRoleIsForbidden() {
        given().header("Authorization", "Bearer " + tokenWithRole("USER"))
                .contentType("application/json")
                .body(
                        """
                {"question": "What is this platform?"}
                """)
                .when()
                .post("/api/v1/assistant/ask")
                .then()
                .statusCode(403)
                .body("error", equalTo("FORBIDDEN"));
    }

    @Test
    @DisplayName("rejects a blank question")
    void blankQuestionIsRejected() {
        given().header("Authorization", "Bearer " + tokenWithRole("ADMIN"))
                .contentType("application/json")
                .body(
                        """
                {"question": "   "}
                """)
                .when()
                .post("/api/v1/assistant/ask")
                .then()
                .statusCode(400)
                .body("error", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("surfaces an unreachable Ollama as 503, not a raw exception")
    void unavailableLlmSurfacesAs503() {
        when(llmPort.complete(anyString(), anyString()))
                .thenThrow(
                        new AssistantUnavailableException(
                                new RuntimeException("connection refused")));

        given().header("Authorization", "Bearer " + tokenWithRole("SUPPORT"))
                .contentType("application/json")
                .body(
                        """
                {"question": "What is this platform?"}
                """)
                .when()
                .post("/api/v1/assistant/ask")
                .then()
                .statusCode(503)
                .body("error", equalTo("ASSISTANT_UNAVAILABLE"));
    }
}
