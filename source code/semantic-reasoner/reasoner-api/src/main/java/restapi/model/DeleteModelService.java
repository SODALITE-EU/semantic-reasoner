package restapi.model;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import httpclient.AuthUtil;
import httpclient.dto.AuthResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.SodaliteAbstractModel;
import kb.utils.MyUtils;
import restapi.AbstractService;
import restapi.util.SharedUtil;

/** A service that deletes a model 
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/delete")
@Api()
public class DeleteModelService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(DeleteModelService.class.getName());
	/**
	  * Delete a model in KB
	  * Internal Note: Check for changing the operation from GET to DELETE
	  * @param uri The uri of the model
	  * @param version The version of the model
	  * @throws IOException If your input format is invalid
	  * @return Success or not
	 * @throws URISyntaxException 
	 */
	@DELETE
	@Produces("application/json")
	@ApiOperation(
			value = "Delete a model in Knowledge Base")
//			response = String.class)
	public Response deleteModel(@ApiParam(value = "uri", required = true) @QueryParam("uri") String uri,
			@ApiParam(value = "version", required = false) @QueryParam("version") String version,
			@ApiParam(
					value = "boolean value representing if it is called from the refactorer or the IaC builder",
					required = false,
					defaultValue = "false") @QueryParam("hard") boolean hard,
			@ApiParam(value = "token", required = false) @QueryParam("token") String token)
			throws IOException, URISyntaxException {
		LOG.info( "Delete model uri={}",  uri);

		KBApi api = new KBApi();
		
		if (AuthUtil.authentication()) {
			SodaliteAbstractModel model = api.getModelFromURI(uri, version);
			if (model != null) {
				String shortNamespace = model.getNamespace() == null ? "global" : MyUtils.getNamespaceFromContext(model.getNamespace());
				LOG.info( "Model from URI shortNamespace = {}",  shortNamespace);
				AuthResponse ares = SharedUtil.authForWriteRoleFromNamespace(model.getIsAADM(), shortNamespace, token);
				if(ares.getResponse() != null)
					return ares.getResponse();
			}
		}
		
		boolean res = api.deleteModel(uri, version, hard);
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
