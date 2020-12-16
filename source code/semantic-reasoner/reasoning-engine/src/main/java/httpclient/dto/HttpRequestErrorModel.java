package httpclient.dto;

import org.joda.time.LocalDateTime;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;

public class HttpRequestErrorModel {
	public enum DownstreamApi {
		  BUG_PREDICTOR_API,
		  KEYCLOAK_API
		}
	
	public LocalDateTime timestamp;
	public DownstreamApi api;
	public HttpStatus statusCode;
	public int rawStatus;
	public String error;
	public String description;
	
	public HttpRequestErrorModel(LocalDateTime timestamp,  DownstreamApi api, HttpStatus statusCode, int rawStatus, String error, String description) {
		this.timestamp = timestamp;
		this.api= api;
		this.statusCode = statusCode;
		this.rawStatus = rawStatus;
		this.error = error;
		this.description = description;
	}
	
	public JSONObject toJson() {		
		JSONObject error = new JSONObject();
		
		error.put("timestamp", timestamp.toString());
		error.put("api", api.toString());
		error.put("statusCode", statusCode.toString());
		error.put("description", description);
		
		return error;
	}
}
