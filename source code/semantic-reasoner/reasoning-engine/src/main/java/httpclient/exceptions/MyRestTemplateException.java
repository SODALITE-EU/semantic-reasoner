package httpclient.exceptions;

import org.springframework.http.HttpStatus;

public class MyRestTemplateException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public enum DownstreamApi {
		  BUG_PREDICTOR_API,
		  KEYCLOAK_API
		}
	
	public HttpStatus statusCode;
	public String error;
	public DownstreamApi api;

	public MyRestTemplateException(DownstreamApi api, HttpStatus statusCode, String error) {
		super(error);
		this.api = api;
		this.statusCode = statusCode;
		this.error = error;
	  }
}
