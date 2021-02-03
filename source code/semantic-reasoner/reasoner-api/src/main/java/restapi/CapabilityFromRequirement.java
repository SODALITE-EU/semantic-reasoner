package restapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.Capability;

@Path("/capability-from-requirement")
@Api()
public class CapabilityFromRequirement {
	/**
	  * Getting the capabilities of a single TOSCA resource for a specific requirement.
	  * @param resource  type or template
	  * @param requirement The requirement name e.g. protected_by
	  * @throws IOException If your input format is invalid
	  * @return The capabilities in JSON format
	 * @throws URISyntaxException 
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the capabilities of a specific requirement of a single TOSCA resource",
//			response = String.class,
			responseContainer = "List")
	public Response getCapabilitiesFromRequirements (
			@ApiParam(
					value = "A TOSCA resource, e.g. a node",
					required = true,
					defaultValue = "sodalite.nodes.DockerNetwork") @QueryParam("resource") String resource,
			@ApiParam(
					value = "A requirement name",
					required = true) @QueryParam("requirement") String requirement,
			@ApiParam(
					value = "For template, it is true. For type, it is false",
					required = true,
					defaultValue = "false") @QueryParam("template") boolean template,
			@ApiParam(value = "token", required = false) @QueryParam("token") String token)
			throws IOException, URISyntaxException {
		
		KBApi api = new KBApi();
		
		Set<Capability> capabilities = api.getCapabilitiesFromRequirements(resource, requirement, template);
		api.shutDown();
		JsonObject _capabilities = new JsonObject();
		JsonArray array = new JsonArray();
		for (Capability capability : capabilities) {
			array.add(capability.serialise());
		}
		_capabilities.add("data", array);

		return Response.ok(_capabilities.toString()).build();		
	}
	
}
