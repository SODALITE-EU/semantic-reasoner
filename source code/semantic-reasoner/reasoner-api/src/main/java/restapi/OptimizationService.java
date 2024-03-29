package restapi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.ClientProtocolException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import httpclient.AuthConsts;
import httpclient.AuthUtil;
import httpclient.HttpClientRequest;
import httpclient.dto.AuthResponse;
import httpclient.dto.HttpRequestErrorModel;
import httpclient.exceptions.MyRestTemplateException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.KBApiOptimizations;
import kb.clean.ModifyKB;
import kb.configs.ConfigsLoader;
import kb.dsl.DSLMappingService;
import kb.dsl.dto.DslModel;
import kb.dsl.exceptions.MappingException;

import kb.repository.KB;
import kb.repository.KBConsts;
import kb.utils.MyUtils;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
import restapi.util.SharedUtil;


/** A service that submits the abstract application deployment model to the Knowledge Base and
 * returns applicable optimizations according to the capabilities.
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/optimizations")
@Api()
public class OptimizationService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(OptimizationService.class.getName());
	static ConfigsLoader configInstance;
	static {
		configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
	}
	/**
	 * Storing the submitted AADM in the KB , assigning a unique id
	 * and returning applicable optimizations according to the capabilities
	 * @param aadmTTL The AADM in turtle format
	 * @param aadmURI A unique id
	 * @param aadmDSL The original aadm DSL
	 * @param complete
	 * @param namespace The namespace of the model e.g. docker
	 * @param name The file name of the model
	 * @param token token
	 * @throws RDFParseException A parse exception that can be thrown by a parser when it encounters an error
	 * @throws UnsupportedRDFormatException RuntimeException indicating that a specific RDF format is not supported.
	 * @throws IOException If your input format is invalid
	 * @throws MappingException Unknown entity issue
	 * @return The AADM URI along with potential optimizations
	 * @throws URISyntaxException 
	*/
	@POST
	//@Produces("text/plain")
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	@ApiOperation(
			value = "Returns the optimizations of a specific aadm")
	public Response getOptimizations(@ApiParam(value = "The TTL of AADM", required = true) @FormParam("aadmTTL") String aadmTTL,
			@ApiParam(value = "An id to uniquely identify a submission", required = false) @FormParam("aadmURI") String aadmURI,
			@ApiParam(value = "The aadm in DSL", required = false) @FormParam("aadmDSL") String aadmDSL,
			@ApiParam(value = "A flag to enable the auto-completion of missing elements in models", required = false) @DefaultValue("false") @FormParam("complete") boolean complete,
			@ApiParam(value = "namespace", required = false) @DefaultValue("") @FormParam("namespace") String namespace,
			@ApiParam(value = "name", required = false) @DefaultValue("") @FormParam("name") String name,
			@ApiParam(value = "version", required = false) @DefaultValue("") @FormParam("version") String version,
			@ApiParam(value = "token", required = false) @QueryParam("token") String token)
					throws RDFParseException, UnsupportedRDFormatException, IOException, MappingException, URISyntaxException  {
		
		if(AuthUtil.authentication()) {
			AuthResponse ares = SharedUtil.authorization(AuthUtil.createRoleFromNamespace(namespace, AuthConsts.AADM_W), token, true);
			if (ares.getResponse() != null)
				return ares.getResponse();
		}
		
		KB kb = new KB(configInstance.getGraphdb(), KB.REPOSITORY);
		
		DSLMappingService m = new DSLMappingService(kb, aadmTTL, aadmURI, complete, namespace, aadmDSL, name, version);
		DslModel aadmModel = null;

		//Contains the final response
		JSONObject response = new JSONObject();
		try {
			aadmModel = m.start();
			//String aadmid = MyUtils.getStringPattern(aadmUri.toString(), ".*/(AADM_.*).*");
			m.save();
			HttpClientRequest.getWarnings(response, aadmModel.getFullUri(), KBConsts.AADM);
			
			getOptimizations(response, aadmModel.getFullUri());
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
		} catch (MyRestTemplateException e) {
			if (aadmModel != null) {
				KBApi api = new KBApi(kb);
				api.deleteModel(aadmModel.getUri(), version, false);
			}
			
			HttpRequestErrorModel erm = e.error_model;
			LOG.error("rawStatus={}, api={}, statusCode={}, error={}", erm.rawStatus, erm.api, erm.statusCode, erm.error);
			
		 	return Response.status(erm.rawStatus).entity(erm.toJson().toString()).build();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		} finally {
			m.shutDown();
		}
		
		if (aadmModel != null) {
			response.put( "uri", aadmModel.getUri());
			response.put( "version", aadmModel.getVersion());
		}
		return Response.ok(Status.ACCEPTED).entity(response.toString()).build();
	}
	
	public void getOptimizations(JSONObject response, IRI aadmId) throws ClientProtocolException, IOException, ParseException, ValidationException {	
		KBApiOptimizations api = new KBApiOptimizations();
		Set<ValidationModel> optimizations = api.getOptimizationSuggestions(aadmId);
		api.shutDown();
		
		JSONObject _optimizations = new JSONObject();
		JSONArray array = new JSONArray();
		for (ValidationModel optimization : optimizations) {
			array.add(optimization.toJson());
		}
		if (!optimizations.isEmpty())
			response.put("templates_optimizations", array);

	}



}
