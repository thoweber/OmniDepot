package io.omnidepot.format.npm;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/**
 * NPM Registry Protocol Adapter Resource Endpoint (ADR-004).
 */
@Path("/npm")
public class NpmRegistryResource {

    @GET
    @Path("/{packageName}")
    public Response getPackageMetadata(@PathParam("packageName") String packageName) {
        return Response.ok("{\"name\":\"" + packageName + "\",\"versions\":{}}")
                .type("application/json")
                .build();
    }
}
