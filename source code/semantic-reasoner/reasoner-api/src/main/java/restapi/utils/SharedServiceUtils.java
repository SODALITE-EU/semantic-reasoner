package restapi.utils;

import java.net.URISyntaxException;
import java.util.logging.Level;
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
			LOG.log(Level.WARNING, "rawStatus={0}, api={1}, statusCode={2}, error={3}", new Object[] {erm.rawStatus, erm.api, erm.statusCode, erm.error});
		}
		
		return erm;
	}

}
