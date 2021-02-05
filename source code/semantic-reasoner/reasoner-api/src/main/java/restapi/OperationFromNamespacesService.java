package restapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import httpclient.AuthUtil;
import httpclient.dto.AuthResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.configs.ConfigsLoader;
import kb.dto.Operation;
import kb.dto.Property;
import kb.repository.KB;
import kb.repository.KBConsts;
import restapi.util.SharedUtil;

/** A service that returns all the namespaces in KB in JSON format
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/operationsfromnamespaces")
@Api()
public class OperationFromNamespacesService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(OperationFromNamespacesService.class.getName());
	
	static ConfigsLoader configInstance;
	static {
		configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
	}
	
	/**
	 * Gets operations from interface types
	 * @param imports The imported namespaces
	 * @param token token
	 * @throws URISyntaxException 
	*/
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the namespace",
//			response = String.class,
			responseContainer = "List")
	public Response getOperationsFromNamespaces(
			@ApiParam(
			value = "A TOSCA resource, e.g. a namespace for an interface",
			required = false) @QueryParam("imports") List<String> imports,
			@ApiParam(value = "token", required = false) @QueryParam("token") String token)
			throws IOException, URISyntaxException {
		
		LOG.info("resource = {}", imports);
		KBApi api = new KBApi();
		Set<Operation> operations = api.getOperationsFromNamespaces(imports);
		api.shutDown();
		
		JsonObject _operations = new JsonObject();
		JsonArray array = new JsonArray();
		for (Operation operation : operations) {
			array.add(operation.serialise());
		}
		_operations.add("data", array);
		
		return Response.ok(_operations.toString()).build();
	}

}