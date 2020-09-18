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

import kb.repository.KB;
import kb.utils.ConfigsLoader;

public final class HttpClientRequest {
	
	public static String BUG_PREDICTOR_SERVICE = "bug-predictor-api/v0.1/bugs/tosca/jsonv2";

	
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
	public static boolean getWarnings(JSONObject response, String submissionId) throws ClientProtocolException, IOException, ParseException {
		String warnings = bugPredictorApi(submissionId);
		if (warnings.equals("Unreachable"))
			return false;
				
		JSONParser parser = new JSONParser();
		JSONArray warningsJson = (JSONArray)((JSONObject) parser.parse(warnings)).get("warnings");
		if (!warningsJson.isEmpty())
			response.put("warnings",warningsJson);
		return true;
	}

	public static String bugPredictorApi(String aadmId) throws ClientProtocolException, IOException {
		ConfigsLoader configLoader = ConfigsLoader.getInstance();
		String bugPredictorEndpoint = configLoader.getBugPredictorServer() + BUG_PREDICTOR_SERVICE;

		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(bugPredictorEndpoint);

		httpPost.setHeader("Accept", "application/json");
		httpPost.setHeader("Content-type", "application/json");

		String jsonInputString = "{\"server\": \"" + configLoader.getGraphdb() + "\","+ "\"repository\":\""+ KB.REPOSITORY + "\","+ "\"aadmid\":\""+ aadmId + "\"}";
		System.out.println("jsonInputString = " + jsonInputString);
		
		StringEntity stringEntity = new StringEntity(jsonInputString);
		httpPost.setEntity(stringEntity);
		System.out.println("Executing request " + httpPost.getRequestLine());
		
		BufferedReader br = null;
		try {		 
			HttpResponse response = httpclient.execute(httpPost);
			br = new BufferedReader(
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
			e.printStackTrace();
			System.err.println("Bug Predictor service at " + bugPredictorEndpoint + "returned an HttpHostConnectException" );
			return "Unreachable";
		} finally {
			if (br != null)
				br.close();
		}		 

	}
}
