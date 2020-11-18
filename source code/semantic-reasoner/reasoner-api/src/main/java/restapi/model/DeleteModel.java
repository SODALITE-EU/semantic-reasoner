package restapi.model;

import java.io.IOException;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;

import restapi.AbstractService;

/** A service that deletes a model 
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
	  * Internal Note: Check for changing the operation from GET to DELETE
	  * @param uri The uri of the model
	  * @throws IOException If your input format is invalid
	  * @return Success or not
	 */
	@DELETE
	@Produces("application/json")
	@ApiOperation(
			value = "Delete a model in Knowledge Base")
//			response = String.class)
	public Response DeleteModel(@ApiParam(value = "uri", required = true) @QueryParam("uri") String uri)
			throws IOException {

		KBApi api = new KBApi();
		boolean res = api.deleteModel(uri);
		api.shutDown();

		JsonObject _result = new JsonObject();
		JsonObject _text = new JsonObject();
		
		if(!res) {
			_text.addProperty("text", "The model does not exist");
			_result.add("failure", _text);
			return Response.status(404).entity(_result.toString()).build();
		}
		
		_text.addProperty("text", "Successfully deleted the model");
		_result.add("success", _text);
		
		return Response.ok(_result.toString()).build();
	}
}
