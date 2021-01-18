package kb.dsl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kb.dsl.exceptions.MappingException;
import kb.dsl.util.RepositoryTestUtils;
import kb.repository.KB;
import kb.repository.SodaliteRepository;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PolicyMappingTest {
	private static final Logger LOG = LoggerFactory.getLogger(PolicyMappingTest.class.getName());
	private static final String SEMANTIC_REASONER_TEST = "SEMANTIC_REASONER_TEST";
	
	private static SodaliteRepository repositoryManager;
	private static Repository repository;
	private static KB kb;

	

	static DSLRMMappingService rm1;
	static DSLMappingService aadm1; 
	
	static RepositoryConnection repositoryConnection;

	@BeforeAll
	 static void loadResourceModels() {		
		LOG.info("loadRepository");
		repositoryManager = new SodaliteRepository(".", "/config.ttl");
		kb = new KB(repositoryManager, SEMANTIC_REASONER_TEST);

		repository = repositoryManager.getRepository(SEMANTIC_REASONER_TEST);
	
		repositoryConnection = repository.getConnection();
		
		RepositoryTestUtils.loadCoreOntologies(repositoryConnection);
	}
	
	
	@Test
	void testDSLMappingPolicyService() {	
		IRI  rmIRI1 = null;
		IRI aadmIRI = null;
		try {
			LOG.info("Loading resource models");			
			
			String rmTTL1 = RepositoryTestUtils.fileToString("resource_models/policies.test.rm.ttl");
			
			
			rm1  = new DSLRMMappingService(kb, rmTTL1,"", "radon","DSL","");
			
			try {
				rmIRI1 = rm1.start();
				rm1.save();
				assertNotNull(rmIRI1);
				
				aadmIRI = aadm1.start();
				aadm1.save();
				assertNotNull(aadmIRI);
				
				LOG.info("Test Passed: saving rm and aadm for policy");
			} catch (MappingException e) {
				LOG.error(e.getMessage(), e);
			} catch (ValidationException e) {
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					LOG.info("validationModel" + validationModel.toJson());
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
			
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		
		repositoryConnection.close();
	}
	
	
	@AfterAll
	public static void cleanUp() {
		rm1.shutDown();
		removeRepository();
	}
	
	static void removeRepository() {
		repository.shutDown();
		repositoryManager.removeRepository(SEMANTIC_REASONER_TEST);
		repositoryManager.shutDown("TEST");
	}

	
}
