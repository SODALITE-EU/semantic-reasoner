package restapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
import kb.clean.ModifyKB;
import kb.configs.ConfigsLoader;
import kb.dsl.DSLMappingService;
import kb.dsl.dto.DslModel;
import kb.dsl.exceptions.MappingException;
import kb.dsl.exceptions.models.DslValidationModel;
import kb.repository.KB;
import kb.repository.KBConsts;
import kb.utils.MyUtils;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
import restapi.util.SharedUtil;


/** A service that submits the abstract application deployment model to the Knowledge Base.
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/saveAADM")
@Api()
public class SubmitService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(SubmitService.class.getName());
	static ConfigsLoader configInstance;
	static {
		configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
	}
	
	/**
	 * Storing the submitted AADM in the KB and assigning a unique id.
	 * @param aadmTTL The AADM in turtle format
	 * @param aadmURI A unique id
	 * @param aadmDSL The original DSL of the aadm
	 * @param complete flag for auto-completion of entities
	 * @param namespace The namespace of the model
	 * @param name The file name of the model
	 * @param token The token
	 * @throws RDFParseException A parse exception that can be thrown by a parser when it encounters an error
	 * @throws UnsupportedRDFormatException   RuntimeException indicating that a specific RDF format is not supported.
	 * @throws IOException If your input format is invalid
	 * @throws MappingException Unknown entity issue
	 * @return The AADM URI
	 * @throws URISyntaxException 
	 * @throws MyRestTemplateException 
	*/
	@POST
	//@Produces("text/plain")
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	@ApiOperation(value = "Stores submitted node templates in the KB")
	public Response saveAADM(@ApiParam(value = "The TTL of AADM", required = true) @FormParam("aadmTTL") String aadmTTL,
			@ApiParam(value = "An id to uniquely identify a submission", required = true) @DefaultValue("") @FormParam("aadmURI") String aadmURI,
			@ApiParam(value = "The aadm in DSL", required = true) @DefaultValue("") @FormParam("aadmDSL") String aadmDSL,
			@ApiParam(value = "A flag to enable the auto-completion of missing elements in models", required = false) @DefaultValue("false") @FormParam("complete") boolean complete,
			@ApiParam(value = "namespace", required = false) @DefaultValue("") @FormParam("namespace") String namespace,
			@ApiParam(value = "name", required = false) @DefaultValue("") @FormParam("name") String name,
			@ApiParam(value = "version", required = false) @DefaultValue("") @FormParam("version") String version,
			@ApiParam(value = "token") @FormParam("token") String token)
			throws RDFParseException, UnsupportedRDFormatException, IOException, MappingException, MyRestTemplateException, URISyntaxException {
		
		long startTime = Instant.now().toEpochMilli();
		
		if(AuthUtil.authentication()) {
			AuthResponse ares = SharedUtil.authForWriteRoleFromNamespace(SharedUtil.IS_AADM, namespace, token);
			if (ares.getResponse() != null)
				return ares.getResponse();
		}
					
		KB kb = new KB(configInstance.getGraphdb(), KB.REPOSITORY);
		
		DSLMappingService m = new DSLMappingService(kb, aadmTTL, aadmURI, complete, namespace, aadmDSL, name, version);
		DslModel aadm = null;
		//Contains the final response
		JSONObject response = new JSONObject();
			
		try {
			//aadmUri = m.start();
			aadm = m.start();
			//String aadmid = MyUtils.getStringPattern(aadmUri.toString(), ".*/(AADM_.*).*");
			m.save();
			HttpClientRequest.getWarnings(response, aadm.getFullUri(), KBConsts.AADM);

			addRequirementModels(m, response);
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
			
			long endTime = Instant.now().toEpochMilli();
			long timeElapsed = endTime - startTime;
			LOG.info("Submit AADM execution time in milliseconds: " + timeElapsed);
			return Response.status(Status.BAD_REQUEST).entity(errors.toString()).build();
		} catch (MyRestTemplateException e) {
			if (aadm != null) {
				KBApi api = new KBApi(kb);
				api.deleteModel(aadm.getUri(), version, false);
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
		if (aadm != null) {
			response.put( "uri", aadm.getUri());
			response.put( "version", aadm.getVersion());
		}
		
		long endTime = Instant.now().toEpochMilli();
		long timeElapsed = endTime - startTime;
		LOG.info("Submit AADM execution time in milliseconds: " + timeElapsed);
		
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
