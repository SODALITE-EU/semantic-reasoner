package httpclient.exceptions;

import java.util.List;

import httpclient.dto.AuthErrorModel;

public class AuthException extends Exception{
	private static final long serialVersionUID = 1L;

	public List<AuthErrorModel> roleModels;
	
	public AuthException(List<AuthErrorModel> roleModels) {
		this.roleModels = roleModels;
	}
}
