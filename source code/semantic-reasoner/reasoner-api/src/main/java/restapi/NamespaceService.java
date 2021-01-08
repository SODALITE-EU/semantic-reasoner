package restapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import httpclient.AuthUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.configs.ConfigsLoader;
import kb.repository.KB;
import restapi.util.SharedUtil;

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
	
	/**
	 * Getting all the namespaces in KB
	 * @param token token
	 * @throws URISyntaxException 
	*/
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the namespace",
//			response = String.class,
			responseContainer = "List")
	public Response getNamespaces(@ApiParam(value = "token", required = false) @QueryParam("token") String token)
			throws IOException, URISyntaxException {
		
		if(AuthUtil.authentication()) {
			Response res = SharedUtil.authorization(null, null, token, false);
			if (res != null)
				return res;
		}
		
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
