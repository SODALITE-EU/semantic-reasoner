package restapi;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;



import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;

@Path("/is-subclass-of")
@Api()
public class CheckSubclassService extends AbstractService {
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the capabilities of a single TOSCA resource",
//			response = String.class,
			responseContainer = "Map")
	public Response isSubclassOf(
			@ApiParam(
					value = "the node type",
					required = true) @QueryParam("nodeType") String nodeType,
			@ApiParam(
					value = "the super node type",
					required = true) @QueryParam("superNodeType") String superNodeType)
			throws IOException {

		KBApi api = new KBApi();
		Boolean answer = api.isSubClassOf(nodeType, superNodeType);
		return Response.ok(answer.toString()).build();
	}
}
