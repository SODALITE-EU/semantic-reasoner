package httpclient.dto;

import org.json.simple.JSONObject;

public abstract class AuthModel {
	public enum DownstreamApi {
		  BUG_PREDICTOR_API,
		  KEYCLOAK_API
	}
	
	public abstract JSONObject toJson();

}
