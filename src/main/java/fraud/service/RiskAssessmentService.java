package fraud.service;


import fraud.client.dto.BinRangeDetail;
import fraud.model.TransactionRequest;
import fraud.risk.RiskAssessment;
import fraud.risk.RiskLevel;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RiskAssessmentService {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("5000.00");
    private static final String EXPECTED_COUNTRY = "POL"; // Mastercard BIN API often uses ISO-3166 alpha-3 (e.g., POL, USA)

    public RiskAssessment evaluate(TransactionRequest request, BinRangeDetail binDetail) {
        // Rule 0: Unknown BIN is immediately Critical (Level 5)
        if (binDetail == null) {
            return new RiskAssessment(
                    5,
                    RiskLevel.CRITICAL,
                    true,
                    "Flagged as Critical Risk: Card BIN was not found in Mastercard records.",
                    List.of("UNRECOGNIZED_BIN")
            );
        }

        int score = 1; // Baseline: Level 1 (Very Low Risk)
        List<String> reasons = new ArrayList<>();
        List<String> triggeredRules = new ArrayList<>();

        // Rule 1: Prepaid funding source (+1)
        if ("PREPAID".equalsIgnoreCase(binDetail.fundingSource())) {
            score++;
            triggeredRules.add("PREPAID_CARD");
            reasons.add("Card is a prepaid instrument, which carries higher anonymity risk");
        }

        // Rule 2: Cross-border card issuance (+1)
        if (binDetail.countryCode() != null && !EXPECTED_COUNTRY.equalsIgnoreCase(binDetail.countryCode())) {
            score++;
            triggeredRules.add("FOREIGN_ISSUER");
            reasons.add("Card was issued outside expected region (" + binDetail.countryCode() + ")");
        }

        // Rule 3: High transaction amount (+1)
        if (request.amount() != null && request.amount().compareTo(HIGH_AMOUNT_THRESHOLD) > 0) {
            score++;
            triggeredRules.add("HIGH_AMOUNT");
            reasons.add("Transaction amount exceeds " + HIGH_AMOUNT_THRESHOLD);
        }

        // Rule 4: Commercial / Corporate card on high-value payment (+1)
        if ("COMMERCIAL".equalsIgnoreCase(binDetail.brandProductCode())
                && request.amount() != null
                && request.amount().compareTo(new BigDecimal("2500.00")) > 0) {
            score++;
            triggeredRules.add("HIGH_VALUE_COMMERCIAL");
            reasons.add("High-value charge on a commercial/corporate card");
        }

        RiskLevel level = RiskLevel.fromScore(score);
        boolean flagged = level.getScore() >= RiskLevel.HIGH.getScore();

        String summary = triggeredRules.isEmpty()
                ? "Transaction passed all risk checks with no risk factors triggered."
                : String.format("Flagged as %s (Level %d/5): %s.",
                level.getLabel(), level.getScore(), String.join("; ", reasons));

        return new RiskAssessment(level.getScore(), level, flagged, summary, triggeredRules);
    }
}
