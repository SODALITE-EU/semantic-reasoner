package restapi;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.Node;
import kb.dto.NodeType;

/** A service that returns node types that satisfy a certain requirement of a node template of type nodeType.
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/valid-requirement-nodes-type")
@Api()
public class ValidRequirementNodeTypeService extends AbstractService {
	/**
	 * Getting the node types that satisfy a certain requirement of a node template of type nodeType
	 * @param requirement The name of a requirement
	 * @param nodeType The name of the node type
	 * @throws IOException If your input format is invalid
	 * @return The node types in JSON format
	*/
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns node types that satisfy a certain requirement",
//			response = String.class,
			responseContainer = "List")
	public Response getValidNodeTypes(
			@ApiParam(
					value = "the name of the requirement, e.g. host",
					required = true,
					defaultValue = "host") @QueryParam("requirement") String requirement,
			@ApiParam(
					value = "the node type for which the requirement is relevant, e.g. tosca.nodes.SoftwareComponent",
					required = true,
					defaultValue = "tosca.nodes.SoftwareComponent") @QueryParam("nodeType") String nodeType)
			throws IOException {

		KBApi api = new KBApi();
		Set<NodeType> nodeTypes = api.getRequirementValidNodeType(requirement, nodeType);
		api.shutDown();

		JsonObject _nodesTypes = new JsonObject();
		JsonArray array = new JsonArray();
		for (NodeType nodeT : nodeTypes) {
			array.add(nodeT.serialise());
		}
		_nodesTypes.add("data", array);

		return Response.ok(_nodesTypes.toString()).build();
	}

}
