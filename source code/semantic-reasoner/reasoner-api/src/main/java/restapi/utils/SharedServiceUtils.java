package restapi.utils;

import java.net.URISyntaxException;
import java.util.logging.Logger;

import httpclient.HttpClientRequest;
import httpclient.dto.HttpRequestErrorModel;
import httpclient.exceptions.MyRestTemplateException;

public class SharedServiceUtils {
	
	private static final Logger LOG = Logger.getLogger(SharedServiceUtils.class.getName());
	
	private SharedServiceUtils() {
		
	}
	
	public static HttpRequestErrorModel validateToKen(String toKen) throws URISyntaxException {
		HttpRequestErrorModel erm = null;
		try {	
			HttpClientRequest.validateToKen(toKen);
		} catch (MyRestTemplateException e) {
			erm = e.error_model;
			LOG.warning(String.format("rawStatus=%s, api=%s, statusCode=%s, error=%s",erm.rawStatus, erm.api, erm.statusCode, erm.error));
		}
		
		return erm;
	}

}
