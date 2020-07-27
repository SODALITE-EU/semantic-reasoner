package restapi;

import java.io.IOException;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;


import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.dsl.DSLMappingService;
import kb.dsl.exceptions.MappingException;
import kb.repository.KB;
import kb.utils.ConfigsLoader;
import kb.utils.MyUtils;
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
	static ConfigsLoader configInstance;
	static {
		configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
	}
	
	/**
	 * Storing the submitted AADM in the KB and assigning a unique id.
	 * @param aadmTTL The AADM in turtle format
	 * @param aadmURI A unique id
	 * @throws RDFParseException A parse exception that can be thrown by a parser when it encounters an error
	 * @throws UnsupportedRDFormatException   RuntimeException indicating that a specific RDF format is not supported.
	 * @throws IOException If your input format is invalid
	 * @throws MappingException Unknown entity issue
	 * @return The AADM URI
	*/
	@POST
	//@Produces("text/plain")
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	@ApiOperation(value = "Stores submitted node templates in the KB")
	public Response saveAADM(@ApiParam(value = "The TTL of AADM", required = true) @FormParam("aadmTTL") String aadmTTL,
			@ApiParam(value = "An id to uniquely identify a submission", required = true) @FormParam("aadmURI") String aadmURI,
			@ApiParam(value = "A flag to enable the auto-completion of missing elements in models", required = false) @DefaultValue("false") @FormParam("complete") boolean complete)
			throws RDFParseException, UnsupportedRDFormatException, IOException, MappingException {
		
		KB kb = new KB(configInstance.getGraphdb(), "TOSCA");
		
		DSLMappingService m = new DSLMappingService(kb, aadmTTL, aadmURI, complete);
		IRI aadmUri = null;

		//Contains the final response
		JSONObject response = new JSONObject();
		try {
			aadmUri = m.start();
			String aadmid = MyUtils.getStringPattern(aadmUri.toString(), ".*/(AADM_.*).*");
			m.save();
			if(!HttpClientRequest.getWarnings(response, aadmid)) {
				kb.connection.clear(m.getContext());
				return Response.status(Status.BAD_REQUEST).entity("Error connecting to host " + configInstance.getBugPredictorServer()).build();
			}
			
			addRequirementModels(m, response);
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
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("There was an internal server error").build();
		} finally {
			m.shutDown();
		}
		
		response.put("aadmuri", aadmUri.stringValue());
		return Response.ok(Status.ACCEPTED).entity(response.toString()).build();
	}
	
	public void addRequirementModels(DSLMappingService m, JSONObject response) {
		JSONArray marray = new JSONArray();
		for (ValidationModel validationModel : m.getModifiedModels()) {
			marray.add(validationModel.toJson());
		}
		response.put("modifications", marray);
		
		JSONArray sarray = new JSONArray();
		for (ValidationModel validationModel : m.getSuggestedModels()) {
			sarray.add(validationModel.toJson());
		}
		response.put("suggestions", sarray);
	}
}
