package kb.dsl;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import httpclient.HttpClientRequest;
import httpclient.dto.HttpRequestErrorModel;
import httpclient.exceptions.MyRestTemplateException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;

public class HttpClientRequestTest {
	
	  WireMockServer wireMockServer = new WireMockServer();
	  
	
	  @BeforeEach
	  public void setup () {
		  wireMockServer = new WireMockServer(8080);
		  wireMockServer.start();
		  setupStub();
	  }
	  
	  public void setupStub() {
		  wireMockServer.stubFor(post(urlEqualTo("/" + HttpClientRequest.BUG_PREDICTOR_SERVICE))
				  .willReturn(aResponse().withHeader("Content-Type", "text/plain")
						  .withStatus(200)
						  .withBodyFile("warnings.json")));
		  
		  wireMockServer.stubFor(post(urlEqualTo("/" + HttpClientRequest.INTROSPECT_SERVICE))
				  .willReturn(aResponse().withHeader("Content-Type", "text/plain")
						  .withStatus(200)
						  .withBodyFile("{\"active\" : false}")));
	  }
	  
	  @AfterEach
	  public void teardown () {
		  wireMockServer.stop();
	  }
	  
	  @Test
	  public void testBugPredictor() throws MyRestTemplateException, URISyntaxException, ParseException {
		  JSONObject response = new JSONObject();
		  HttpClientRequest.getWarnings(response, "ddd");
		  assertTrue(response.containsKey("warnings"));
	  }
	  
	  @Test
	  public void testValidateToKenFalse() throws URISyntaxException, ParseException {
		  try {
			  HttpClientRequest.validateToKen("testtoken");
		  } catch (MyRestTemplateException e) {
			  HttpRequestErrorModel erm = e.error_model;
			  assertEquals(erm.rawStatus, 401);
			  assertEquals(erm.description,"Access Token not active");
		  }
	}
	  

}
