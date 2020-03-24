package kb.dsl;

import static org.junit.jupiter.api.Assertions.*;

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

import kb.dsl.exceptions.MappingException;
import kb.repository.KB;
import kb.repository.SodaliteRepository;
import kb.utils.MyUtils;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;

class DSLMappingServiceTest {
	
    private String aadmTTL;
    private static SodaliteRepository repositoryManager;
    private static Repository repository;
    private static KB kb;
	
	@BeforeAll
	static void beforeAll() {
			repositoryManager = new SodaliteRepository(".", "/config.ttl");
			kb = new KB(repositoryManager, "SEMANTIC_REASONER_TEST");

			repository = repositoryManager.getRepository("SEMANTIC_REASONER_TEST");
			
			RepositoryConnection repositoryConnection = repository.getConnection();
			try {
				InputStream input =
					DSLMappingServiceTest.class.getResourceAsStream("/import/DUL.rdf");
				repositoryConnection.add(input, "", RDFFormat.RDFXML);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				InputStream input =
						DSLMappingServiceTest.class.getResourceAsStream("/core/sodalite-metamodel.ttl");
				repositoryConnection.add(input, "", RDFFormat.TURTLE);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				InputStream input =
					DSLMappingServiceTest.class.getResourceAsStream("/core/tosca-builtins.ttl");
				repositoryConnection.add(input, "", RDFFormat.TURTLE);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				InputStream input =
					DSLMappingServiceTest.class.getResourceAsStream("/snow/snow_tier1.ttl");
				repositoryConnection.add(input, "", RDFFormat.TURTLE);
			} catch (IOException e) {
				e.printStackTrace();
			}

			repositoryConnection.close();
	}

	@AfterAll
	static void afterAll() {
		repository.shutDown();
		repositoryManager.removeRepository("SEMANTIC_REASONER_TEST");
		repositoryManager.shutDown("TEST");
	}
	
	@Test
	void testDSLMappingService() {
		IRI aadmIRI = null;
		try {
			aadmTTL = MyUtils.fileToString("dsl/ide_snow_v3.ttl");
			DSLMappingService m  = new DSLMappingService(kb, aadmTTL, "test");
			try {
				aadmIRI = m.start();
				m.save();
			} catch (MappingException e) {
				e.printStackTrace();
			} catch (ValidationException e) {	
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					System.out.println("validationModel" + validationModel.toJson());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
			finally {
				m.shutDown();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		assertNotNull(aadmIRI);
		System.out.println("Test Passed: aadm for snow");
	}
	
}
