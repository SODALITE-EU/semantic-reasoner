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
import kb.dto.Interface;
import kb.utils.MyUtils;
import restapi.util.SharedUtil;

/** A service that returns the interfaces of a single TOSCA node
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/interfaces")
@Api()
public class InterfaceService extends AbstractService {
	/**
	  * Getting the the interfaces of a single TOSCA resource.
	  * @param resource The resource name
	  * @param template Flag that represents if it is template or type
	  * @throws IOException If your input format is invalid
	  * @return The interfaces in JSON format
	 * @throws URISyntaxException 
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the interfaces of a single TOSCA resource",
//			response = String.class,
			responseContainer = "List")
	public Response getInterface(
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
			Response res = SharedUtil.authForReadRoleFromResource(template, resource, token);
			if (res != null)
				return res;
		}


		KBApi api = new KBApi();
		Set<Interface> interfaces = api.getInterfaces(api.getResourceIRI(resource), template);
		api.shutDown();

		JsonObject _interfaces = new JsonObject();
		JsonArray array = new JsonArray();
		for (Interface _interface : interfaces) {
			array.add(_interface.serialise());
		}
		_interfaces.add("data", array);

		return Response.ok(_interfaces.toString()).build();
	}

}
