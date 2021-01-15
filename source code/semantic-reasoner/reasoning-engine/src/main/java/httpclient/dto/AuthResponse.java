package httpclient.dto;

import java.util.List;

import javax.ws.rs.core.Response;

public class AuthResponse {
	Response res;
	List<String> roles;
	
	public AuthResponse() {
		
	}
	
	public AuthResponse(Response res, List<String> roles) {
		this.res = res;
		this.roles = roles;
	}

	public Response getResponse() {
		return this.res;
	}
	
	public List<String> getRoles() {
		return this.roles;
	}
	
	public void setResponse(Response res) {
		this.res = res;
	}
	
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
}
