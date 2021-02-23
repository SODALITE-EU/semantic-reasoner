package restapi.tosca.mapper;

import java.io.IOException;
import java.net.URISyntaxException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import httpclient.exceptions.MyRestTemplateException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.configs.ConfigsLoader;
import kb.dsl.DSLMappingService;
import kb.dsl.DSLRMMappingService;
import kb.dsl.exceptions.MappingException;
import kb.dsl.exceptions.models.DslValidationModel;
import kb.repository.KB;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
import restapi.AbstractService;
import tosca.mapper.TOSCAMappingService;


/** A service that submits the abstract application deployment model and Resource model from TOSCA format to the Knowledge Base.
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/saveTOSCA")
@Api()
public class SubmitTOSCAService extends AbstractService {

	private static final Logger LOG = LoggerFactory.getLogger(SubmitTOSCAService.class.getName());
	static ConfigsLoader configInstance;
	static {
		configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
	}
	/**
	 * Storing the submitted resource model and aadm to the Knowledge Base
	 * @param modelTTL Both resource model and aadm in tosca format
	 * @param aadmURI A unique id
	 * @param rmName The file name of the resource model
	 * @param aadmName The file name of the aadm
	 * @param namespace The namespace of the model
	 * @param token The token
	 * @throws RDFParseException A parse exception that can be thrown by a parser when it encounters an error
	 * @throws UnsupportedRDFormatException   RuntimeException indicating that a specific RDF format is not supported.
	 * @throws IOException If your input format is invalid
	 * @throws MappingException Unknown entity issue
	 * @throws MyRestTemplateException A problem while connecting to other component
	 * @throws URISyntaxException 
	 * @return The AADM and RM uri
	*/
	@POST
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	@ApiOperation(value = "Stores submitted node templates in the KB")
	public Response saveTOSCA(@ApiParam(value = "The TTL of model", required = true) @FormParam("modelTTL") String modelTTL,
			@ApiParam(value = "An id to uniquely identify a submission", required = true) @DefaultValue("") @FormParam("aadmURI") String aadmURI,
			@ApiParam(value = "An id to uniquely identify a submission", required = true) @DefaultValue("") @FormParam("rmURI") String rmURI,
			@ApiParam(value = "name", required = false) @DefaultValue("") @FormParam("rmName") String rmName,
			@ApiParam(value = "name", required = false) @DefaultValue("") @FormParam("aadmName") String aadmName,
			@ApiParam(value = "namespace", required = false) @DefaultValue("") @FormParam("namespace") String namespace,
			@ApiParam(value = "token") @FormParam("token") String token)
			throws RDFParseException, UnsupportedRDFormatException, IOException, MappingException, MyRestTemplateException, URISyntaxException {
		
		JSONObject response = new JSONObject();
		LOG.info("modelTTL = {}, namespace = {}", modelTTL, namespace);
		KB kb = new KB(configInstance.getGraphdb(), "TOSCA");
		
		TOSCAMappingService toscaMapper = new TOSCAMappingService(modelTTL);
		toscaMapper.parse();
		String rmTTL = toscaMapper.getExchangeRM();
		String aadmTTL = toscaMapper.getExchangeAADM();
		
		if (aadmTTL == null && rmTTL == null)
			return Response.status(Status.BAD_REQUEST).entity("rm and aadm are empty").build();
		
		DSLRMMappingService rm = new DSLRMMappingService(kb, rmTTL, rmURI, namespace, "", rmName);
		DSLMappingService aadm = new DSLMappingService(kb, aadmTTL, aadmURI, false, namespace, "", aadmName);

		IRI rmUri = null;
		IRI aadmUri = null;
		try {
			rmUri = rm.start();
			rm.save();
			aadmUri = aadm.start();
			aadm.save();			
		}  catch (MappingException e) {
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
		}  catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("There was an internal server error").build();
		} finally {
			rm.shutDown();
		}
		
		
		
		if (rmUri != null)
			response.put("rmuri", rmUri.stringValue());
		if (aadmUri != null)
			response.put("aadmuri", aadmUri.stringValue());
		
		return Response.ok(Status.ACCEPTED).entity(response.toString()).build();
	}
	
}