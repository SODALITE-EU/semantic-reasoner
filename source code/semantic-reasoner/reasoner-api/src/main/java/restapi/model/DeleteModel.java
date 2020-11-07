package restapi.model;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.DefaultValue;
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
import kb.dto.SodaliteAbstractModel;
import restapi.AbstractService;

/** A service that returns 
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/delete")
@Api()
public class DeleteModel extends AbstractService {
	/**
	  * Delete a model in KB 
	  * @param uri 
	  * @throws IOException If your input format is invalid
	  * @return Success or not
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Delete a model in Knowledge Base")
//			response = String.class)
	public Response DeleteModel(@ApiParam(value = "uri", required = true) @QueryParam("uri") String uri)
			throws IOException {

		KBApi api = new KBApi();
		api.deleteModel(uri);
		api.shutDown();

		return Response.ok("ok").build();
	}
}
