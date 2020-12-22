package httpclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.joda.time.LocalDateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.boot.web.client.RestTemplateBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import httpclient.dto.HttpRequestErrorModel;
import httpclient.dto.HttpRequestErrorModel.DownstreamApi;
import httpclient.exceptions.MyRestTemplateException;
import kb.configs.ConfigsLoader;
import kb.repository.KB;

public class HttpClientRequest {
	
	private static final Logger LOG = Logger.getLogger(HttpClientRequest.class.getName());
	
	static ConfigsLoader configInstance = ConfigsLoader.getInstance();
	static {
		configInstance.loadProperties();
		keycloak = configInstance.getKeycloak();
		keycloakClientId = configInstance.getKeycloakClientId();
		keycloakClientSecret = configInstance.getKeycloakClientSecret();
		
		bugPredictor = configInstance.getBugPredictorServer();
	}

	static String keycloak;
	static String keycloakClientId;
	static String keycloakClientSecret;
	
	static String bugPredictor;
	
			
	public static final String BUG_PREDICTOR_SERVICE = "bug-predictor-api/v0.1/bugs/tosca/jsonv2";
	public static final String INTROSPECT_SERVICE = "auth/realms/SODALITE/protocol/openid-connect/token/introspect";
	
	public static void setKeyCloak(String urlbase) {
		keycloak = urlbase;
	}
	
	public static void setBugPredictor(String urlbase) {
		bugPredictor = urlbase;
	}
	
	public static <T> T sendJSONMessage(URI uri, HttpMethod method, String content, Class<T> returnType, ResponseErrorHandler errorHandler) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
			
		RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
		
		RestTemplate restTemplate = restTemplateBuilder
				.errorHandler(errorHandler)
		        .build();

		
		HttpEntity<String> entity = new HttpEntity<>(content, headers);
		
		ResponseEntity<T> response = restTemplate.exchange(uri, method, entity, returnType);
		
		return response.getBody();
	}
	
	public static <T> T sendFormURLEncodedMessage(URI uri, HttpMethod method, MultiValueMap<String, Object> parts, Class<T> returnType, ResponseErrorHandler errorHandler,
			String username, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		if (!username.isEmpty() && !password.isEmpty()) {
			LOG.info("auth basic");
			headers.setBasicAuth(username, password);
		}
		
		HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(parts, headers);
		
		RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
		
		RestTemplate restTemplate = restTemplateBuilder
					.setReadTimeout(Duration.ofMillis((long)30 * 1000))
					.setConnectTimeout(Duration.ofMillis((long)10 * 1000))
					.errorHandler(errorHandler)
					.build();
		
		ResponseEntity<T> response = restTemplate.exchange(uri, method, entity, returnType);
		
		return response.getBody();
	}
	
	public static void validateToKen(String token) throws URISyntaxException, MyRestTemplateException {
		LOG.info("validateToKen:");
		String url = keycloak + INTROSPECT_SERVICE;
		
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

		map.add("token", token);
		
		String result = sendFormURLEncodedMessage(new URI(url), HttpMethod.POST, map, String.class, new KeycloakCustomErrorHandler(), keycloakClientId, keycloakClientSecret);
		
		JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
		
		boolean active = jsonObject.get("active").getAsBoolean();
		
		if(!active) {
			HttpRequestErrorModel e = new HttpRequestErrorModel(LocalDateTime.now(), DownstreamApi.KEYCLOAK_API, HttpStatus.UNAUTHORIZED, 401, "", "Access Token not active");
			throw new MyRestTemplateException(e);
		}
		
	}
	
	public static void getWarnings(JSONObject response, String aadmId) throws URISyntaxException, ParseException, MyRestTemplateException {
		LOG.info("getWarnings:");
		String url = bugPredictor + BUG_PREDICTOR_SERVICE;
		
		String input = "{\"server\": \"" + configInstance.getGraphdb() + "\","+ "\"repository\":\""+ KB.REPOSITORY + "\","+ "\"aadmid\":\""+ aadmId + "\"}";
		LOG.log(Level.INFO, "input = {0}", input);
		
		String result = null;
		result = sendJSONMessage(new URI(url), HttpMethod.POST, input, String.class, new BugCustomErrorHandler());
		LOG.log(Level.INFO, "result = {0}", result.toString());
		JSONParser parser = new JSONParser();
		JSONArray warningsJson = (JSONArray)((JSONObject) parser.parse(result)).get("warnings");
		if (!warningsJson.isEmpty())
			response.put("warnings",warningsJson);
	}
	
	
	public static void main(String[] args) throws ParseException, MyRestTemplateException, URISyntaxException  {			
		JSONObject response = new JSONObject();
		HttpClientRequest.getWarnings(response, "ddd");
			
		HttpClientRequest.validateToKen("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIwLV82aEVJRFdBajNvTnYtZE5Ud05VblJlcmt3ZmR3UExlUnM1ZTBsVDJrIn0.eyJleHAiOjE2MDgwNTU0NzYsImlhdCI6MTYwODA1NTE3NiwianRpIjoiYzlhNmNhNTEtZTg5NC00NTU5LTljNzEtMjM4NWM1YmNlZWJlIiwiaXNzIjoiaHR0cDovLzE5Mi4xNjguMi4xNzk6ODA4MC9hdXRoL3JlYWxtcy9TT0RBTElURSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJkZGYzODE1My1lNmQ3LTRjYzQtOTcxYi1hY2M2ZThkODE1M2EiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJzb2RhbGl0ZS1pZGUiLCJzZXNzaW9uX3N0YXRlIjoiMmJhM2EyYWYtZWZlNy00OGI5LTgxMjAtNmEzMzE4OTBkOTBiIiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsic29kYWxpdGUtaWRlIjp7InJvbGVzIjpbImFhZG1fciIsImNsaW5pY2FsX2FhZG1fciIsInNub3dfYWFkbV9yIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IlpvZSBWYXNpbGVpb3UiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ6b2UiLCJnaXZlbl9uYW1lIjoiWm9lIiwiZmFtaWx5X25hbWUiOiJWYXNpbGVpb3UiLCJlbWFpbCI6InpvZXZhczFAZ21haWwuY29tIn0.p8S89U9rOWdSpexCb4f-3FnptyNyfpfJHnJ3NSxw5J1H_1yr3yR1EJ8Xlp918X9Jq5pqHEdY-Ernw8fwWX90-J6BGuRchFULl_SZ-ofdhpnFAeqRNlD97N8-kuRygit3YrRoWnINJSf99UPaOVsZePzxRtFqd3AyvPc59bY6kPYe9vWYBAbZz79kZu_unjPpgzeNWD_1k46k5Jy2yILwDrEUC8UTeT4g0v2WRYeV0R3IF6qYtfv4F70wzxLCry0lUQIwKIfWEfz8BUeX7L7sC7Zu4sPSnQKlzT8spGfHfs9osCJPUSZKFV45X3Fg1qFJt8SOqufwXWALTi86zlMRHw");

	}

}
