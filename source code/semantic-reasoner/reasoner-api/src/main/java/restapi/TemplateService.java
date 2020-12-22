package restapi;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.Node;

/** A service that returns all the templates associated to specific namespaces
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/

@Path("/templates")
@Api()
public class TemplateService extends AbstractService {
	private static final Logger LOG = Logger.getLogger(TemplateService.class.getName());
	/**
	  * Getting all the templates in the KB that belong to global namespace and in the imports namespaces
	  * @param imports The namespaces to be searched e.g. docker, snow
	  * @throws IOException if your input format is invalid
	  * @return All the relevant templates in json format
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(value = "Returns all the known TOSCA nodes", responseContainer = "List")
	public Response getTemplates(@ApiParam(
			value = "the namespaces",
			required = true,
			defaultValue = "") @MatrixParam("imports") List<String> imports) throws IOException {
		
		 LOG.log(Level.WARNING, "imports = {0}",  Arrays.toString(imports.toArray()));
		
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
