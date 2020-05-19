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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.dsl.DSLRMMappingService;
import kb.dsl.exceptions.MappingException;
import kb.repository.KB;
import kb.utils.MyUtils;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
import restapi.utils.HttpClientRequest;

@Path("/saveRM")
@Api()
public class SubmitRMService extends AbstractService  {

	@POST
	//@Produces("text/plain")
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	@ApiOperation(value = "Stores submitted node types in the KB")
	public Response saveRM(@ApiParam(value = "The TTL of RM", required = true) @FormParam("rmTTL") String rmTTL,
			@ApiParam(value = "An id to uniquely identify a submission", required = true) @FormParam("rmURI") String rmURI)
			throws RDFParseException, UnsupportedRDFormatException, IOException, MappingException {
		
		KB kb;
		String getenv = System.getenv("graphdb");
		if (getenv != null)
			kb = new KB(getenv, "TOSCA");
		else
			kb = new KB();
		
		
		DSLRMMappingService m = new DSLRMMappingService(kb, rmTTL, rmURI);
		IRI rmUri = null;
		
		//Contains the final response
		JSONObject response = new JSONObject();
		try {
				rmUri = m.start();
				String rmid = MyUtils.getStringPattern(rmUri.toString(), ".*/(RM_.*).*");
				m.save();
				HttpClientRequest.getWarnings(response, rmid);
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
				
		response.put("rmuri", rmUri.stringValue());
		return Response.ok(Status.ACCEPTED).entity(response.toString()).build();
	}

}
