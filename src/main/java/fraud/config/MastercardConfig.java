package fraud.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "mastercard.api")
public interface MastercardConfig {
    String consumerKey();
    String keyFilePath();
    String keyAlias();
    String keyPassword();
}
