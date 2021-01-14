package httpclient.dto;

import org.joda.time.LocalDateTime;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;


public class AuthErrorModel extends AuthModel {

	private LocalDateTime timestamp;
	private String description;
	private HttpStatus statusCode;
	private int rawStatus;
	
	public AuthErrorModel(LocalDateTime timestamp, String description, HttpStatus statusCode, int rawStatus) {
		this.timestamp = timestamp;
		this.description = description;
		this.statusCode = statusCode;
		this.rawStatus = rawStatus;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}
	
	public int getRawStatus() {
		return this.rawStatus;
	}
	
	@Override
	public JSONObject toJson() {
		JSONObject errorObj = new JSONObject();
		
		errorObj.put("timestamp", timestamp.toString());
		errorObj.put("api", DownstreamApi.KEYCLOAK_API);
		errorObj.put("statusCode", statusCode);
		errorObj.put("description", description);
		
		return errorObj;
	}

}
