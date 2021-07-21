package restapi;


import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.configs.ConfigsLoader;
import kb.dto.NodeFull;

/** A service that returns the full information of a node
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/nodeFull")
@Api()
public class NodeFullService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(NodeFullService.class.getName());
	
	/**
	  * Getting the full information of a single TOSCA resource.
	  * @param resource The resource name
	  * @return The node full information in JSON format
	 * @throws IOException 
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the node full information of a single TOSCA resource",
			responseContainer = "List")
	public Response getNodeFull(
			@ApiParam(
					value = "A TOSCA resource, e.g. a node",
					required = true,
					defaultValue = "") @QueryParam("resource") String resource) throws IOException {
		LOG.info("resource: {}", resource);

		KBApi api = new KBApi();
		
		String resourceIRI = api.getResourceIRI(resource);
		LOG.info("api.getResourceIRI(resource): {}", resourceIRI);
		
		NodeFull nodeFull = api.getNode(resourceIRI, true);
		
		JsonElement serialise = nodeFull.serialise();

		return Response.ok(serialise.toString()).build();
	}
}
