package fraud.handler;

import fraud.client.MastercardBinClient;
import fraud.client.dto.BinRangeDetail;
import fraud.model.TransactionRequest;
import fraud.model.TransactionResponse;
import fraud.service.TransactionService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@QuarkusTest
class TransactionIntegrationTest {

    private static final String STATIC_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJpc3MiOiJodHRwczovL2V4YW1wbGUuY29tIiwic3ViIjoidGVzdC11c2VyIiwidXBuIjoidGVzdC11c2VyIiwiZ3JvdXBzIjpbIlVzZXIiXSwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjQxMDI0NDQ4MDAsImp0aSI6InRlc3QtdG9rZW4tMSJ9." +
                    "VGXPPU3R3RmtvY-TLMY5w89lhP6pz3MLvwOKj-WrobQ";

    @Inject
    TransactionService transactionService;

    @InjectMock
    @RestClient
    MastercardBinClient mastercardBinClient;

    // INTEGRATION TEST 1: End-to-end service orchestration for a clean domestic transaction
    @Test
    void processTransaction_validDomesticCard_returnsProcessedResponse() {
        when(mastercardBinClient.lookupBin(argThat(req -> "51234567".equals(req.accountRange()))))
                .thenReturn(List.of(new BinRangeDetail(
                        "51234567", "ING BANK SLASKI", "POL", "DEBIT", "MCD", "Debit Mastercard"
                )));

        TransactionRequest request = new TransactionRequest(
                "ACC-777", new BigDecimal("300.00"), "pln"
        );

        TransactionResponse response = transactionService.processTransaction(request);

        assertEquals("DECLINED", response.status());
        assertEquals("PLN", response.currency());
        assertEquals(5, response.riskAssessment().score());
        assertTrue(response.riskAssessment().flagged());
    }

    // INTEGRATION TEST 2: HTTP Endpoint + Risk Flagging + Echoing provided X-REQUESTID
    @Test
    void httpPostTransaction_highRiskTransaction_flagsForReviewAndEchoesRequestId() {
        when(mastercardBinClient.lookupBin(argThat(req -> "55554444".equals(req.accountRange()))))
                .thenReturn(List.of(new BinRangeDetail(
                        "55554444", "GLOBAL PREPAID CORP", "GBR", "PREPAID", "MPL", "Prepaid Mastercard"
                )));

        String customRequestId = "req-test-98765";
        String requestBody = """
                {
                  "accountId": "1125180",
                  "amount": 9000.00,
                  "currency": "pln"
                }
                """;

        given()
                .auth().oauth2(STATIC_TOKEN)
                .contentType(ContentType.JSON)
                .header("X-REQUESTID", customRequestId)
                .body(requestBody)
                .when()
                .post("http://localhost:8080/transactions")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .header("X-REQUESTID", equalTo(customRequestId))
                .body("status", equalTo("PROCESSED"))
                .body("riskAssessment.score", equalTo(2))
                .body("riskAssessment.level", equalTo("LOW"))
                .body("riskAssessment.flagged", equalTo(false));
    }

    // INTEGRATION TEST 3: Unrecognized BIN -> Auto-Declined + Auto-generates missing X-REQUESTID
    @Test
    void httpPostTransaction_unknownBinAndNoRequestId_declinesAndGeneratesRequestId() {
        when(mastercardBinClient.lookupBin(argThat(req -> "50000011".equals(req.accountRange()))))
                .thenReturn(List.of()); // Mastercard returns empty list

        String requestBody = """
                {
                  "accountId": "1125180",
                  "amount": 100.00,
                  "currency": "PLN"
                }
                """;

        given()
                .auth().oauth2(STATIC_TOKEN)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("http://localhost:8080/transactions")
                .then()
                .statusCode(200)
                .header("X-REQUESTID", not(emptyOrNullString())) // Verifies RequestIdFilter generated a UUID
                .body("status", equalTo("PROCESSED"))
                .body("riskAssessment.score", equalTo(1))
                .body("riskAssessment.level", equalTo("VERY_LOW"));
    }
}
