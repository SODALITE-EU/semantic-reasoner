package kb.dsl;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import httpclient.HttpClientRequest;
import httpclient.dto.HttpRequestErrorModel;
import httpclient.exceptions.MyRestTemplateException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpClientRequestTest {
	private static final Logger LOG = Logger.getLogger(HttpClientRequestTest.class.getName());
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
	public static void testBugPredictor() throws MyRestTemplateException, URISyntaxException, ParseException {
		HttpClientRequest.setBugPredictor("http://localhost:" + wireMockPort+"/");
		JSONObject response = new JSONObject();
		HttpClientRequest.getWarnings(response, "ddd");
		assertTrue(response.containsKey("warnings"));
		LOG.info("Test Passed: testBugPredictor");
	}
	  
	@Test
	public static void testValidateToKenFalse() throws URISyntaxException, ParseException {
		HttpClientRequest.setKeyCloak("http://localhost:" + wireMockPort+"/");
		try {
			HttpClientRequest.validateToKen("testtoken");
		} catch (MyRestTemplateException e) {
			HttpRequestErrorModel erm = e.error_model;
			assertEquals(erm.rawStatus, 401);
			assertEquals(erm.description,"Access Token not active");
			LOG.info("Test Passed: testValidateToKenFalse");
		}
	}
	  
	private static int getAvailablePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0); ) {
			int port = socket.getLocalPort();
			LOG.log(Level.INFO,"Using port {0}", port);
			return port;
		}
	}
}
