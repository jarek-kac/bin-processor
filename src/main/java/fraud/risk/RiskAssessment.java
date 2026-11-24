package fraud.risk;

import java.util.List;

public record RiskAssessment(
        int score,              // 1 to 5
        RiskLevel level,        // VERY_LOW -> CRITICAL
        boolean flagged,        // true if score >= 4 (HIGH or CRITICAL)
        String summary,         // explanation
        List<String> triggeredRules
) {}
