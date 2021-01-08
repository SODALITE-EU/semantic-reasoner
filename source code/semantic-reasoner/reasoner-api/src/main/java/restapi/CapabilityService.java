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

import httpclient.AuthConsts;
import httpclient.AuthUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.Capability;
import kb.utils.MyUtils;
import restapi.util.SharedUtil;

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
	  * @param template Flag that represents if it is template or type
	  * @throws IOException If your input format is invalid
	  * @return The capabilities in JSON format
	 * @throws URISyntaxException 
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
					defaultValue = "my.nodes.SkylineExtractor") @QueryParam("resource") String resource,
			@ApiParam(
					value = "For template, it is true. For type, it is false",
					required = true,
					defaultValue = "false") @QueryParam("template") boolean template,
			@ApiParam(value = "token", required = false) @QueryParam("token") String token)
			throws IOException, URISyntaxException {
		
		if(AuthUtil.authentication()) {
			String typeOfRole = template ? AuthConsts.AADM_R : AuthConsts.RM_R;
			String _namespace =  MyUtils.getNamespaceFromReference(resource);
			String namespace = _namespace == null ? "global":_namespace;
			Response res = SharedUtil.authorization(AuthUtil.createRoleFromNamespace(namespace, typeOfRole), null, token, true);
			if (res != null)
				return res;
		}

		KBApi api = new KBApi();
		Set<Capability> capabilities = api.getCapabilities(api.getResourceIRI(resource), template);
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
