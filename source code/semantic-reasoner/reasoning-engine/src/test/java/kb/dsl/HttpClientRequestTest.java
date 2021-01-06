package kb.dsl;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpStatus;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import httpclient.HttpClientRequest;
import httpclient.dto.AuthErrorModel;

import httpclient.exceptions.AuthException;
import httpclient.exceptions.MyRestTemplateException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpClientRequestTest {
	private static final Logger LOG = LoggerFactory.getLogger(HttpClientRequestTest.class.getName());
	private static WireMockServer wireMockServer;
	private static int wireMockPort;
	  
	
	@BeforeAll
	public static void setup() throws IOException {
		wireMockPort = getAvailablePort();
		wireMockServer = new WireMockServer(options().port(wireMockPort)); 
		wireMockServer.start();
		WireMock.configureFor("localhost", wireMockPort);
		
		setupStub();
	}
	  
	public static void setupStub() {
		wireMockServer.stubFor(post(urlEqualTo("/" + HttpClientRequest.BUG_PREDICTOR_SERVICE))
				.willReturn(aResponse().withHeader("Content-Type", "text/plain")
						.withStatus(200)
						.withBodyFile("warnings.json")));
		  wireMockServer.stubFor(post(urlEqualTo("/" + HttpClientRequest.INTROSPECT_SERVICE))
				  .willReturn(aResponse().withHeader("Content-Type", "application/json")
						  .withStatus(200)
						  .withBody("{\"active\" : false}")));
	}
	  
	@AfterAll
	public static void teardown () {
		wireMockServer.stop();
	}
	  
	@Test
	public void testBugPredictor() throws MyRestTemplateException, URISyntaxException, ParseException {
		HttpClientRequest.setBugPredictor("http://localhost:" + wireMockPort+"/");
		JSONObject response = new JSONObject();
		HttpClientRequest.getWarnings(response, "ddd");
		assertTrue(response.containsKey("warnings"));
		LOG.info("Test Passed: testBugPredictor");
	}
	  
	@Test
	public void testValidateToKenFalse() throws URISyntaxException, ParseException {
		HttpClientRequest.setKeyCloak("http://localhost:" + wireMockPort+"/");
		try {
			HttpClientRequest.validateToKen("testtoken");
		} catch (AuthException e) {
			List<AuthErrorModel> models = e.roleModels;
			for (AuthErrorModel r : models) {
				assertEquals(r.statusCode, HttpStatus.UNAUTHORIZED);
				assertEquals(r.description,"Access Token not active");
			}
			
			LOG.info("Test Passed: testValidateToKenFalse");
		}
	}
	  
	private static int getAvailablePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0); ) {
			int port = socket.getLocalPort();
			LOG.info("Using port {}", port);
			return port;
		}
	}
}
