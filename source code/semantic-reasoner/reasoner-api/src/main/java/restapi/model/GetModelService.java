package restapi.model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import httpclient.AuthUtil;
import httpclient.dto.AuthResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import kb.KBApi;
import kb.dto.Capability;
import kb.dto.SodaliteAbstractModel;
import kb.utils.MyUtils;
import restapi.AbstractService;
import restapi.util.SharedUtil;

/** A service that returns 
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/model")
@Api()
public class GetModelService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(GetModelService.class.getName());
	/**
	  * Getting all the AADMs or RMs in KB 
	  * @param resource  (e.g. node type name or template name) could be provided
	  * @param namespace If empty global namespace is searched, otherwise the provided one
	  * @param uri The uri of the model is provided
	  * @param version The version is optionally provided. If version is not provided, and uri is provided, all the versions of a specific model are returned.
	  * If version = "" and uri is provided then the untagged model is returned. And if version is provided, the specific versioned model is returned.
	  * @throws IOException If your input format is invalid
	  * @return The model metadata
	 * @throws URISyntaxException 
	 */
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the model saved in Knowledge Base",
//			response = String.class,
			responseContainer = "List")
	public Response getModel(@ApiParam(value = "resource name", required = false) @DefaultValue("") @QueryParam("resource") String resource,
			@ApiParam(value = "empty or namespace value", required = false) @DefaultValue("") @QueryParam("namespace") String namespace,
			@ApiParam(value = "uri", required = false)@DefaultValue("")  @QueryParam("uri") String uri,
			@ApiParam(value = "version", required = false)  @QueryParam("version") String version,
			@ApiParam(value = "token", required = false) @QueryParam("token") String token)
			throws IOException, URISyntaxException {
		LOG.info( "getModel: resource= {}, namespace = {}, uri = {}, version = {}",  resource, namespace, uri, version);
		
		Set<SodaliteAbstractModel> models = new HashSet<SodaliteAbstractModel> ();
		AuthResponse ares = new AuthResponse();
				
		KBApi api = new KBApi();
		if (!"".equals(resource)) {
			models = api.getModelForResource(resource, namespace, version);
			
			if(AuthUtil.authentication() && !"".equals(namespace)) {
				Iterator<SodaliteAbstractModel> iter = models.iterator();
				if (!models.isEmpty()) {
					SodaliteAbstractModel model = iter.next();
					String shortNamespace = MyUtils.getNamespaceFromContext(namespace);
					ares = SharedUtil.authForReadRoleFromNamespace(model.getIsAADM(), shortNamespace, token);
					LOG.info( "Model for Resource shortNamespace = {}",  shortNamespace);
				}
			}
		} else if (!"".equals(uri)) {
			models = api.getModelFromURI(uri, version);
			Iterator<SodaliteAbstractModel> iter = models.iterator();

			if(AuthUtil.authentication() && !models.isEmpty()) {
				SodaliteAbstractModel firstModel = iter.next();
				if (firstModel.getNamespace() != null) {
					String shortNamespace = MyUtils.getNamespaceFromContext(firstModel.getNamespace());
					ares = SharedUtil.authForReadRoleFromNamespace(firstModel.getIsAADM(), shortNamespace, token);
					LOG.info( "Model from URI shortNamespace = {}",  shortNamespace);
				}
			}
		} else {
			return Response.status(Status.BAD_REQUEST).entity("Resource or uri should be provided").build();
		}
		
		//Error returned in auth
		if (AuthUtil.authentication() &&ares.getResponse() != null)
					return ares.getResponse();
		
		
		api.shutDown();
		JsonObject _model = new JsonObject();
		JsonArray array = new JsonArray();
		
		for (SodaliteAbstractModel m : models) {
			array.add(m.serialise());
		}
			
		_model.add("data", array);

		return Response.ok(_model.toString()).build();
	}
}
