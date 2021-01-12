package restapi.model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import httpclient.AuthConsts;
import httpclient.AuthUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.SodaliteAbstractModel;
import kb.repository.KBConsts;
import restapi.AbstractService;
import restapi.util.SharedUtil;

/** A service that returns all the AADM|RM in KB
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/models")
@Api()
public class GetModelsService extends AbstractService {
	/**
	  * Getting all the AADMs or RMs in KB 
	  * @param type  The type of the model e.g. AADM or RM
	  * @param namespace The namespace to be searched
	  * @param token token
	  * @throws IOException If your input format is invalid
	  * @return All the he models (AADM|RM) 
	 * @throws URISyntaxException 
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the models saved in Knowledge Base",
//			response = String.class,
			responseContainer = "List")
	public Response getModels(@ApiParam(value = "AADM|RM", required = true) @QueryParam("type") String type,
			@ApiParam(value = "namespace", required = false) @DefaultValue("") @QueryParam("namespace") String namespace,
			@ApiParam(value = "token", required = false) @QueryParam("token") String token)
			throws IOException, URISyntaxException {
		
		if(AuthUtil.authentication()) {
			boolean template = type.equals(SharedUtil.AADM);
			Response res = SharedUtil.authForReadRoleFromNamespace(template, namespace, token);
			if (res != null)
				return res;
		}
		
		KBApi api = new KBApi();
		Set<SodaliteAbstractModel> models = api.getModels(type, namespace);
		api.shutDown();
		JsonObject _models = new JsonObject();
		JsonArray array = new JsonArray();
		for (SodaliteAbstractModel sm : models) {
			array.add(sm.serialise());
		}
		_models.add("data", array);

		return Response.ok(_models.toString()).build();
	}
}
