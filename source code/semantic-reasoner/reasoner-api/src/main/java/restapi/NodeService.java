package restapi;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import kb.KBApi;
import kb.dto.Node;

/** A service that returns all the known TOSCA nodes in the KB
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/nodes")
@Api()
public class NodeService extends AbstractService {
	/**
	  * Getting all the known TOSCA nodes in the KB
	  * @throws IOException if your input format is invalid
	  * @return All the TOSCA nodes in JSON format
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(value = "Returns all the known TOSCA nodes", responseContainer = "List")
	public Response getNodes() throws IOException {

		KBApi api = new KBApi();
		Set<Node> nodes = api.getNodes();
		api.shutDown();
		// Gson gson = new Gson();

		JsonObject _nodes = new JsonObject();
		JsonArray array = new JsonArray();
		for (Node node : nodes) {
			array.add(node.serialise());
		}
		_nodes.add("data", array);

		return Response.ok(_nodes.toString()).build();
	}

}
