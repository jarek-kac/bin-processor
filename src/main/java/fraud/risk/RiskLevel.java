package fraud.risk;


import java.util.List;

public enum RiskLevel {
    VERY_LOW(1, "Very Low Risk"),
    LOW(2, "Low Risk"),
    MODERATE(3, "Moderate Risk"),
    HIGH(4, "High Risk"),
    CRITICAL(5, "Critical Risk");

    private final int score;
    private final String label;

    RiskLevel(int score, String label) {
        this.score = score;
        this.label = label;
    }

    public int getScore() { return score; }
    public String getLabel() { return label; }

    public static RiskLevel fromScore(int score) {
        int clamped = Math.max(1, Math.min(5, score));
        for (RiskLevel level : values()) {
            if (level.score == clamped) return level;
        }
        return VERY_LOW;
    }
}

