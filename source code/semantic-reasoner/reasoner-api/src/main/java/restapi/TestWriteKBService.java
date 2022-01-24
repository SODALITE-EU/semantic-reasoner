package restapi;

import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import kb.configs.ConfigsLoader;
import kb.repository.KB;
@Path("/testKBWrite")
@Api()
public class TestWriteKBService	extends AbstractService {
		private static final Logger LOG = LoggerFactory.getLogger(TemplateService.class.getName());
		
			@GET
			@Produces("text/plain")
			@ApiOperation(
					value = "Returns the result of the communication between reasoner and its dependent components")
			public String testReasoner() throws URISyntaxException, ParseException {  
				//Test defect predictor		
				ConfigsLoader configInstance = ConfigsLoader.getInstance();
				configInstance.loadProperties();
				ValueFactory factory=SimpleValueFactory.getInstance();
				
				KB kb = new KB(configInstance.getGraphdb(), KB.REPOSITORY);
				
				
				IRI user = factory.createIRI("https://www.sodalite.eu/ontologies/workspace/1/test/" + "zoeid");
				ModelBuilder resourceBuilder = new ModelBuilder();
				resourceBuilder.add(user, RDF.TYPE, "soda:User");
				
				Model rmodel = resourceBuilder.build();
				
				try {
					kb.connection.add(rmodel);
				} catch (RepositoryException e) {
					LOG.error(e.getMessage(), e);
					return "RepositoryException";
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
					return "Exception";
				}
				kb.shutDown();
				return "success";
			}

}
