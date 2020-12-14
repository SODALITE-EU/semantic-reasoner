package httpclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import httpclient.exceptions.MyRestTemplateException;
import httpclient.exceptions.MyRestTemplateException.DownstreamApi;

public class BugCustomErrorHandler implements ResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
		// TODO Auto-generated method stub
		return (
		          httpResponse.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR
		          || httpResponse.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR);
	}

	@Override
	public void handleError(ClientHttpResponse httpResponse) throws IOException {
		if (httpResponse.getStatusCode().is4xxClientError() || httpResponse.getStatusCode().is5xxServerError()) {
		      try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getBody()))) {
		        String httpBodyResponse = reader.lines().collect(Collectors.joining(""));

		        // TODO deserialize (could be JSON, XML, whatever...) httpBodyResponse to a POJO that matches the error structure for that specific API, then extract the error message.
		        // Here the whole response will be treated as the error message, you probably don't want that.
		        String errorMessage = httpBodyResponse;

		        throw new MyRestTemplateException(DownstreamApi.BUG_PREDICTOR_API, httpResponse.getStatusCode(), errorMessage);
		      }
		    }
	}

}
