package restapi.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import httpclient.AuthConsts;
import httpclient.AuthUtil;
import httpclient.HttpClientRequest;
import httpclient.dto.AuthErrorModel;
import httpclient.dto.HttpRequestErrorModel;
import httpclient.exceptions.AuthException;
import httpclient.exceptions.MyRestTemplateException;

public class SharedUtil {
	private static final Logger LOG = Logger.getLogger(SharedUtil.class.getName());
	private SharedUtil() {
		throw new IllegalStateException("SharedUtil class");
	}
	
	/**
	 * Authorized the iser
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
				LOG.log(Level.INFO, "roles={0}", roles_input);
								
				if (checkRole)
					AuthUtil.checkRoles(roles_input, roles_output);
			}
		} catch (MyRestTemplateException e) {
			HttpRequestErrorModel erm = e.error_model;
			LOG.log(Level.WARNING, "rawStatus={0}, api={1}, statusCode={2}, error={3}", new Object[] {erm.rawStatus, erm.api, erm.statusCode, erm.error});
		 	return Response.status(erm.rawStatus).entity(erm.toJson().toString()).build();
		} catch (AuthException e) {
			List<AuthErrorModel> roleErrorModels = e.roleModels;
			JSONArray array = new JSONArray();
			for (AuthErrorModel rm : roleErrorModels) {
				array.add(rm.toJson());
			}
			
			JSONObject errors = new JSONObject();
			errors.put("autherrors", array);
			LOG.log(Level.WARNING, "autherrors={0}", errors.toString());
			return Response.status(Status.FORBIDDEN).entity(errors.toString()).build();
		}
		return null;
	}


}
