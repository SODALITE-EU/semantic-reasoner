package restapi;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.google.gson.JsonElement;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.AADM;
import kb.utils.AADMJsonFormat;
import kb.utils.MyUtils;

@Path("/aadm")
@Api()
public class AADMService extends AbstractService {

	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the full JSON of an AADM")
	public Response getAADM(
			@ApiParam(
					value = "The Id (IRI) of the AADM",
					required = true) @QueryParam("aadmIRI") String aadmIRI)
			throws IOException {

		KBApi api = new KBApi();
		AADM aadm = api.getAADM(aadmIRI);
		api.shutDown();

		JsonElement convert = AADMJsonFormat.convert(aadm.serialise());

		return Response.ok(MyUtils.getGson(true).toJson(convert)).build();
	}

	public static void main(String[] args) throws IOException {
		AADMService s = new AADMService();
		Response aadm = s.getAADM(
				"https://www.sodalite.eu/ontologies/snow-blueprint-containerized-OS/AbstractApplicationDeployment_1");
		System.out.println(aadm.getEntity().toString());
	}

}
