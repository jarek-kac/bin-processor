package fraud.resource;


import fraud.model.TransactionRequest;
import fraud.model.TransactionResponse;
import fraud.service.TransactionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@Path("/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityScheme(
        securitySchemeName = "bearer",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class TransactionResource {

    @Inject
    TransactionService transactionService;

    @POST
    @RolesAllowed({"User", "Admin"})
    @SecurityRequirement(name = "bearer")
    public Response createTransaction(TransactionRequest request) {
        TransactionResponse response = transactionService.processTransaction(request);
        return Response.status(Response.Status.OK).entity(response).build();
    }

}
