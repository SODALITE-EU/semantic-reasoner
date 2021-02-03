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

import httpclient.AuthUtil;
import httpclient.dto.AuthResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import kb.KBApi;
import kb.dto.Attribute;
import kb.repository.KBConsts;
import restapi.util.SharedUtil;

/** A service that returns the attributes of a single TOSCA node
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@SwaggerDefinition(
		info = @Info(
				description = "REST API for the SODALITE Reasoner",
				version = "0.6",
				title = "SODALITE REST API",
				contact = @Contact(name = "George Meditskos", email = "gmeditsk@iti.gr")),
		produces = {
				"application/json"
		},
		schemes = { SwaggerDefinition.Scheme.HTTP },
		basePath = "reasoner-api/v0.6",
		host = "CERTH HOST IP")

@Path("/attributes")
@Api()
public class AttributeService extends AbstractService {
	/**
	  * Getting the attributes of a single TOSCA resource.
	  * @param resource The resource name
	  * @param template Flag that represents if it is template or type
	  * @param token token
	  * @throws IOException If your input format is invalid
	  * @return The attributes in JSON format
	  * @throws URISyntaxException 
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the attributes of a single TOSCA resource",
//			response = Attribute.class,
			responseContainer = "List")
	public Response getAttributeProperty(
			@ApiParam(
					value = "A TOSCA resource, e.g. a node",
					required = true,
					defaultValue = "tosca.nodes.Compute") @QueryParam("resource") String resource,
			@ApiParam(
					value = "For template, it is true. For type, it is false",
					required = true,
					defaultValue = "false") @QueryParam("template") boolean template,
			@ApiParam(value = "token", required = false) @QueryParam("token") String token)
			throws IOException, URISyntaxException {
		
		if(AuthUtil.authentication()) {
			AuthResponse ares = SharedUtil.authForReadRoleFromResource(template, resource, token);
			if (ares.getResponse() != null)
				return ares.getResponse();
		}
		
		KBApi api = new KBApi();
		Set<Attribute> attributes = api.getAttributes(api.getResourceIRI(resource), template, !KBConsts.AADM_JSON);
		api.shutDown();

		JsonObject _attributes = new JsonObject();
		JsonArray array = new JsonArray();
		for (Attribute attribute : attributes) {
			array.add(attribute.serialise());
		}
		_attributes.add("data", array);

		return Response.ok(_attributes.toString()).build();
	}

}
