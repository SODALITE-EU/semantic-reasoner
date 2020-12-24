package restapi;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import kb.KBApi;
import kb.configs.ConfigsLoader;
import kb.dto.Capability;
import kb.repository.KB;

/** A service that returns all the namespaces in KB in JSON format
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/namespaces")
@Api()
public class NamespaceService extends AbstractService {
	static ConfigsLoader configInstance;
	static {
		configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
	}
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the namespace",
//			response = String.class,
			responseContainer = "List")
	public Response getNamespaces()
			throws IOException {
		KB kb = new KB(configInstance.getGraphdb(), "TOSCA");
		List<Resource> cList = Iterations.asList(kb.connection.getContextIDs());
		kb.shutDown();
		
		JsonObject _context = new JsonObject();
		JsonArray array = new JsonArray();
		for (Resource  c: cList) {
			array.add(c.toString());
		}
		_context.add("data", array);
		
		return Response.ok(_context.toString()).build();
	}
}
