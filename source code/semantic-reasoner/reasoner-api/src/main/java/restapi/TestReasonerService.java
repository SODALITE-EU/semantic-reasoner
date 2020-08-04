package restapi;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.ClientProtocolException;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import kb.utils.ConfigsLoader;
import restapi.utils.HttpClientRequest;

/** A service that submits the abstract application deployment model to the Knowledge Base.
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/testReasoner")
@Api()
public class TestReasonerService extends AbstractService {

	@GET
	@Produces("text/plain")
	@ApiOperation(
			value = "Returns the result of the communication between reasoner and its dependent components")
	public Response testReasoner() {
		//Test defect predictor		
		ConfigsLoader configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
		
		if (configInstance.getBugPredictorServer() == null) {
			return Response.status(Status.BAD_REQUEST).entity("no defect predictor url set").build();
		}
		
		try {
			String response = HttpClientRequest.bugPredictorApi("123");
			if (response.equals("Unreachable"))
				return Response.status(Status.BAD_REQUEST).entity("Error connecting to defect predictor host: " + configInstance.getBugPredictorServer()).build();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity("Error while trying to connect to defect-predictor").build();
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity("Error while trying to connect to defect-predictor").build();
		}
		
		//test graphdb
		String message = null;
		String graphdb = configInstance.getGraphdb();
		if (graphdb == null) {
			return Response.status(Status.BAD_REQUEST).entity("no graphdb url set").build();
		}
		
		RepositoryManager _manager;
		_manager = new RemoteRepositoryManager(graphdb);
		try {
			_manager.getAllRepositoryInfos();
		} catch (Exception exception) {
			message = "graphdb host is unknown: " + graphdb;
			return Response.status(Status.BAD_REQUEST).entity(message).build();
		}
		return Response.ok("Successfully connected to both defect predictor and graph-db").build();
	}
}