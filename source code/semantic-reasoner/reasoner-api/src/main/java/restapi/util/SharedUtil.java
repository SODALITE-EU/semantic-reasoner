package restapi.util;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.rdf4j.model.Resource;

import httpclient.AuthConsts;
import httpclient.AuthUtil;
import httpclient.HttpClientRequest;
import httpclient.dto.AuthErrorModel;
import httpclient.dto.AuthResponse;
import httpclient.dto.HttpRequestErrorModel;
import httpclient.exceptions.AuthException;
import httpclient.exceptions.MyRestTemplateException;

import kb.utils.MyUtils;

public class SharedUtil {
	private static final Logger LOG = LoggerFactory.getLogger(SharedUtil.class.getName());
	public static final boolean IS_AADM = true;
	public static final String AADM = "AADM";
	private SharedUtil() {
		throw new IllegalStateException("SharedUtil class");
	}
	
	/**
	 * Authorized the user
	 * @param rolesInput includes all the roles to be validated. e.g. [docker_rm_r, openstack_rm_r]
	 * @param token token
	 * @param checkRole When enabled roles are checked. Some services such as NamespaceService do not need role validation
	 * @return Response if error occurs
	*/
	public static AuthResponse authorization(List<String> rolesInput, String token, boolean checkRole) throws URISyntaxException {
		AuthResponse amodel = new AuthResponse();
		try {	
			List<String> rolesOutput = HttpClientRequest.validateToKen(token);
			amodel.setRoles(rolesOutput);
			if (checkRole && rolesInput!=null) {
				LOG.info("rolesInput={}", rolesInput);
				AuthUtil.checkRoles(rolesInput, rolesOutput);
			}
		} catch (MyRestTemplateException e) {
			HttpRequestErrorModel erm = e.error_model;
			LOG.warn("rawStatus={}, api={}, statusCode={}, error={}", erm.rawStatus, erm.api, erm.statusCode, erm.error);
		 	amodel.setResponse(Response.status(erm.rawStatus).entity(erm.toJson().toString()).build());
		} catch (AuthException e) {
			List<AuthErrorModel> roleErrorModels = e.roleModels;
			JSONArray array = new JSONArray();
			//Probably no for is needed, but add all missing roles in one description
			//If no array is needed, then change array in AuthException to AuthErrorModel
			//for (AuthErrorModel rm : roleErrorModels) {
				array.add(roleErrorModels.get(0).toJson());
			//}
			
			JSONObject errors = new JSONObject();
			errors.put("autherrors", array);
			LOG.warn("autherrors={}", errors.toString());
			amodel.setResponse(Response.status(roleErrorModels.get(0).getRawStatus()).entity(errors.toString()).build());
		}
		return amodel;
	}

	
	public static AuthResponse authForReadRoleFromResource(boolean template, String resource, String token) throws URISyntaxException {
		String typeOfRole = template ? AuthConsts.AADM_R : AuthConsts.RM_R;

		String namespaceInput =  MyUtils.getNamespaceFromReference(resource);

		List<String> roles = null;
		if(namespaceInput != null)
			roles = AuthUtil.createRoleFromNamespace(namespaceInput, typeOfRole);
		return authorization(roles, token, true);
	}
	
	public static AuthResponse authForWriteRoleFromNamespace(boolean template, String namespace, String token) throws URISyntaxException {
		String typeOfRole = template ? AuthConsts.AADM_W : AuthConsts.RM_W;

		String shortNamespace = namespace == null ? AuthConsts.GLOBAL:namespace;
		return authorization(AuthUtil.createRoleFromNamespace(shortNamespace, typeOfRole), token, true);
	}
	
	//for save TOSCA service
	public static AuthResponse authForWriteRolesFromNamespaces(String rmNamespace, String aadmNamespace, String token) throws URISyntaxException {
		List<String> roles = new ArrayList<>();
		
		String localRmNamespace = rmNamespace == null ? AuthConsts.GLOBAL:rmNamespace;
		String localAadmNamespace = aadmNamespace == null ? AuthConsts.GLOBAL:aadmNamespace;
		
		roles.addAll(AuthUtil.createRoleFromNamespace(localRmNamespace, AuthConsts.RM_W));
		roles.addAll(AuthUtil.createRoleFromNamespace(localAadmNamespace, AuthConsts.AADM_W));
	
		return authorization(roles, token, true);
	}
	
	public static AuthResponse authForReadRoleFromNamespace(boolean template, String namespace, String token) throws URISyntaxException {
		String typeOfRole = template ? AuthConsts.AADM_R : AuthConsts.RM_R;
		
		List<String> roles = null;
		if(!"".equals(namespace))
			roles = AuthUtil.createRoleFromNamespace(namespace, typeOfRole);
		return authorization(roles, token, true);
	}
	
	public static AuthResponse authForImports(List<String> imports, String typeOfRole, String token) throws URISyntaxException {
		return authorization(AuthUtil.createRolesFromNamespaces(imports, typeOfRole), token, true);
	}
	
	public static AuthResponse authWithoutRoles(String token) throws URISyntaxException {
		return SharedUtil.authorization(null, token, false);
	}
	
	public static AuthResponse authForResources(List<String> resources, String typeOfRole, String token) throws URISyntaxException {
		return authorization(AuthUtil.createRolesFromResources(resources, typeOfRole), token, true);
	}
	
	public static List<Resource> authorizedContexts(List<Resource> contexts, List<String> roles) {
		List<Resource> authorizedContexts = new ArrayList<>();
		for(Resource ctx : contexts) {
			String ctxStr = ctx.toString();
			String namespace = MyUtils.getNamespaceFromContext(ctxStr);
			if (roles.contains(namespace + AuthConsts.AADM_R) || roles.contains(namespace + AuthConsts.RM_R))
				authorizedContexts.add(ctx);
		}
		return authorizedContexts;
	}
	
	
	
	
}
