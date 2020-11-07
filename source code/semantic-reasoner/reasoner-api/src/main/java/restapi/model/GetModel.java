package restapi.model;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.SodaliteAbstractModel;
import restapi.AbstractService;

/** A service that returns 
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/model")
@Api()
public class GetModel extends AbstractService {
	/**
	  * Getting all the AADMs or RMs in KB 
	  * @param resource  (e.g. node type name or template name) is provided
	  * @param namespace If empty global namespace is searched, otherwise the provided one
	  * @param uri If uri is provided
	  * @throws IOException If your input format is invalid
	  * @return The related model
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the model saved in Knowledge Base",
//			response = String.class,
			responseContainer = "List")
	public Response getModel(@ApiParam(value = "resource name", required = false) @DefaultValue("") @QueryParam("resource") String resource,
			@ApiParam(value = "empty or namespace value", required = false) @DefaultValue("") @QueryParam("namespace") String namespace,
			@ApiParam(value = "uri", required = false)@DefaultValue("")  @QueryParam("uri") String uri)
			throws IOException {
		SodaliteAbstractModel model = null;
		
		KBApi api = new KBApi();
		if (!"".equals(resource)) {
			model = api.getModelForResource(resource, namespace);
		} else if (!"".equals(uri)) {
			model = api.getModelFromURI(uri);
		} else {
			return Response.status(Status.BAD_REQUEST).entity("Resource or uri should be provided").build();
		}
		
		api.shutDown();
		JsonObject _model = new JsonObject();
		JsonArray array = new JsonArray();
		if (model != null)
			array.add(model.serialise());
			
		_model.add("data", array);

		return Response.ok(_model.toString()).build();
	}
}
