package fraud.model;

import fraud.client.dto.BinRangeDetail;
import fraud.risk.RiskAssessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        String accountId,
        BigDecimal amount,
        String currency,
        String status,
        CardMetadata cardMetadata,
        RiskAssessment riskAssessment,
        Instant timestamp
) {
    // Nested record to cleanly encapsulate Mastercard BIN details
    public record CardMetadata(
            String issuerName,
            String countryCode,
            String fundingSource,
            String productName
    ) {
        public static CardMetadata from(BinRangeDetail detail) {
            if (detail == null) return null;
            return new CardMetadata(
                    detail.customerName(),
                    detail.countryCode(),
                    detail.fundingSource(),
                    detail.brandProductName()
            );
        }
    }

    public static TransactionResponse fromEvaluation(
            TransactionRequest request,
            BinRangeDetail binDetail,
            RiskAssessment risk
    ) {
        // Decline automatically on Level 5 (CRITICAL), flag for review on Level 4 (HIGH), otherwise approve
        String status = switch (risk.level()) {
            case CRITICAL -> "DECLINED";
            case HIGH -> "FLAGGED_FOR_REVIEW";
            default -> "PROCESSED";
        };

        return new TransactionResponse(
                request.accountId(),
                request.amount(),
                request.currency().toUpperCase(),
                status,
                CardMetadata.from(binDetail),
                risk,
                Instant.now()
        );
    }
}