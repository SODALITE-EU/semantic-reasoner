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
import kb.dto.Requirement;

/** A service that returns the requirements of a single TOSCA node
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/requirements")
@Api()
public class RequirementService extends AbstractService {
	/**
	 * Getting the requirements of a single TOSCA resource.
	 * @param resource The resource name
	 * @param template Flag that represents if it is template or type
	 * @throws IOException If your input format is invalid
	 * @return The requirements in JSON format
	*/
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the requirements of a single TOSCA resource",
//			response = String.class,
			responseContainer = "List")
	public Response getRequirements(
			@ApiParam(
					value = "A TOSCA resource, e.g. a node",
					required = true,
					defaultValue = "my.nodes.SkylineExtractor") @QueryParam("resource") String resource,
			@ApiParam(
					value = "For template, it is true. For type, it is false",
					required = true,
					defaultValue = "false") @QueryParam("template") boolean template)
			throws IOException {

		KBApi api = new KBApi();
		Set<Requirement> requirements = api.getRequirements(api.getResourceIRI(resource), template);
		api.shutDown();

		JsonObject _requirements = new JsonObject();
		JsonArray array = new JsonArray();
		for (Requirement requirement : requirements) {
			array.add(requirement.serialise());
		}
		_requirements.add("data", array);

		return Response.ok(_requirements.toString()).build();
	}

}
