package fraud.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercard.developer.oauth.OAuth;
import com.mastercard.developer.utils.AuthenticationUtils;
import fraud.config.MastercardConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

@Provider
@ApplicationScoped
public class MastercardOAuthFilter implements ClientRequestFilter {

    @Inject
    MastercardConfig config;

    @Inject
    ObjectMapper objectMapper;

    private PrivateKey signingKey;

    @PostConstruct
    void init() {
        try {
            // Load the .p12 private key once at application startup
            this.signingKey = AuthenticationUtils.loadSigningKey(
                    config.keyFilePath(),
                    config.keyAlias(),
                    config.keyPassword()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Mastercard .p12 signing key", e);
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        Object requestId = org.jboss.logging.MDC.get("requestId");
        if (requestId != null) {
            requestContext.getHeaders().putSingle("X-REQUESTID", requestId.toString());
        }

        String payload = "";
        if (requestContext.hasEntity()) {
            payload = objectMapper.writeValueAsString(requestContext.getEntity());
        }

        String authHeader = OAuth.getAuthorizationHeader(
                requestContext.getUri(),
                requestContext.getMethod(),
                payload,
                StandardCharsets.UTF_8,
                config.consumerKey(),
                signingKey
        );

        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, authHeader);
    }
}
