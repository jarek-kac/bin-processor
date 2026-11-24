package fraud.handler;

import fraud.client.dto.BinRangeDetail;
import fraud.model.TransactionRequest;
import fraud.model.TransactionResponse;
import fraud.risk.RiskAssessment;
import fraud.risk.RiskLevel;
import fraud.service.RiskAssessmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskAssessmentAndMappingUnitTest {

    private RiskAssessmentService riskService;

    @BeforeEach
    void setUp() {
        riskService = new RiskAssessmentService();
    }

    // UNIT TEST 1: Baseline Very Low Risk (Level 1) when all rules pass
    @Test
    void evaluate_domesticDebitOnLowAmount_returnVeryLowRisk() {
        TransactionRequest request = new TransactionRequest(
                "ACC-100", new BigDecimal("56.33"), "PLN"
        );
        BinRangeDetail domesticDebit = new BinRangeDetail(
                "512345", "Bank Poland", "POL", "DEBIT", "MCD", "Debit Mastercard"
        );

        RiskAssessment result = riskService.evaluate(request, domesticDebit);

        assertEquals(1, result.score());
        assertEquals(RiskLevel.VERY_LOW, result.level());
        assertFalse(result.flagged());
        assertTrue(result.triggeredRules().isEmpty());
        assertTrue(result.summary().contains("passed all risk"));
    }

    // UNIT TEST 2: Cumulative Risk Rules (Prepaid + Foreign + High Amount -> Level 4 HIGH)
    @Test
    void evaluate_manyRiskFactors_collectedScoreAndFlagsHighRisk() {
        TransactionRequest request = new TransactionRequest(
                "ACC-200", new BigDecimal("7556.93"), "PLN"
        );
        BinRangeDetail riskyPrepaid = new BinRangeDetail(
                "543210", "BANK NOT POLAND", "USA", "PREPAID", "MPL", "Prepaid Mastercard"
        );

        RiskAssessment result = riskService.evaluate(request, riskyPrepaid);

        assertEquals(4, result.score());
        assertEquals(RiskLevel.HIGH, result.level());
        assertTrue(result.flagged());
        assertEquals(List.of("PREPAID_CARD", "FOREIGN_ISSUER", "HIGH_AMOUNT"), result.triggeredRules());
        assertTrue(result.summary().contains("Level 4/5"));
        assertTrue(result.summary().contains("USA"));
    }

    // UNIT TEST 3: Missing/Unrecognized BIN immediately triggers Level 5 CRITICAL
    @Test
    void evaluate_nullBinDetail_returnsCriticalRisk() {
        TransactionRequest request = new TransactionRequest(
                "ACC-300", new BigDecimal("55.34"), "PLN"
        );

        RiskAssessment result = riskService.evaluate(request, null);

        assertEquals(5, result.score());
        assertEquals(RiskLevel.CRITICAL, result.level());
        assertTrue(result.flagged());
        assertEquals(List.of("UNRECOGNIZED_BIN"), result.triggeredRules());
    }

    // UNIT TEST 4: Response Mapping (Status transitions & CardMetadata mapping)
    @Test
    void responseMapping_mapsStatusAndMetadataAccurately() {
        TransactionRequest request = new TransactionRequest(
                "ACC-400",  new BigDecimal("120.00"), "pln"
        );
        BinRangeDetail bin = new BinRangeDetail(
                "512345", "BANK POLAND", "POL", "CREDIT", "MCS", "Standard Mastercard"
        );

        // 1. Level 1 -> PROCESSED & uppercase currency
        RiskAssessment lowRisk = new RiskAssessment(1, RiskLevel.VERY_LOW, false, "OK", List.of());
        TransactionResponse processed = TransactionResponse.fromEvaluation(request, bin, lowRisk);

        assertEquals("PROCESSED", processed.status());
        assertEquals("PLN", processed.currency());
        assertNotNull(processed.accountId());
        assertEquals("BANK POLAND", processed.cardMetadata().issuerName());
        assertEquals("CREDIT", processed.cardMetadata().fundingSource());

        // 2. Level 4 -> FLAGGED_FOR_REVIEW
        RiskAssessment highRisk = new RiskAssessment(4, RiskLevel.HIGH, true, "Flagged", List.of("HIGH_AMOUNT"));
        TransactionResponse flagged = TransactionResponse.fromEvaluation(request, bin, highRisk);
        assertEquals("FLAGGED_FOR_REVIEW", flagged.status());

        // 3. Level 5 -> DECLINED & null cardMetadata when BIN is missing
        RiskAssessment criticalRisk = new RiskAssessment(5, RiskLevel.CRITICAL, true, "Declined", List.of("UNRECOGNIZED_BIN"));
        TransactionResponse declined = TransactionResponse.fromEvaluation(request, null, criticalRisk);
        assertEquals("DECLINED", declined.status());
        assertNull(declined.cardMetadata());
    }
}
