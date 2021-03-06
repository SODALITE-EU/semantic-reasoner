package restapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import httpclient.AuthConsts;
import httpclient.AuthUtil;
import httpclient.dto.AuthResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.Node;
import restapi.util.SharedUtil;

/** A service that returns all the templates associated to specific namespaces
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/

@Path("/templates")
@Api()
public class TemplateService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(TemplateService.class.getName());
	/**
	  * Getting all the templates in the KB that belong to global namespace and in the imports namespaces
	  * @param imports The namespaces to be searched e.g. docker, snow
	  * @param token token
	  * @throws IOException if your input format is invalid
	  * @return All the relevant templates in json format
	 * @throws URISyntaxException 
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(value = "Returns all the known TOSCA nodes", responseContainer = "List")
	public Response getTemplates(@ApiParam(
			value = "the namespaces",
			required = true,
			defaultValue = "") @MatrixParam("imports") List<String> imports,
			@ApiParam(value = "token", required = false) @MatrixParam("token") String token) throws IOException, URISyntaxException {
		
		 LOG.info( "imports = {}",  Arrays.toString(imports.toArray()));
		 
		 if(AuthUtil.authentication()) {
			 AuthResponse ares = SharedUtil.authForImports(imports, AuthConsts.AADM_R, token);
			 if (ares.getResponse() != null)
				 return ares.getResponse();
		 }		
		 
		 KBApi api = new KBApi();
		 Set<Node> nodes = api.getTemplates(imports);
		 api.shutDown();

		 JsonObject _nodes = new JsonObject();
		 JsonArray array = new JsonArray();
		 for (Node node : nodes) {
			array.add(node.serialise());
		 }
		 _nodes.add("data", array);

		 return Response.ok(_nodes.toString()).build();
	}

}
