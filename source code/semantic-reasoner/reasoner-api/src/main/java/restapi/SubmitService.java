package restapi;

import java.io.IOException;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.ClientProtocolException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.dsl.DSLMappingService;
import kb.dsl.exceptions.MappingException;
import kb.repository.KB;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
import restapi.utils.HttpClientRequest;

/** A service that submits the abstract application deployment model to the Knowledge Base.
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/saveAADM")
@Api()
public class SubmitService extends AbstractService {
	/**
	 * Storing the submitted AADM in the KB and assigning a unique id.
	 * @param aadmTTL The AADM in turtle format
	 * @param submissionId A unique id
	 * @throws RDFParseException A parse exception that can be thrown by a parser when it encounters an error
	 * @throws UnsupportedRDFormatException  Α RuntimeException indicating that a specific RDF format is not supported.
	 * @throws IOException If your input format is invalid
	 * @throws MappingException Unknown entity issue
	 * @return The AADM URI
	*/
	@POST
	@Produces("text/plain")
	@Consumes("application/x-www-form-urlencoded")
	@ApiOperation(value = "Stores submitted node templates in the KB")
	public Response saveAADM(@ApiParam(value = "The TTL of AADM", required = true) @FormParam("aadmTTL") String aadmTTL,
			@ApiParam(value = "An id to uniquely identify a submission", required = true) @FormParam("submissionId") String submissionId)
			throws RDFParseException, UnsupportedRDFormatException, IOException, MappingException {

		KB kb;
		String getenv = System.getenv("graphdb");
		if (getenv != null)
			kb = new KB(getenv, "TOSCA");
		else
			kb = new KB();
		DSLMappingService m = new DSLMappingService(kb, aadmTTL, submissionId);
		IRI aadmUri = null;

		//Contains the final response
		JSONObject response = new JSONObject();
		try {
			aadmUri = m.start();
			m.save();
			getWarnings(response, submissionId);
		} catch (MappingException e) {
			e.printStackTrace();
		} catch (ValidationException e) {	
			List<ValidationModel> validationModels = e.validationModels;
			JSONArray array = new JSONArray();
			for (ValidationModel validationModel : validationModels) {
				array.add(validationModel.toJson());
			}
			
			JSONObject errors = new JSONObject();
			errors.put("errors", array);
			return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			m.shutDown();
		}
		
		response.put("aadmuri", aadmUri.stringValue());
		return Response.ok(Status.ACCEPTED).entity(response.toString()).build();
	}
	
	/**
	 * Calling the bug predictor for getting the potential warnings of the model.
	 * @param response The response is the parameter in which the warnings are saved
	 * @throws IOException If your input format is invalid
	 * @throws ClientProtocolException Signals an error in the HTTP protocol.
	 * @throws ParseException Signals that an error has been reached unexpectedly while parsing
	 */
	public void getWarnings(JSONObject response, String submissionId) throws ClientProtocolException, IOException, ParseException {
		String warnings = HttpClientRequest.bugPredictorApi(submissionId);
		if (warnings != "") {
			JSONParser parser = new JSONParser();
			JSONObject warningsJson = (JSONObject) parser.parse(warnings);
			response.put("warnings",warningsJson.get("warnings"));
		}
	}

}
