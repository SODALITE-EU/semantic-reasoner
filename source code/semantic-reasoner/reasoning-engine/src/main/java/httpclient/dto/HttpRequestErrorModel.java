package httpclient.dto;

import org.joda.time.LocalDateTime;
import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;

public class HttpRequestErrorModel extends AuthModel {
	
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
		JSONObject errorObj = new JSONObject();
		
		errorObj.put("timestamp", timestamp.toString());
		errorObj.put("api", api.toString());
		errorObj.put("statusCode", statusCode.toString());
		errorObj.put("description", description);
		
		return errorObj;
	}
}
