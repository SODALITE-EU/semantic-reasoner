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
@Path("/operations")
@Api()
public class OperationService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(OperationService.class.getName());
	
	static ConfigsLoader configInstance;
	static {
		configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
	}
	
	/**
	 * Getting operations for a specific node type
	 * @param resource The resource such as docker/docker_host
	 * @param template Flag that represents if it is template or type
	 * @param token token
	 * @throws URISyntaxException 
	*/
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the namespace",
//			response = String.class,
			responseContainer = "List")
	public Response getOperations(@ApiParam(
			value = "A TOSCA resource, e.g. a node",
			required = true,
			defaultValue = "docker/my.nodes.SkylineExtractor") @QueryParam("resource") String resource,
			@ApiParam(
			value = "For template, it is true. For type, it is false",
			required = true,
			defaultValue = "false") @QueryParam("template") boolean template,
			@ApiParam(value = "token", required = false) @QueryParam("token") String token)
			throws IOException, URISyntaxException {
		
		LOG.info("resource = {}, template = {}", resource, template);
		KBApi api = new KBApi();
		Set<Operation> operations = api.getOperations(api.getResourceIRI(resource), template);
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
