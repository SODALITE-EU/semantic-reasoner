package httpclient.exceptions;


import httpclient.dto.HttpRequestErrorModel;

public class MyRestTemplateException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;

	public HttpRequestErrorModel error_model;

	public MyRestTemplateException(HttpRequestErrorModel error_model) {
		super(error_model.error);
		this.error_model = error_model;
	}
}
