package restapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import httpclient.AuthUtil;
import httpclient.HttpClientRequest;
import httpclient.dto.AuthResponse;
import httpclient.dto.HttpRequestErrorModel;
import httpclient.exceptions.MyRestTemplateException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.configs.ConfigsLoader;
import kb.dsl.DSLRMMappingService;
import kb.dsl.exceptions.MappingException;
import kb.dsl.exceptions.models.DslValidationModel;
import kb.repository.KB;
import kb.repository.KBConsts;
import kb.utils.MyUtils;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
import restapi.util.SharedUtil;

/** A service that submits the resource model to the Knowledge Base.
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/saveRM")
@Api()
public class SubmitRMService extends AbstractService  {
	private static final Logger LOG = LoggerFactory.getLogger(SubmitRMService.class.getName());
	static ConfigsLoader configInstance;
	static {
		configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
	}
	/**
	 * Storing the submitted RM in the KB and assigning a unique id.
	 * @param  rmTTL The RM in turtle format
	 * @param  rmURI A unique id
	 * @param  rmDSL The original DSL of the resource model
	 * @param  namespace The namespace of the resource model e.g. docker
	 * @param  name The filename of the model
	 * @param token The token
	 * @throws RDFParseException A parse exception that can be thrown by a parser when it encounters an error
	 * @throws UnsupportedRDFormatException   RuntimeException indicating that a specific RDF format is not supported.
	 * @throws IOException If your input format is invalid
	 * @throws MappingException Unknown entity issue
	 * @return The AADM URI
	 * @throws URISyntaxException 
	*/
	@POST
	//@Produces("text/plain")
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	@ApiOperation(value = "Stores submitted node types in the KB")
	public Response saveRM(@ApiParam(value = "The TTL of RM", required = true) @FormParam("rmTTL") String rmTTL,
			@ApiParam(value = "An id to uniquely identify a submission", required = true)  @DefaultValue("") @FormParam("rmURI") String rmURI,
			@ApiParam(value = "The rm in DSL", required = true) @FormParam("rmDSL") String rmDSL,
			@ApiParam(value = "namespace", required = false) @DefaultValue("") @FormParam("namespace") String namespace,
			@ApiParam(value = "name", required = false) @DefaultValue("") @FormParam("name") String name,
			@ApiParam(value = "token") @FormParam("token") String token)
			throws RDFParseException, UnsupportedRDFormatException, IOException, MappingException, URISyntaxException {
	
		LOG.info("rmURI = {}, namespace = {}, name = {}", rmURI, namespace, name);
		if(AuthUtil.authentication()) {
			AuthResponse ares = SharedUtil.authForWriteRoleFromNamespace(!SharedUtil.IS_AADM, namespace, token);
			if (ares.getResponse() != null)
				return ares.getResponse();
		}
		
		KB kb = new KB(configInstance.getGraphdb(), "TOSCA");
		
		DSLRMMappingService m = new DSLRMMappingService(kb, rmTTL, rmURI, namespace, rmDSL, name);
		IRI rmUri = null;
		
		//Contains the final response
		JSONObject response = new JSONObject();
		try {
			rmUri = m.start();
			//String rmid = MyUtils.getStringPattern(rmUri.toString(), ".*/(AADM_.*).*");
			m.save();
			
			HttpClientRequest.getWarnings(response, rmUri.toString(), !KBConsts.AADM);
		} catch (MappingException e) {
			e.printStackTrace();
			List<DslValidationModel> validationModels = e.mappingValidationModels;
			JSONArray array = new JSONArray();
			for (DslValidationModel validationModel : validationModels) {
				array.add(validationModel.toJson());
			}
				
			JSONObject errors = new JSONObject();
			errors.put("errors", array);
			return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
		} catch (ValidationException e) {	
			List<ValidationModel> validationModels = e.validationModels;
			JSONArray array = new JSONArray();
			for (ValidationModel validationModel : validationModels) {
				array.add(validationModel.toJson());
			}
					
			JSONObject errors = new JSONObject();
			errors.put("errors", array);
			return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
		}  catch (MyRestTemplateException e) {
			if (rmUri != null) {
				KBApi api = new KBApi(kb);
				api.deleteModel(rmUri.toString());
			}
			
			HttpRequestErrorModel erm = e.error_model;
			LOG.error("rawStatus={}, api={}, statusCode={}, error={}", erm.rawStatus, erm.api, erm.statusCode, erm.error);
		 	return Response.status(erm.rawStatus).entity(erm.toJson().toString()).build();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("There was an internal server error").build();
		} finally {
			m.shutDown();
		}

		if (rmUri != null)
			response.put("rmuri", rmUri.stringValue());
		return Response.ok(Status.ACCEPTED).entity(response.toString()).build();
	}

}
