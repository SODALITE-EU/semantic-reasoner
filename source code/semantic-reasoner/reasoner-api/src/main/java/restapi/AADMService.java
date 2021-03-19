package restapi;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/** A service that returns the abstract application deployment model in JSON format.
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/aadm")
@Api()
public class AADMService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(AADMService.class.getName());
	/**
	 * Getting the abstract application deployment model in JSON format.
	 * @param aadmIRI The IRI of AADM
	 * @throws IOException if your input format is invalid
	 * @return The AADM in JSON format
	*/
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the full JSON of an AADM")
	public Response getAADM(
			@ApiParam(
					value = "The Id (IRI) of the AADM",
					required = true) @QueryParam("aadmIRI") String aadmIRI,
			@ApiParam(
					value = "boolean value representing if it is called from the refactorer or the IaC builder",
					required = false,
					defaultValue = "false") @QueryParam("refactorer") boolean refactorer)
			throws IOException {

		KBApi api = new KBApi();
		AADM aadm = api.getAADM(aadmIRI);
		aadm.setForRefactorer(refactorer);
		api.shutDown();

		JsonElement convert = AADMJsonFormat.convert(aadm.serialise());

		return Response.ok(MyUtils.getGson(true).toJson(convert)).build();
	}

	public static void main(String[] args) throws IOException {
		AADMService s = new AADMService();
		Response aadm = s.getAADM(
				"https://www.sodalite.eu/ontologies/snow-blueprint-containerized-OS/AbstractApplicationDeployment_1", false);
		LOG.info("AADM = {}", aadm.getEntity().toString());
	}

}
