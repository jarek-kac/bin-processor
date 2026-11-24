package fraud.client;

import fraud.client.dto.BinLookupRequest;
import fraud.client.dto.BinRangeDetail;
import fraud.filters.MastercardOAuthFilter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;


@RegisterRestClient(configKey = "mastercard-bin-api")
@RegisterProvider(MastercardOAuthFilter.class)
public interface MastercardBinClient {

    @POST
    @Path("/bin-ranges/account-searches")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    List<BinRangeDetail> lookupBin(BinLookupRequest request);
}