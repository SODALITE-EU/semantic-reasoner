package restapi.utils;

import java.net.URISyntaxException;

import javax.ws.rs.core.Response;

import httpclient.HttpClientRequest;
import httpclient.dto.HttpRequestErrorModel;
import httpclient.exceptions.MyRestTemplateException;

public class SharedServiceUtil {
	
	public static HttpRequestErrorModel validateToKen(String toKen) throws URISyntaxException {
		HttpRequestErrorModel erm = null;
		try {	
			HttpClientRequest.validateToKen(toKen);
		} catch (MyRestTemplateException e) {
			erm = e.error_model;
			System.out.println(String.format("rawStatus=%s, api=%s, statusCode=%s, error=%s",erm.rawStatus, erm.api, erm.statusCode, erm.error));
		}
		
		return erm;
	}

}
