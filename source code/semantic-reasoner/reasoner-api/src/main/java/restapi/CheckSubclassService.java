package restapi;

import java.io.IOException;
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

import httpclient.HttpClientRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;

/** Filter out the node types that are not subclass of a super node type
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/

@Path("/is-subclass-of")
@Api()
public class CheckSubclassService extends AbstractService {
	private static final Logger LOG = Logger.getLogger(CheckSubclassService.class.getName());
	/**
	  * A service that returns all the node types that are subclasses of a specific node type
	  * @param nodeTypes A list with the node types to be checked if they are subclasses of superNodeType
	  * @param superNodeType Represents the superclass
	  * @throws IOException if your input format is invalid
	  * @return All the list of the node types sent without the ones that are not subclass of superNodeType
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the node types that are subclass of a supertype",
			responseContainer = "List")
	public Response isSubclassOf(
			@ApiParam(
					value = "the node types",
					required = true,
					defaultValue = "") @MatrixParam("nodeTypes") List<String> nodeTypes,
			@ApiParam(
					value = "the super node type",
					required = true) @MatrixParam("superNodeType") String superNodeType)
			throws IOException {
		LOG.log(Level.INFO, "nodeTypes = {0}, superNodeType = {1}", new Object[] {nodeTypes, superNodeType});
		KBApi api = new KBApi();
		Set<String> nodes  = api.isSubClassOf(nodeTypes, superNodeType);
		api.shutDown();
		
		JsonObject _nodes = new JsonObject();
		JsonArray array = new JsonArray();
		for (String node : nodes) {
			array.add(node);
		}
		_nodes.add("data", array);

		return Response.ok(_nodes.toString()).build();
	}
}
