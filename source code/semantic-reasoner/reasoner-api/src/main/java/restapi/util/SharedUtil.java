package restapi.util;

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
	
	public static Response authorization(String namespace, ArrayList<String> roles, String toKen, String typeOfRole) throws URISyntaxException {
		try {	
			roles = HttpClientRequest.validateToKen(toKen);
			LOG.log(Level.INFO, "roles={0}", roles);
			String role = null;
			switch (typeOfRole) {
				case AuthConsts.AADM_W:
					role = namespace.isEmpty() ? AuthConsts.GLOBAL_AADM_W : namespace + AuthConsts.AADM_W;
					break;
				case AuthConsts.AADM_R:
					role = namespace.isEmpty() ? AuthConsts.GLOBAL_AADM_R : namespace + AuthConsts.AADM_R;
					break;
				case AuthConsts.RM_W:
					role = namespace.isEmpty() ? AuthConsts.GLOBAL_RM_W : namespace + AuthConsts.RM_W;
					break;
				case AuthConsts.RM_R:
					role = namespace.isEmpty() ? AuthConsts.GLOBAL_RM_R : namespace + AuthConsts.RM_R;
					break;
				default:
					LOG.log(Level.INFO, "default");
					return null;
			}
			if (role != null)
				AuthUtil.checkRole(role, roles);
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
