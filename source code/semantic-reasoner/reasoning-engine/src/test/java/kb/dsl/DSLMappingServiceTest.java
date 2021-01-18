package kb.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.eclipse.rdf4j.model.IRI;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import org.eclipse.rdf4j.rio.RDFFormat;

import org.apache.commons.io.IOUtils;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;


import kb.KBApi;
import kb.dsl.exceptions.MappingException;
import kb.dsl.exceptions.models.DslValidationModel;
import kb.dsl.util.RepositoryTestUtils;
import kb.dto.Attribute;
import kb.dto.Capability;
import kb.dto.Interface;
import kb.dto.Property;
import kb.dto.Requirement;
import kb.repository.KB;
import kb.repository.SodaliteRepository;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
//Tests are running in arbitrary order, this is a good practice, tests to be independent
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DSLMappingServiceTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(DSLMappingServiceTest.class.getName());
	private static final String SEMANTIC_REASONER_TEST = "SEMANTIC_REASONER_TEST";
	
	private static SodaliteRepository repositoryManager;
	private static Repository repository;
	private static KB kb;
	private static KBApi api;

	static DSLRMMappingService rm1;
	static DSLRMMappingService rm2;
	static DSLRMMappingService rm3;
	static DSLRMMappingService rm4; 

	@BeforeAll
	 static void loadResourceModels() {
		LOG.info("loadResourceModels");
		repositoryManager = new SodaliteRepository(".", "/config.ttl");
		kb = new KB(repositoryManager, SEMANTIC_REASONER_TEST);
		api = new KBApi(kb);

		repository = repositoryManager.getRepository(SEMANTIC_REASONER_TEST);
		
		RepositoryConnection repositoryConnection = repository.getConnection();
		RepositoryTestUtils.loadCoreOntologies(repositoryConnection);		
	
		IRI rmIRI3, rmIRI4 = null;
		try {
			LOG.info("Loading resource models");			
			//String rmTTL1 = RepositoryTestUtils.fileToString("resource_models/modules.docker_registry.rm.ttl");
			//String rmTTL2 = RepositoryTestUtils.fileToString("resource_models/modules.docker_component.rm.ttl");
			String rmTTL3 = RepositoryTestUtils.fileToString("resource_models/modules.openstack_security_rule.rm.ttl");
			String rmTTL4 = RepositoryTestUtils.fileToString("resource_models/modules.openstack_vm.rm.ttl");
			
			
			/*rm1  = new DSLRMMappingService(kb, rmTTL1,"", "docker","DSL","");
			rm2  = new DSLRMMappingService(kb, rmTTL2,"", "docker","DSL","");*/
			rm3  = new DSLRMMappingService(kb, rmTTL3,"", "openstack","DSL","");
			rm4  = new DSLRMMappingService(kb, rmTTL4,"", "openstack","DSL","");
			
			try {
				/*rm1.start();
				rm1.save();
				
				rm2.start();
				rm2.save();*/
				
				rmIRI3 = rm3.start();
				rm3.save();
				assertNotNull(rmIRI3);
				
				rmIRI4 =rm4.start();
				rm4.save();
				assertNotNull(rmIRI4);
				
				LOG.info("Test Passed: saving rm for openstack");
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

	

	//Test for a snow aadm. It is mapped without any error
	@Test
	void testDSLMappingService() {

		IRI aadmIRI = null;
		DSLMappingService m = null;
		try {
			String aadmTTL = RepositoryTestUtils.fileToString("dsl/snow/ide_snow_v3.ttl");
			m  = new DSLMappingService(kb, aadmTTL,"", false,"snow","DSL","snow.ttl");
			try {
				aadmIRI = m.start();
				m.save();
			} catch (MappingException e) {
				LOG.error(e.getMessage(), e);
				m.shutDown();
			} catch (ValidationException e) {
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					LOG.info("validationModel" + validationModel.toJson());
				}
				m.shutDown();
				return;
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
				m.shutDown();
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			if (m!=null)
				m.shutDown();
		}
		assertNotNull(aadmIRI);
		LOG.info("Test Passed: aadm for snow");
	}
	
	/* Test for required properties that are missing
	   The required registry_ip property has been removed from Template_2. */
	@Test
	void testMissingRequiredProperty() {
		LOG.info("testMissingRequiredProperty");
		DSLMappingService m = null;
		try {
			String aadmTTL = RepositoryTestUtils.fileToString("dsl/snow/ide_snow_v3_required_property_missing.ttl");
			m = new DSLMappingService(kb, aadmTTL,"", false,"snow","DSL","snow.ttl");
			try {
				m.start();
			} catch (ValidationException e) {	
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					LOG.info("validationModel" + validationModel.toJson());
				}
				assertEquals(validationModels.size(),1);
				LOG.info("Test Passed: group_description required property is missing");
				return;
			} catch (Exception e) {
				m.shutDown();
				LOG.error(e.getMessage(), e);
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
	
		if (m != null)
			m.shutDown();
		assertTrue(false);
	}
	
	/* Test for a Mapping Exception.
	   The  name of Template_3 has been removed. */
	@Test
	void testMappingException() {
		LOG.info("testMappingException");
		DSLMappingService m = null;
		try {
			String aadmTTL = RepositoryTestUtils.fileToString("dsl/snow/ide_snow_v3_mapping_exception.ttl");
			m = new DSLMappingService(kb, aadmTTL,"", false,"snow","DSL","snow.ttl");
			try {
				m.start();
				m.save();
			} catch (MappingException e) {
				LOG.info("Test Passed for MappingException: " + e.getMessage());
				List<DslValidationModel> validationModels = e.mappingValidationModels;
				for (DslValidationModel v : validationModels) {
					LOG.info("validationModel" + v.toJson());
				}
				assertEquals(validationModels.size(),1);
				return;
			} catch (ValidationException e) {	
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					LOG.info("validationModel {}", validationModel.toJson());
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			} finally {
				if (m!=null)
					m.shutDown();
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		m.shutDown();
		assertTrue(false);
	}
	//MOVE THOSE KBAPI tests in another test file class
	@Test
	void getProperties() throws IOException {
		LOG.info("getProperties");
		
		Set<Property> properties;
		properties = api.getProperties(api.getResourceIRI("openstack/sodalite.nodes.OpenStack.SecurityRules"), false);
		
		assertTrue(properties.size() == 3);
		LOG.info("Test Passed: getProperties of a node type");
	}
	
	@Test
	void getInterfaces() throws IOException {
		LOG.info("getInterfaces");
		
		Set<Interface> interfaces;
		interfaces = api.getInterfaces(api.getResourceIRI("openstack/sodalite.nodes.OpenStack.VM"), false);
		
		assertTrue(interfaces.size() == 1);
		LOG.info("Test Passed: getInterfaces of a node type");
	}
	
	@Test
	void getRequirements() throws IOException {
		LOG.info("getRequirements");
		
		Set<Requirement> requirements;
		requirements = api.getRequirements(api.getResourceIRI("openstack/sodalite.nodes.OpenStack.VM"), false);
		assertTrue(requirements.size() == 3);
		LOG.info("Test Passed: getRequirements of a node type");
	}
	
	@Test
	void getCapabilities() throws IOException {
		LOG.info("getCapabilities");
		
		Set<Capability> capabilities;
		capabilities = api.getCapabilities(api.getResourceIRI("openstack/sodalite.nodes.OpenStack.VM"), false);
		LOG.info("capabilities.size = {}\n", capabilities.size());
		assertTrue(capabilities.size() == 7);
		LOG.info("Test Passed: getCapabilities of a node type");
	}
	
	@Test
	void getAttributes() throws IOException {
		LOG.info("getAttributes");
		
		Set<Attribute> attributes;
		attributes = api.getAttributes(api.getResourceIRI("openstack/sodalite.nodes.OpenStack.VM"), false);
		
		assertTrue(attributes.size() == 10);
		LOG.info("Test Passed: getAttributes of a node type");
	}
	

	
	@AfterAll
	public static void cleanUp() {
		//rm1.shutDown();
		//rm2.shutDown();
		rm3.shutDown();
		//rm4.shutDown();
		removeRepository();
	}
	
	static void removeRepository() {
		repository.shutDown();
		repositoryManager.removeRepository(SEMANTIC_REASONER_TEST);
		repositoryManager.shutDown("TEST");
	}
	
}
