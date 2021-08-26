package restapi;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.JsonElement;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.RM;
import kb.utils.AADMJsonFormat;
import kb.utils.MyUtils;

/** A service that returns the resource model in JSON format.
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/rm")
@Api()
public class RMService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(AADMService.class.getName());
	/**
	 * Getting the resource model in JSON format.
	 * It is used by the IDE so as to create DSL of the models that are saved by the Platform Discovery Service
	 * @param rmIRI The IRI of RM
	 * @throws IOException if your input format is invalid
	 * @return The RM in JSON format
	*/
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the JSON of a RM with only the nodes included in the model")
	public Response getRM(
			@ApiParam(
					value = "The Id (IRI) of the RM",
					required = true) @QueryParam("rmIRI") String rmIRI
			)
			throws IOException {

		KBApi api = new KBApi();
		RM rm = api.getRM(rmIRI);
		api.shutDown();
		if (rm != null) {
			JsonElement convert = AADMJsonFormat.convert(rm.serialise());
			return Response.ok(MyUtils.getGson(true).toJson(convert)).build();
		} else
			return Response.status(Status.NOT_FOUND).entity("RM is not found").build();
		
	}

	public static void main(String[] args) throws IOException {
		RMService s = new RMService();
		Response rm = s.getRM(
				"https://www.sodalite.eu/ontologies/snow-blueprint-containerized-OS/AbstractApplicationDeployment_1");
		LOG.info("AADM = {}", rm.getEntity().toString());
	}

}
