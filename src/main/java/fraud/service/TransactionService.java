package fraud.service;

import fraud.client.MastercardBinClient;
import fraud.client.dto.BinLookupRequest;
import fraud.client.dto.BinRangeDetail;
import fraud.model.TransactionRequest;
import fraud.model.TransactionResponse;
import fraud.risk.RiskAssessment;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TransactionService {

    private static final Logger LOG = Logger.getLogger(TransactionService.class);

    @Inject
    @RestClient
    MastercardBinClient binClient;

    @Inject
    RiskAssessmentService riskAssessmentService;

    @CacheResult(cacheName = "bin-lookup-cache")
    public Optional<BinRangeDetail> findPrimaryBin(@CacheKey String binPrefix) {
        LOG.infof("Cache miss for BIN prefix " + binPrefix);

        List<BinRangeDetail> results = binClient.lookupBin(new BinLookupRequest(binPrefix));

        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }




    public TransactionResponse processTransaction(TransactionRequest request) {

        BinRangeDetail primaryBin = findPrimaryBin(request.accountId()).orElse(null);

        RiskAssessment risk = riskAssessmentService.evaluate(request, primaryBin);

        return TransactionResponse.fromEvaluation(request, primaryBin, risk);
    }

    private String extractBinPrefix(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 6) {
            throw new IllegalArgumentException("Card number must contain at least 6 digits");
        }
        return cardNumber.substring(0, Math.min(cardNumber.length(), 8));
    }
}