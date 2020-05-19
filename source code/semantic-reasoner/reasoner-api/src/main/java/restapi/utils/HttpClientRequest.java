package restapi.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class HttpClientRequest {
	
	public static String bugPredictorServer;
	public static String repositoryServer;
	
	public static String BUG_PREDICTOR_SERVER = "http://localhost:8084/";
	public static String BUG_PREDICTOR_SERVICE = "bug-predictor-api/v0.1/bugs/tosca/jsonv2";

	public static String REPOSITORY_SERVER_URL = "http://localhost:7200";
	public static String REPOSITORY = "TOSCA";
	
	
	static {
		String getenv = System.getenv("bugPredictorServer");
		if (getenv != null)
			bugPredictorServer = getenv;
		else
			bugPredictorServer = BUG_PREDICTOR_SERVER;
		
		getenv = System.getenv("graphdb");
		if (getenv != null)
			repositoryServer = getenv;
		else
			repositoryServer = REPOSITORY_SERVER_URL;
	}
	
	private HttpClientRequest() { 
		throw new UnsupportedOperationException();
		
    }
	
	/**
	 * Calling the bug predictor for getting the potential warnings of the model.
	 * @param response The response is the parameter in which the warnings are saved
	 * @param submissionId
	 * @throws IOException If your input format is invalid
	 * @throws ClientProtocolException Signals an error in the HTTP protocol.
	 * @throws ParseException Signals that an error has been reached unexpectedly while parsing
	 */
	public static void getWarnings(JSONObject response, String submissionId) throws ClientProtocolException, IOException, ParseException {
		String warnings = bugPredictorApi(submissionId);
		JSONParser parser = new JSONParser();
		JSONArray warningsJson = (JSONArray)((JSONObject) parser.parse(warnings)).get("warnings");
		if (!warningsJson.isEmpty())
			response.put("warnings",warningsJson);
	}

	public static String bugPredictorApi(String aadmId) throws ClientProtocolException, IOException {
		String bugPredictorEndpoint = bugPredictorServer + BUG_PREDICTOR_SERVICE;

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(bugPredictorEndpoint);

		httpPost.setHeader("Accept", "application/json");
		httpPost.setHeader("Content-type", "application/json");

		String jsonInputString = "{\"server\": \"" + repositoryServer + "\","+ "\"repository\":\""+ REPOSITORY + "\","+ "\"aadmid\":\""+ aadmId + "\"}";
		
		StringEntity stringEntity = new StringEntity(jsonInputString);
		httpPost.setEntity(stringEntity);
		System.out.println("Executing request " + httpPost.getRequestLine());
		try {		 
			HttpResponse response = httpclient.execute(httpPost);
			BufferedReader br = new BufferedReader(
					new InputStreamReader((response.getEntity().getContent())));
						 
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Failed: HTTP error code : "
							+ response.getStatusLine().getStatusCode());
			}

			StringBuffer responseResult = new StringBuffer();
			String line = "";
			while ((line = br.readLine()) != null) {
				System.out.println("Response: \n" + responseResult.append(line));
			}
			return responseResult.toString();
		} catch(HttpHostConnectException e) {
			System.err.println("Bug Predictor service unreachable at " + bugPredictorEndpoint);
			return "";
		}		 

	}
}
