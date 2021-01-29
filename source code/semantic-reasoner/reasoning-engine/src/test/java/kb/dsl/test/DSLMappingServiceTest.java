package kb.dsl.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.eclipse.rdf4j.model.IRI;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;


import kb.KBApi;
import kb.dsl.DSLMappingService;
import kb.dsl.DSLRMMappingService;
import kb.dsl.exceptions.MappingException;
import kb.dsl.exceptions.models.DslValidationModel;
import kb.dsl.test.util.RepositoryTestUtils;
import kb.dsl.verify.singularity.VerifySingularity;
import kb.dto.AADM;
import kb.dto.Attribute;
import kb.dto.Capability;
import kb.dto.Interface;
import kb.dto.Node;
import kb.dto.NodeFull;
import kb.dto.NodeType;
import kb.dto.Property;
import kb.dto.Requirement;
import kb.dto.SodaliteAbstractModel;
import kb.repository.KB;
import kb.repository.SodaliteRepository;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;

//Tests are running in arbitrary order, this is a good practice, tests to be independent
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DSLMappingServiceTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(DSLMappingServiceTest.class.getName());
	
	private static SodaliteRepository repositoryManager;
	private static Repository repository;
	private static KB kb;
	private static KBApi api;

	static DSLRMMappingService rm1;
	static DSLRMMappingService rm2;
	static DSLRMMappingService rm3;
	static DSLRMMappingService rm4;
	
	static IRI rmIRI3;

	static IRI rmIRI4 = null;
	static IRI aadmIRI = null;

	@BeforeAll
	 static void loadResourceModels() {
		LOG.info("loadResourceModels");
		repositoryManager = new SodaliteRepository("target/", "/config.ttl");
		kb = new KB(repositoryManager, RepositoryTestUtils.SEMANTIC_REASONER_TEST);
		api = new KBApi(kb);

		repository = repositoryManager.getRepository(RepositoryTestUtils.SEMANTIC_REASONER_TEST);
		
		RepositoryConnection repositoryConnection = repository.getConnection();
		RepositoryTestUtils.loadCoreOntologies(repositoryConnection);		
	
		try {
			LOG.info("Loading resource models");
			String rmTTL1 = RepositoryTestUtils.fileToString("resource_models/modules.docker_registry.rm.ttl");	
			//String rmTTL2 = RepositoryTestUtils.fileToString("resource_models/modules.docker_component.rm.ttl");
			
			String rmTTL3 = RepositoryTestUtils.fileToString("resource_models/modules.openstack_security_rule.rm.ttl");
			String rmTTL4 = RepositoryTestUtils.fileToString("resource_models/modules.openstack_vm.rm.ttl");
			
			rm1  = new DSLRMMappingService(kb, rmTTL1,"", "docker","DSL","");
			rm3  = new DSLRMMappingService(kb, rmTTL3,"", "openstack","DSL","");
			rm4  = new DSLRMMappingService(kb, rmTTL4,"", "openstack","DSL","");
			
			try {
				rm1.start();
				rm1.save();
				
				rmIRI3 = rm3.start();
				rm3.save();
				assertNotNull(rmIRI3);
				
				rmIRI4 =rm4.start();
				rm4.save();
				assertNotNull(rmIRI4);
				
				DSLMappingService m = null;
				String aadmTTL = RepositoryTestUtils.fileToString("dsl/snow/ide_snow_v3.ttl");
				m  = new DSLMappingService(kb, aadmTTL,"", false,"snow","DSL","snow.ttl");
				aadmIRI = m.start();
				assertNotNull(aadmIRI);
				m.save();
				
				LOG.info("Test Passed: saving rm and aadm for openstack");
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
			} catch (ValidationException e) {
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					LOG.info("validationModel" + validationModel.toJson());
				}
				return;
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
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
				LOG.error(e.getMessage(), e);
				fail("Exception was thrown in start");
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			fail("Exception was thrown");
		}
	
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
			}
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
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
	
	
	@Test
	void getPropAttrs() throws IOException {
		LOG.info("getPropAttrs");
		
		Set<String> names = api.getPropAttrNames(api.getResourceIRI("openstack/sodalite.nodes.OpenStack.SecurityRules"), "prop");
		System.out.println("NAMES = " + names.toString());
		assertTrue(names.size() == 3);
		LOG.info("Test Passed: getPropAttrs for a node template");
	}
	
	@Test
	void getNodes() throws IOException {
		LOG.info("getNodes");
		List<String> imports = new ArrayList<>(); 
		  
		imports.add("docker"); 
		Set<Node> nodes = api.getNodes(imports, "node");
		assertTrue(nodes.size() == 9);
		LOG.info("Test Passed: getNodes for TOSCA normative node types and types in docker namespace");
	}
	
	@Test
	void getNode() throws IOException {
		LOG.info("getNode");
		String nodeType = KB.BASE_NAMESPACE + "openstack/" + "sodalite.datatypes.OpenStack.SecurityRule";
		NodeFull node = api.getNode(nodeType, true);
		assertTrue(node.getUri().equals(nodeType));
		LOG.info("Test Passed: getNode for custom node type");
	}
	
	@Test
	void getTemplates() throws IOException {
		LOG.info("getTemplates");
		List<String> imports = new ArrayList<>(); 
		  
		imports.add("snow"); 
		Set<Node> nodes = api.getTemplates(imports);
		assertTrue(nodes.size() == 1);
		LOG.info("Test Passed: getTemplates for snow");
	}
	
	@Test
	void getAADM() throws IOException {
		LOG.info("getAADM");
		
		AADM aadm = api.getAADM(aadmIRI.toString());
		Set<NodeFull> templates = aadm.getTemplates();

		assertTrue(templates.size() == 2);
		LOG.info("Test Passed: getAADM");
	
		LOG.info("removeInputs");
		VerifySingularity.removeInputs(kb, aadmIRI.toString());
		
		String input = null;
		for (NodeFull n: templates) {
			String label = n.getLabel().toString();
			if(label.startsWith("topology_template_inputs"))
				input = n.getUri();
		}
		
		Set<Property> inputs = api.getInputs(input, true);
		assertTrue(inputs.size() == 0);
		
		LOG.info("Test Passed: removeInputs");
	}
	
	
	@Test
	void isSubClassOf() throws IOException {
		LOG.info("isSubClassOf");
		List<String> nodeTypes = new ArrayList<>();
		nodeTypes.add("openstack/sodalite.nodes.OpenStack.SecurityRules");
		
		Set<String> subNodes = api.isSubClassOf(nodeTypes, "tosca.nodes.Root");

		assertTrue(subNodes.size() == 1);
		LOG.info("Test Passed: isSubClassOf");
	}
	
	@Test
	void getRequirementValidNodeType() throws IOException {
		LOG.info("getRequirementValidNodeType");
		List<String> imports = new ArrayList<>();
		imports.add("docker");
		
		Set<NodeType> nodeTypes = api.getRequirementValidNodeType("host", "docker/sodalite.nodes.DockerRegistry", imports);
		assertTrue(nodeTypes.size() == 1);
	}
	
	@Test
	void getRequirementValidNodes() throws IOException {
		LOG.info("getRequirementValidNodes");
		List<String> imports = new ArrayList<>();
		imports.add("docker");
		
		Set<Node> nodes = api.getRequirementValidNodes("host", "docker/sodalite.nodes.DockerRegistry", imports);
		assertTrue(nodes.size() == 0);
	}
	
	@Test
	void testGetModels() throws IOException {
		LOG.info("testGetModels");
		try {
			Set<SodaliteAbstractModel> models = api.getModels("RM", KB.BASE_NAMESPACE + "openstack/");
		
			assertEquals(models.size(), 2);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			fail("Exception was thrown");
		}
		
		LOG.info("Test Passed: Get Models");
	}
	
	@Test
	void testGetModelForResource()  {
		LOG.info("testGetModel");
		try {
			SodaliteAbstractModel model = api.getModelForResource("sodalite.nodes.OpenStack.SecurityRules", KB.BASE_NAMESPACE + "openstack/");
			assertEquals(model.getUri(), rmIRI3.toString());
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			fail("IOException was thrown");
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			fail("Exception was thrown");
		}
		
		LOG.info("Test Passed: Get Model For Resource");
	}
	
	//@Test
	void testGetModelForURI()  {
		LOG.info("testGetModelForURI");
		try {
			SodaliteAbstractModel model = api.getModelFromURI(rmIRI3.toString());
			assertEquals(model.getUri(), rmIRI3.toString());
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			fail("IOException was thrown");
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			fail("Exception was thrown");
		}
		
		LOG.info("Test Passed: Get Model For URI");
	}

	
	@AfterAll
	public static void cleanUp() {
		//rm1.shutDown();
		//rm2.shutDown();
		rm3.shutDown();
		//rm4.shutDown();
		RepositoryTestUtils.removeRepository(repository, repositoryManager);
	}
}
