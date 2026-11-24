package fraud.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION - 10) // Run very early in the request lifecycle
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String REQUEST_ID_HEADER = "X-REQUESTID";
    public static final String MDC_KEY = "requestId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(REQUEST_ID_HEADER);

        // Generate a new UUID if the caller did not provide X-REQUESTID
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // Store on the request context and in JBoss Logging MDC
        requestContext.setProperty(REQUEST_ID_HEADER, requestId);
        MDC.put(MDC_KEY, requestId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Echo X-REQUESTID back in the HTTP response headers
        Object requestId = requestContext.getProperty(REQUEST_ID_HEADER);
        if (requestId != null) {
            responseContext.getHeaders().putSingle(REQUEST_ID_HEADER, requestId.toString());
        }

        // Always clear MDC to prevent thread-pool leakage
        MDC.remove(MDC_KEY);
    }
}