package httpclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.joda.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import httpclient.dto.HttpRequestErrorModel;
import httpclient.dto.HttpRequestErrorModel.DownstreamApi;
import httpclient.exceptions.MyRestTemplateException;

public class KeycloakCustomErrorHandler implements ResponseErrorHandler {
	@Override
	public boolean hasError(ClientHttpResponse httpResponse) throws IOException {
		return (
		          httpResponse.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR
		          || httpResponse.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR);
	}

	@Override
	public void handleError(ClientHttpResponse httpResponse) throws IOException {
		if (httpResponse.getStatusCode().is4xxClientError() || httpResponse.getStatusCode().is5xxServerError()) {
		      try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getBody()))) {
		        String httpBodyResponse = reader.lines().collect(Collectors.joining(""));
		        String errorMessage = httpBodyResponse;

		        HttpRequestErrorModel e = new HttpRequestErrorModel(LocalDateTime.now(), DownstreamApi.KEYCLOAK_API, httpResponse.getStatusCode(), httpResponse.getRawStatusCode(), errorMessage, "Error in keycloak request");
		        throw new MyRestTemplateException(e);
		      }
		    }
	}

}
