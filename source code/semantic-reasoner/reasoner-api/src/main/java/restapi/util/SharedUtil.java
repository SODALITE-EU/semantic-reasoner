package restapi.util;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import httpclient.AuthConsts;
import httpclient.AuthUtil;
import httpclient.HttpClientRequest;
import httpclient.dto.AuthErrorModel;
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
	 * @param roles_input includes all the roles to be validated. e.g. [docker_rm_r, openstack_rm_r]
	 * @param roles_output The roles assigned to the user will be saved
	 * @param token token
	 * @param checkRole When enabled roles are checked. Some services such as NamespaceService do not need role validation
	 * @return Response if error occurs
	*/
	public static Response authorization(ArrayList<String> roles_input, ArrayList<String> roles_output, String token, boolean checkRole) throws URISyntaxException {
		try {	
			roles_output = HttpClientRequest.validateToKen(token);
			if (checkRole) {
				LOG.info("roles={}", roles_input);
								
				if (checkRole)
					AuthUtil.checkRoles(roles_input, roles_output);
			}
		} catch (MyRestTemplateException e) {
			HttpRequestErrorModel erm = e.error_model;
			LOG.warn("rawStatus={}, api={}, statusCode={}, error={}", erm.rawStatus, erm.api, erm.statusCode, erm.error);
		 	return Response.status(erm.rawStatus).entity(erm.toJson().toString()).build();
		} catch (AuthException e) {
			List<AuthErrorModel> roleErrorModels = e.roleModels;
			JSONArray array = new JSONArray();
			for (AuthErrorModel rm : roleErrorModels) {
				array.add(rm.toJson());
			}
			
			JSONObject errors = new JSONObject();
			errors.put("autherrors", array);
			LOG.warn("autherrors={}", errors.toString());
			return Response.status(Status.FORBIDDEN).entity(errors.toString()).build();
		}
		return null;
	}

	
	public static Response authForReadRoleFromResource(boolean template, String resource, String token) throws URISyntaxException {
		String typeOfRole = template ? AuthConsts.AADM_R : AuthConsts.RM_R;

		String _namespace =  MyUtils.getNamespaceFromReference(resource);

		String namespace = _namespace == null ? "global":_namespace;
		Response res = authorization(AuthUtil.createRoleFromNamespace(namespace, typeOfRole), null, token, true);
		return res;
	}
	
	public static Response authForWriteRoleFromNamespace(boolean template, ArrayList<String> roles_output, String namespace, String token) throws URISyntaxException {
		String typeOfRole = template ? AuthConsts.AADM_W : AuthConsts.RM_W;

		String _namespace = namespace == null ? "global":namespace;
		Response res = authorization(AuthUtil.createRoleFromNamespace(_namespace, typeOfRole), roles_output, token, true);
		return res;
	}
	
	public static Response authForReadRoleFromNamespace(boolean template, String namespace, String token) throws URISyntaxException {
		String typeOfRole = template ? AuthConsts.AADM_R : AuthConsts.RM_R;

		String _namespace = namespace == null ? "global":namespace;
		Response res = authorization(AuthUtil.createRoleFromNamespace(_namespace, typeOfRole), null, token, true);
		return res;
	}
	
	public static Response authForImports(List<String> imports, String typeOfRole, String token) throws URISyntaxException {
		Response res = authorization(AuthUtil.createRolesFromNamespaces(imports, typeOfRole), null, token, true);
		return res;
	}
	
	public static Response authWithoutRoles(String token) throws URISyntaxException {
		Response res = SharedUtil.authorization(null, null, token, false);
		return res;
	}
	
}
