package httpclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.boot.web.client.RestTemplateBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import httpclient.exceptions.MyRestTemplateException;
import kb.configs.ConfigsLoader;
import kb.repository.KB;

public final class HttpClientRequest {
	
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
	
	private static RestTemplate restTemplate;
			
	public static String BUG_PREDICTOR_SERVICE = "bug-predictor-api/v0.1/bugs/tosca/jsonv2";
	public static String TOKEN_SERVICE = "auth/realms/SODALITE/protocol/openid-connect/token";
	public static String INTROSPECT_SERVICE = "auth/realms/SODALITE/protocol/openid-connect/token/introspect";
	

	public static <T> T sendJSONMessage(URI uri, HttpMethod method, String content, Class<T> returnType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
			
		if (restTemplate != null) {
			RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
		
			restTemplate = restTemplateBuilder
				.errorHandler(new BugCustomErrorHandler())
		        .build();
		}
		
		HttpEntity<String> entity = new HttpEntity<>(content, headers);
		
		ResponseEntity<T> response = restTemplate.exchange(uri, method, entity, returnType);
		
		return response.getBody();
	}
	
	public static <T> T sendFormURLEncodedMessage(URI uri, HttpMethod method, MultiValueMap<String, Object> parts, Class<T> returnType, 
			String username, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		if (!username.isEmpty() && !password.isEmpty()) {
			System.out.println("auth basic");
			headers.setBasicAuth(username, password);
		}
		
		HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(parts, headers);
		
		if (restTemplate == null) {
			RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
		
			restTemplate = restTemplateBuilder
					.setReadTimeout(Duration.ofMillis(30 * 1000))
					.setConnectTimeout(Duration.ofMillis(10 * 1000))
					.errorHandler(new KeycloakCustomErrorHandler())
					.build();
		}
		ResponseEntity<T> response = restTemplate.exchange(uri, method, entity, returnType);
		
		return response.getBody();
	}
	
	public static void getKeycloakToKen() throws URISyntaxException {
		
		String url = keycloak + TOKEN_SERVICE;
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

		map.add("grant_type", "password");
		map.add("username", "zoe");
		map.add("password", "qwerty");
		map.add("client_id", keycloakClientId);
		map.add("client_secret", keycloakClientSecret);
		try {
			String result = sendFormURLEncodedMessage(new URI(url), HttpMethod.POST, map, String.class, "", "");
			JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
			System.out.println(jsonObject.get("access_token").getAsString());
		} catch (MyRestTemplateException e) {
			System.out.println(String.format("api=%s, statusCode=%s, error=%s",e.api, e.statusCode, e.error));
			e.printStackTrace();
		}
	}
	
	public static void validateToKen(String token) throws URISyntaxException {
		System.out.println("validateToKen:");
		String url = keycloak + INTROSPECT_SERVICE;
		
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

		map.add("token", token);
		
		try {
			String result = sendFormURLEncodedMessage(new URI(url), HttpMethod.POST, map, String.class, keycloakClientId, keycloakClientSecret);
			JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
			System.out.println(jsonObject.get("active").getAsBoolean());
			
			//create a class with active, and roles fields and return this
		} catch (MyRestTemplateException e) {
			System.out.println(String.format("api=%s, statusCode=%s, error=%s",e.api, e.statusCode, e.error));
			e.printStackTrace();
		}
		
	}
	
	public static boolean getWarnings(JSONObject response, String aadmId) throws URISyntaxException, ParseException {
		System.out.println("getWarnings:");
		String url = bugPredictor + BUG_PREDICTOR_SERVICE;
		
		String input = "{\"server\": \"" + configInstance.getGraphdb() + "\","+ "\"repository\":\""+ KB.REPOSITORY + "\","+ "\"aadmid\":\""+ aadmId + "\"}";
		System.out.println("input = " + input);
		
		String result = null;
		try {
			result = sendJSONMessage(new URI(url), HttpMethod.POST, input, String.class);
			System.out.println(result.toString());
			JSONParser parser = new JSONParser();
			JSONArray warningsJson = (JSONArray)((JSONObject) parser.parse(result)).get("warnings");
			if (!warningsJson.isEmpty())
				response.put("warnings",warningsJson);
			return true;
		} catch (MyRestTemplateException e) {
			System.out.println(String.format("api=%s, statusCode=%s, error=%s",e.api, e.statusCode, e.error));
			return false;
		}
	}
	
	
	public static void main(String[] args) throws ParseException  {
		try {
			//new HttpClientRequest().getKeycloakToKen();
			JSONObject response = new JSONObject();
			//HttpClientRequest.getWarnings(response, "ddd");
			HttpClientRequest.validateToKen("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIwLV82aEVJRFdBajNvTnYtZE5Ud05VblJlcmt3ZmR3UExlUnM1ZTBsVDJrIn0.eyJleHAiOjE2MDc5MzgxMzgsImlhdCI6MTYwNzkzNzgzOCwianRpIjoiMWNiNTA2NDItOTJkZC00NWNiLWJlZTAtMDllYjE0OGI5NDNiIiwiaXNzIjoiaHR0cDovLzE5Mi4xNjguMi4xNzk6ODA4MC9hdXRoL3JlYWxtcy9TT0RBTElURSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJkZGYzODE1My1lNmQ3LTRjYzQtOTcxYi1hY2M2ZThkODE1M2EiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJzb2RhbGl0ZS1pZGUiLCJzZXNzaW9uX3N0YXRlIjoiN2NhNWYyM2MtNWEzMi00Y2ViLTkxY2YtNmYyODYyZTViMjk4IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsic29kYWxpdGUtaWRlIjp7InJvbGVzIjpbImFhZG1fciIsImNsaW5pY2FsX2FhZG1fciIsInNub3dfYWFkbV9yIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IlpvZSBWYXNpbGVpb3UiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ6b2UiLCJnaXZlbl9uYW1lIjoiWm9lIiwiZmFtaWx5X25hbWUiOiJWYXNpbGVpb3UiLCJlbWFpbCI6InpvZXZhczFAZ21haWwuY29tIn0.me-MHY7X-JKWX7siWitZK5H9R6tIXdP3JRj7EQtd24-G_dG7svFQn6-IA1n41UalutSguf12dVdBPWuR6aaMjeYgmSLa7au4Be72ar_qLEKMd-QyeaiBTSlT2MbmF5mb-xvik0iKWEskkzCI_DNS82MNyFG9EWlCI5-xtR9zfcZWYv7OLzTZ_qZfCNVHlJAas3_HTzjLuWT3cjEu9MJINdMcNQYE4kW957p6HYo-TQ0YlAN8TXnVlAgPts51oSONQ0GbGmyNIMtroRrIdCW7fWXynwH6oi91chGHpOCzOU74aHDsXoNd__YakEKZGfVSQAmm6JcNBrsEA_1xR37GeQ");
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
