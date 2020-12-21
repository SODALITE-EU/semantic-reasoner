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

/** A service that returns all the known TOSCA nodes in the KB
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/types")
@Api()
public class TypeService extends AbstractService {
	private static final Logger LOG = Logger.getLogger(TypeService.class.getName());
	/**
	  * Getting all the known TOSCA nodes in the KB
	  * @param imports The namespaces to be searched e.g. docker, snow
	  * @param type Represents the kinf of the type: capability, data, node, relationship, interface
	  * @throws IOException if your input format is invalid
	  * @return All the TOSCA nodes in JSON format
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(value = "Returns all the known TOSCA nodes", responseContainer = "List")
	public Response getNodes(@ApiParam(
			value = "the namespaces",
			required = true,
			defaultValue = "") @MatrixParam("imports") List<String> imports, @ApiParam(
					value = "the type e.g. data, node",
					required = true,
					defaultValue = "") @MatrixParam("type") String type ) throws IOException {
		
		 LOG.log(Level.INFO, "imports are {0}, type is {1}", new Object[] {Arrays.toString(imports.toArray()), type});
		 
		 
		KBApi api = new KBApi();
		Set<Node> nodes = api.getNodes(imports, type);
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
