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
import kb.dto.Property;
import kb.utils.MyUtils;
import restapi.util.SharedUtil;

/** A service that returns the properties of a single TOSCA node
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/properties")
@Api()
public class PropertyService extends AbstractService {
	/**
	 * Getting the properties of a single TOSCA resource.
	 * @param resource The resource name
	 * @param template Flag that represents if it is template or type
	 * @throws IOException If your input format is invalid
	 * @return The requirements in JSON format
	 * @throws URISyntaxException 
	*/
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the properties of a single TOSCA resource",
//			response = String.class,
			responseContainer = "List")
	public Response getProperty(
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

		// need later a parameter here from the IDE to determine if we are talking about
		// a node or template.
		// For now this is always false, since we are asking only for nodes (not
		// templates)
		Set<Property> properties = api.getProperties(api.getResourceIRI(resource), template);
		api.shutDown();
		JsonObject _properties = new JsonObject();
		JsonArray array = new JsonArray();
		for (Property property : properties) {
			array.add(property.serialise());
		}
		_properties.add("data", array);

		return Response.ok(_properties.toString()).build();
	}
}
