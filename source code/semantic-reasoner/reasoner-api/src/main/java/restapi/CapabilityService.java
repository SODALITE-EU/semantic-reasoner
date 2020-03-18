package restapi;

import java.io.IOException;
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

/** A service that returns the capabilities of a single TOSCA resource in JSON format
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/capabilities")
@Api()
public class CapabilityService extends AbstractService {
	/**
	  * Getting the capabilities of a single TOSCA resource.
	  * @param resource The resource name
	  * @throws IOException If your input format is invalid
	  * @return The capabilities in JSON format
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the capabilities of a single TOSCA resource",
//			response = String.class,
			responseContainer = "List")
	public Response getCapabilities(
			@ApiParam(
					value = "A TOSCA resource, e.g. a node",
					required = true,
					defaultValue = "my.nodes.SkylineExtractor") @QueryParam("resource") String resource)
			throws IOException {

		KBApi api = new KBApi();
		Set<Capability> capabilities = api.getCapabilities(resource, false);
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
