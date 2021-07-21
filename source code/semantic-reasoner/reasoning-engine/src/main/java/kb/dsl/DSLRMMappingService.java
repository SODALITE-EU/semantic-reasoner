package kb.dsl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.joda.time.DateTime;

import com.google.common.primitives.Ints;

import kb.dsl.dto.DslModel;
import kb.dsl.exceptions.MappingException;
import kb.dsl.exceptions.models.DslValidationModel;
import kb.dsl.exceptions.models.MappingValidationModel;
import kb.dsl.utils.ErrorConsts;
import kb.dsl.utils.GetResources;
import kb.dsl.utils.NamedResource;
import kb.dsl.verify.singularity.VerifySingularity;
import kb.repository.KB;
import kb.repository.KBConsts;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;

public class DSLRMMappingService {
	
	private static final Logger LOG = LoggerFactory.getLogger(DSLRMMappingService.class.getName());
	
	public KB kb;
	public ValueFactory factory;

	public Model rmModel;

	public ModelBuilder resourceBuilder;
	public ModelBuilder nodeBuilder;


	String base = "https://www.sodalite.eu/ontologies/";
//	String ws = base + "workspace/woq2a8g0gscuqos88bn2p7rvlq3/";
//	String ws = "http://";
	String resourcews = KB.BASE_NAMESPACE;
	final String nodews = KB.BASE_NAMESPACE;
	

	IRI rmKB;
	String rmURI;
	IRI namespace;
	String rmDSL;
	String name;
	
	List<DslValidationModel>  mappingModels =  new ArrayList<>();
	
	String currentType;
	List<String> namespacesOfType = new ArrayList<String>();
	
	Set<String> nodeNames = new HashSet<>();
	
	//for mapping errors, contains e.g. node_types
	String currentPrefixType;
	String subMappingPath = "";

	public DSLRMMappingService(KB kb, String rmTTL, String rmURI, String namespace, String rmDSL, String name) throws RDFParseException, UnsupportedRDFormatException, IOException {
		super();
		this.kb = kb;
		this.factory = SimpleValueFactory.getInstance();

		resourceBuilder = new ModelBuilder();
		resourceBuilder.setNamespace("soda", KB.SODA).setNamespace("dul", KB.DUL).setNamespace("dcterms", KB.DCTERMS)
				.setNamespace("exchange", KB.EXCHANGE).setNamespace("tosca", KB.TOSCA);
		
		nodeBuilder = new ModelBuilder();
		nodeBuilder.setNamespace("soda", KB.SODA).setNamespace("dul", KB.DUL).setNamespace("dcterms", KB.DCTERMS)
				.setNamespace("exchange", KB.EXCHANGE).setNamespace("tosca", KB.TOSCA);
		

		InputStream targetStream = IOUtils.toInputStream(rmTTL, Charset.defaultCharset());
		rmModel = Rio.parse(targetStream, "", RDFFormat.TURTLE);
		targetStream.close();

		this.rmURI = rmURI;
		
		if (!"".equals(namespace))
			this.namespace = factory.createIRI(base + "workspace/1/" + namespace + "/");
		else
			this.namespace = factory.createIRI(base + "workspace/1/global/");
		
		this.rmDSL = rmDSL;
		this.name = name;
	}

	
	public DslModel start() throws MappingException, ValidationException  {

		// AADM
		rmKB = null;
		
		for (Resource _rm : rmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "RM")).subjects()) {
			Optional<Literal> _userId = Models
										.objectLiteral(rmModel.filter(_rm, factory.createIRI(KB.EXCHANGE + "userId"), null));
			
			String userId = null;
			if (!_userId.isPresent())
				mappingModels.add(new MappingValidationModel("RM", ((IRI) _rm).getLocalName(), "No 'userId' defined for RM"));
			else
				userId = _userId.get().getLabel();
			
			resourcews += (rmURI.isEmpty())? MyUtils.randomString() + "/" : MyUtils.getStringPattern(rmURI, ".*/(.*)/RM_.*") + "/";
			LOG.info("resourcews = {}", resourcews);
			resourceBuilder.setNamespace("ws", resourcews);

			rmKB = (rmURI.isEmpty()) ? factory.createIRI(resourcews + "RM_" + MyUtils.randomString()) : factory.createIRI(rmURI);
			//context = rmKB;
			resourceBuilder.add(rmKB, RDF.TYPE, "soda:ResourceModel");
			if (userId != null) {
				IRI user = factory.createIRI(resourcews + userId);
				resourceBuilder.add(user, RDF.TYPE, "soda:User");
				resourceBuilder.add(rmKB, factory.createIRI(KB.SODA + "createdBy"), user);
			}
			resourceBuilder.add(rmKB, factory.createIRI(KB.SODA + "createdAt"), DateTime.now());
			
			if ("".equals(rmDSL)) {
				//mappingModels.add(new MappingValidationModel("RM", "rmDSL", "No 'DSL' defined for the rm model"));
				LOG.info("No 'DSL' defined for the rm model");
			} 
			resourceBuilder.add(rmKB, factory.createIRI(KB.SODA + "hasDSL"), rmDSL);
			
			resourceBuilder.add(rmKB, factory.createIRI(KB.SODA + "hasName"), name);
			resourceBuilder.add(rmKB, factory.createIRI(KB.SODA + "hasNamespace"), MyUtils.getNamespaceFromContext(namespace.toString()));
		}

		if (rmKB == null) {
			mappingModels.add(new MappingValidationModel("RM", "", "No RM container found."));
		}
		
		retrieveLocalNodeNames();

		createTypes();
		
		LOG.info("Mapping errors =  = {}", mappingModels.toString());
		for (DslValidationModel m:mappingModels) {
			LOG.info(m.toString());
		}
		
		if (!mappingModels.isEmpty())
			throw new MappingException(mappingModels);
		
		try {
			LOG.info("nodeNames = {}", this.nodeNames);
			VerifySingularity.removeExistingDefinitions(kb, nodeNames, namespace.toString(), rmKB);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		
		//no version in rms
		DslModel model = new DslModel("", rmKB, "");
		return model;

	}
	
	//Retrieve node names of the local resource model, so as to be used as object values in requirements, properties e.t.c
	private void retrieveLocalNodeNames() throws MappingException {
		for (Resource _node : rmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "Type"))
					.subjects()) {
				IRI node = (IRI) _node;
				
				Optional<Literal> nodeName = Models
						.objectLiteral(rmModel.filter(node, factory.createIRI(KB.EXCHANGE + "name"), null));
				
				if (nodeName.isPresent())
					nodeNames.add(nodeName.get().getLabel());
		}
	}
		
	private void createTypes() throws MappingException {
		for (Resource _node : rmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "Type"))
				.subjects()) {
			IRI node = (IRI) _node;			
			
			Optional<Literal> _nodeName = Models
					.objectLiteral(rmModel.filter(node, factory.createIRI(KB.EXCHANGE + "name"), null));
			
			String nodeName = null;
			if (!_nodeName.isPresent()) {				
				mappingModels.add(new MappingValidationModel(currentPrefixType + ErrorConsts.SLASH , node.getLocalName(), "No 'name' defined for type "));
				throw new MappingException(mappingModels);
			}
			else 
				nodeName = _nodeName.get().getLabel();
			
			currentType = nodeName;
			
			Optional<Literal> _nodeType = Models
					.objectLiteral(rmModel.filter(node, factory.createIRI(KB.EXCHANGE + "derivesFrom"), null));
			
			String nodeType = null;
			if (!_nodeType.isPresent()) {
				mappingModels.add(new MappingValidationModel(currentPrefixType + ErrorConsts.SLASH + currentType , node.getLocalName(), "No 'derivesFrom' defined for node"));
				throw new MappingException(mappingModels);
			}
			else 
				nodeType = _nodeType.get().getLabel();
			
			LOG.info("Name: {}, type: {}", nodeName, nodeType);
			
			NamedResource n = GetResources.setNamedResource(nodews, nodeType, kb);
			String resourceIRI = n.getResourceURI() ;
			LOG.info("resourceIRI: {}", resourceIRI);
			if (resourceIRI != null)
				namespacesOfType = GetResources.getInheritedNamespacesFromType(kb, resourceIRI);
			nodeType = n.getResource();
			LOG.info("namespaceOfType: {}, nodeType: {}", this.namespacesOfType, nodeType);

			IRI nodeDescriptionKB = null;
			if (nodeName != null && nodeType != null) {
				// add node to the rm container instance
				IRI nodeKB = factory.createIRI(namespace + nodeName); // this will be always new
				nodeBuilder.add(nodeKB, factory.createIRI(KB.SODA + "hasName"), nodeName);
				
				//assign kind of type - node_types e.t.c needed by IaC builder
				String kindOfType = MyUtils.getStringPattern(node.getLocalName(), "([A-Za-z]+)_\\d+");
				currentPrefixType = KBConsts.AADM_JSON_CLASSES.get(kindOfType);
				nodeBuilder.add(nodeKB, factory.createIRI(KB.SODA + "hasClass"), KBConsts.AADM_JSON_CLASSES.get(kindOfType));
			
				IRI kbNodeType = GetResources.getKBNodeType(n , "tosca:tosca.entity.Root", kb);
			
				if (kbNodeType == null) {
					if (nodeNames.contains(nodeType))
						kbNodeType = factory.createIRI(namespace + nodeType);
					else {
						LOG.info("Cannot find Node type, currentType: {}, nodeType: {}",currentType, nodeType);
						mappingModels.add(new MappingValidationModel(currentPrefixType + ErrorConsts.SLASH + currentType + ErrorConsts.SLASH  + "derived_from" , nodeType, "Cannot find Node type"));
					}
				} 
				
				nodeBuilder.add(nodeKB, RDF.TYPE, "owl:Class");
				if (kbNodeType != null)
					nodeBuilder.add(nodeKB, RDFS.SUBCLASSOF, kbNodeType);
				if (rmKB != null)
					resourceBuilder.add(rmKB, factory.createIRI(KB.SODA + "includesType"), nodeKB);
			
			
				// create description
				nodeDescriptionKB = factory.createIRI(namespace + "Desc_" + MyUtils.randomString());
				nodeBuilder.add(nodeDescriptionKB, RDF.TYPE, "soda:SodaliteDescription");
				nodeBuilder.add(nodeKB, factory.createIRI(KB.SODA + "hasContext"), nodeDescriptionKB);
				
			}
			
			// properties
			Set<Resource> _properties = Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "properties"));
			for (Resource _property : _properties) {
				IRI property = (IRI) _property;
				IRI propertyClassifierKB = createPropertyOrAttributeKBModel(property);
				//clear context path needed for Mapping errors
				subMappingPath = "";
				// add property classifiers to the template context
				if (nodeDescriptionKB != null)
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "properties"), propertyClassifierKB);
			}
			
			//attributes
			for (Resource _attribute : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "attributes"))) {
				IRI attribute = (IRI) _attribute;
				IRI attributesClassifierKB = createPropertyOrAttributeKBModel(attribute);
				//clear context path needed for Mapping errors
				subMappingPath = "";
				// add attribute classifiers to the template context
				if (nodeDescriptionKB != null)
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "attributes"), attributesClassifierKB);

			}
			
			// requirements
			for (Resource _requirement : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "requirements"))) {
				IRI requirement = (IRI) _requirement;
				IRI requirementClassifierKB = createRequirementKBModel(requirement);
				//clear context path needed for Mapping errors
				subMappingPath = "";
				// add requirement classifiers to the template context
				if (nodeDescriptionKB != null)
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "requirements"),
						requirementClassifierKB);
			}
			
			// capabilities
			for (Resource _capability : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "capabilities"))) {
				IRI capability = (IRI) _capability;
				IRI capabilityClassifierKB = createCapabilityKBModel(capability);
				//clear context path needed for Mapping errors
				subMappingPath = "";
				// add attribute classifiers to the template context
				if (nodeDescriptionKB != null)
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "capabilities"),
						capabilityClassifierKB);
			}
			
			// interfaces
			for (Resource _interface : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "interfaces"))) {
				IRI interface_iri = (IRI) _interface;
				IRI interfaceClassifierKB = createInterfaceKBModel(interface_iri);
				//clear context path needed for Mapping errors
				subMappingPath = "";
				// add attribute classifiers to the template context
				if (nodeDescriptionKB != null)	
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "interfaces"),
						interfaceClassifierKB);
			}
			
			// operations
			for (Resource _operation : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "operations"))) {
				IRI operation = (IRI) _operation;
				IRI operationClassifierKB = createOperationKBModel(operation);
				if (nodeDescriptionKB != null)
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "operations"), operationClassifierKB);
			}
			
			// triggers
			for (Resource _trigger : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "triggers"))) {
				IRI trigger = (IRI) _trigger;
				IRI triggerClassifierKB = createTriggerKBModel(trigger);
				//clear context path needed for Mapping errors
				subMappingPath = "";
				// add property classifiers to the template context
				if (nodeDescriptionKB != null)
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "triggers"), triggerClassifierKB);
			}
		
			// targets
			Optional<Resource> _targets = Models.getPropertyResource(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "targets"));
			
			if (!_targets.isEmpty()) {
				IRI targetClassifierKB = createTargetKBModel((IRI)_targets.get());
				if (nodeDescriptionKB != null)	
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "targets"),
							targetClassifierKB);
			}
			
			
		}
		
	}
	
	private IRI createRequirementKBModel(IRI requirement) throws MappingException {
		Optional<Literal> _requirementName = Models
				.objectLiteral(rmModel.filter(requirement, factory.createIRI(KB.EXCHANGE + "name"), null));
		
		String requirementName = null;
		if (!_requirementName.isPresent())
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS),
															requirement.getLocalName(), "No 'name' defined for requirement"));
		else {
			requirementName = _requirementName.get().getLabel();
			subMappingPath += ErrorConsts.SLASH + requirementName;
		}

		// create classifier
		IRI requirementClassifierKB = factory.createIRI(namespace + "ReqClassifier_" + MyUtils.randomString());
		nodeBuilder.add(requirementClassifierKB, RDF.TYPE, "tosca:Requirement");
		
		IRI requirementProperty = null;
		if (requirementName != null) {
			requirementProperty = GetResources.getKBProperty(requirementName, this.namespacesOfType, kb);
			if (requirementProperty == null || requirementProperty.toString().equals(namespace + requirementName)) {
				//throw new MappingException("Cannot find requirement property: " + requirementName);
				requirementProperty = factory.createIRI(namespace + requirementName);
				nodeBuilder.add(requirementProperty, RDF.TYPE, "rdf:Property");
			}
		
			nodeBuilder.add(requirementClassifierKB, factory.createIRI(KB.DUL + "classifies"), requirementProperty);
		}
		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(rmModel.filter(requirement, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);
		
		

		if (value != null) { // this means there is no parameters
			NamedResource n = GetResources.setNamedResource(nodews, value.getLabel(), kb);
			IRI kbNode = getKBNode(n);
			if (kbNode == null) {
				if (nodeNames.contains(n.getResource()))
					kbNode = factory.createIRI(namespace + n.getResource());
				else {
					mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS),
																	requirement.getLocalName(), "Cannot find Node: " + value.getLabel() + " for requirement =" + requirement));
					LOG.warn("{}: Cannot find Node: {} for requirement: {}", currentType, value.getLabel(), requirement);
				}
			}
			if(kbNode != null)
				nodeBuilder.add(requirementClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
		} else {

			Set<IRI> parameters = createrRequirementParameterKBModel(requirement);
			
			for (IRI parameter : parameters) {
				nodeBuilder.add(requirementClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), parameter);
			}
			//builder.add(requirementClassifierKB, factory.createIRI("dul:hasParameter"), root);
		}

		return requirementClassifierKB;

	}

	private Set<IRI> createrRequirementParameterKBModel(IRI requirement) throws MappingException {
		Set <IRI> parameterClassifiers =  new HashSet<>();
		
		Set<Resource> _parameters = Models.getPropertyResources(rmModel, requirement,
				factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));
		if (_parameters.isEmpty()) {
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS), 
														requirement.getLocalName(), "Cannot find parameters"));
		}
		
		//Requirements can have more than one parameter
		for (Resource _parameter : _parameters) {
			IRI parameter = (IRI) _parameter;
			
			Optional<Literal> _parameterName = Models
					.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null));
			
			String parameterName = null;
			if (!_parameterName.isPresent()) {
				mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS), parameter.getLocalName(), 
															"No 'name' defined for parameter"));
			} else {
				parameterName = _parameterName.get().getLabel();
				subMappingPath += ErrorConsts.SLASH + parameterName;
			}		
	
			// create classifier
			IRI parameterClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
			nodeBuilder.add(parameterClassifierKB, RDF.TYPE, "soda:SodaliteParameter");

			parameterClassifiers.add(parameterClassifierKB);
			if (parameterName != null) {
				IRI paramProperty = GetResources.getKBProperty(parameterName, this.namespacesOfType, kb);
				//Create property in case it  already exists in the same namespace (since it ll be removed in save)
				//or a different property found (having the same suffix e.g. configuration_job, job)
				if (paramProperty == null || ((paramProperty.toString().equals(namespace + parameterName) || !MyUtils.getStringValue(paramProperty).equals(parameterName)))) {
					//throw new MappingException("Cannot find requirement parameter: " + parameterName);
					paramProperty = factory.createIRI(namespace + parameterName);
					nodeBuilder.add(paramProperty, RDF.TYPE, "rdf:Property");
				}
				nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "classifies"), paramProperty);
			}
			
			// check for direct values of parameters
			Literal value = Models
							.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "value"), null))
							.orElse(null);

			if (value != null) { // this means there is no parameters
				Object i = null;
				//the int check was added for occurrences
				if ((i = Ints.tryParse(value.stringValue())) != null) {
					nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				} else {
						NamedResource n = GetResources.setNamedResource(nodews, value.getLabel(), kb);
						IRI kbNode = getKBNode(n);
						if (kbNode == null) {
							// throw new Exception("Cannot find node: " + value);
							if (nodeNames.contains(n.getResource()))
								kbNode = factory.createIRI(namespace + n.getResource());
							else {

								mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS), 
															requirement.getLocalName(),	"Cannot find Node: " + value.getLabel() + " for requirement parameter =" + parameterName));
								LOG.warn("{}: Cannot find Node: {} for requirement parameter: {}", currentType, value.getLabel(), requirement.getLocalName());
							}
						}
						if(kbNode != null)
							nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
					}
			} else {
				Set<IRI> parameterList = createrRequirementParameterKBModel(parameter);
				for (IRI parameter_1 : parameterList) {
					nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), parameter_1);
				}
			}
		}
	
		return parameterClassifiers;
	}
	
	
	private IRI createCapabilityKBModel(IRI capability) throws MappingException {
		Optional<Literal> _capabilityName = Models
				.objectLiteral(rmModel.filter(capability, factory.createIRI(KB.EXCHANGE + "name"), null));
		
		String capabilityName = null;
		if (!_capabilityName.isPresent())
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.CAPABILITIES), 
									capability.getLocalName(), "No 'name' defined for capability"));
		else {
			capabilityName = _capabilityName.get().getLabel();
			subMappingPath += ErrorConsts.SLASH + capabilityName;
		}

		// create classifier
		IRI capabilityClassifierKB = factory.createIRI(namespace + "CapClassifier_" + MyUtils.randomString());
		nodeBuilder.add(capabilityClassifierKB, RDF.TYPE, "tosca:Capability");

		if (capabilityName != null) {
			IRI capabilityProperty = GetResources.getKBProperty(capabilityName, this.namespacesOfType, kb);
			if (capabilityProperty == null || capabilityProperty.toString().equals(namespace + capabilityName)) {
				capabilityProperty = factory.createIRI(namespace + capabilityName);
				nodeBuilder.add(capabilityProperty, RDF.TYPE, "rdf:Property");
			}
			nodeBuilder.add(capabilityClassifierKB, factory.createIRI(KB.DUL + "classifies"), capabilityProperty);
		}

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(rmModel.filter(capability, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			NamedResource n = GetResources.setNamedResource(nodews, value.getLabel(), kb);
			IRI kbNode = getKBNode(n);
			if (kbNode == null) {
				if (nodeNames.contains(n.getResource()))
					kbNode = factory.createIRI(namespace + n.getResource());
				else {

					mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.CAPABILITIES)
											, capability.getLocalName(), "Cannot find Node: " + value.getLabel() + " for capability"));
					LOG.warn("{}: Cannot find Node: {} for capability", currentType, value.getLabel());
				}
			}
			if(kbNode != null)
				nodeBuilder.add(capabilityClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
		} else {
			Set<IRI> parameters = createCapabilityParameterKBModel(capability);
			for (IRI parameter : parameters) {
				nodeBuilder.add(capabilityClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), parameter);
			}
		}
		return capabilityClassifierKB;
	}	
	
	private IRI createInterfaceKBModel(IRI interface_iri) throws MappingException {
		
		Optional<Literal> _interfaceName = Models
				.objectLiteral(rmModel.filter(interface_iri, factory.createIRI(KB.EXCHANGE + "name"), null));
		
		LOG.info("createInterfaceKBModel: interface_iri = {}, _interfaceName = {} ", interface_iri, _interfaceName);
		
		String interfaceName = null;
		if (!_interfaceName.isPresent())
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.INTERFACES), 
											interface_iri.getLocalName(), "No 'name' defined for interface"));
		else {
			interfaceName = _interfaceName.get().getLabel();
			subMappingPath += ErrorConsts.SLASH + interfaceName;
		}
		
		IRI interfaceProperty = null;
		if (interfaceName != null) {
			interfaceProperty = GetResources.getKBProperty(interfaceName, this.namespacesOfType, kb);
			if (interfaceProperty == null || interfaceProperty.toString().equals(namespace + interfaceName)) {
				interfaceProperty = factory.createIRI(namespace + interfaceName);
				nodeBuilder.add(interfaceProperty, RDF.TYPE, "rdf:Property");
			}
		}
		
		Optional<Resource> _type  = Models.getPropertyResource(rmModel, interface_iri,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String type = MyUtils.getStringValue(_type.get());
		
		IRI interfaceClassifierKB = null;
		switch (type) {
			case KBConsts.INTERFACE:
				interfaceClassifierKB = factory.createIRI(namespace + "InterfaceClassifier_" + MyUtils.randomString());
				nodeBuilder.add(interfaceClassifierKB, RDF.TYPE, "tosca:Interface");
				break;
			case KBConsts.PARAMETER:
				interfaceClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
				nodeBuilder.add(interfaceClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			default:
				LOG.warn("type = {} does not exist", type);
		}
		
		if (interfaceProperty != null)
			nodeBuilder.add(interfaceClassifierKB, factory.createIRI(KB.DUL + "classifies"), interfaceProperty);
		
		//description is added for triggers
		Optional<String> description = Models.getPropertyString(rmModel, interface_iri,
				factory.createIRI(KB.EXCHANGE + "description"));
		if (description.isPresent())
					nodeBuilder.add(interfaceClassifierKB, factory.createIRI(KB.DCTERMS + "description"), description.get());

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(rmModel.filter(interface_iri, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);
		
		LOG.info("value: {}", value);

		if (value != null) { // this means there is no parameters
			if (interfaceName != null && (interfaceName.equals("type"))) {
				NamedResource n = GetResources.setNamedResource(nodews, value.getLabel(), kb);
				IRI kbNode = getKBNode(n);
				if (kbNode == null) {
					if (nodeNames.contains(n.getResource()))
						kbNode = factory.createIRI(namespace + n.getResource());
					else {
						mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.INTERFACES), interface_iri.getLocalName(), 
												"Cannot find Node: " + value.getLabel() + " for interface = " + interfaceName));
						LOG.warn("{}: Cannot find Node: {} for interface {}", currentType, value.getLabel(), interfaceName);
					}
				}
				if(kbNode != null)
					nodeBuilder.add(interfaceClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} else {
				Object i = null;
				if ((i = Ints.tryParse(value.toString())) != null)
					 nodeBuilder.add(interfaceClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				else 
					nodeBuilder.add(interfaceClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
			}
		} else {
			Set<Resource> _parameters = Models.getPropertyResources(rmModel, interface_iri,
					factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));
			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createInterfaceKBModel(parameter);
				nodeBuilder.add(interfaceClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), _p);
			}
		}
		return interfaceClassifierKB;
	}
	
	//if no different handling needed for trigger, emrge it with interfaces
	private IRI createTriggerKBModel(IRI trigger) throws MappingException {
		Optional<Literal> _triggerName = Models
				.objectLiteral(rmModel.filter(trigger, factory.createIRI(KB.EXCHANGE + "name"), null));
		
		String triggerName = null;
		if (!_triggerName.isPresent())
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.TRIGGERS), trigger.getLocalName(), "No 'name' defined for trigger"));
		else {
			triggerName = _triggerName.get().getLabel();
			subMappingPath += ErrorConsts.SLASH + triggerName;
		}
		
		IRI triggerProperty = null;
		if (triggerName != null) {
			triggerProperty = GetResources.getKBProperty(triggerName, this.namespacesOfType, kb);
			if (triggerProperty == null || triggerProperty.toString().equals(namespace + triggerName)) {
				triggerProperty = factory.createIRI(namespace + triggerName);
				nodeBuilder.add(triggerProperty, RDF.TYPE, "rdf:Property");
			}
		}
		
		Optional<Resource> _type  = Models.getPropertyResource(rmModel, trigger,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String type = MyUtils.getStringValue(_type.get());
		
		IRI triggerClassifierKB = null;
		switch (type) {
			case KBConsts.TRIGGER:
				triggerClassifierKB = factory.createIRI(namespace + "TriggerClassifer_" + MyUtils.randomString());
				nodeBuilder.add(triggerClassifierKB, RDF.TYPE, "tosca:Trigger");
				break;
			case KBConsts.PARAMETER:
				triggerClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
				nodeBuilder.add(triggerClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			default:
				LOG.warn("type = {} does not exist", type);
		}
		
		if (triggerProperty != null)
			nodeBuilder.add(triggerClassifierKB, factory.createIRI(KB.DUL + "classifies"), triggerProperty);
		
		//description is added for triggers
		Optional<String> description = Models.getPropertyString(rmModel, trigger,
				factory.createIRI(KB.EXCHANGE + KBConsts.DESCRIPTION));
		if (description.isPresent())
					nodeBuilder.add(triggerClassifierKB, factory.createIRI(KB.DCTERMS + KBConsts.DESCRIPTION), description.get());

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(rmModel.filter(trigger, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			//probably for capability and requirement different handling needed, check event_fitler_definition in tosca yaml 1.3
			if (triggerName!=null && triggerName.equals("node")) {
				NamedResource n = GetResources.setNamedResource(nodews, value.getLabel(), kb);
				IRI kbNode = getKBNode(n);
				if (kbNode == null) {
					if (nodeNames.contains(n.getResource()))
						kbNode = factory.createIRI(namespace + n.getResource());
					else {
						mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.TRIGGERS), trigger.getLocalName(), "Cannot find Node: " + value.getLabel() + " for trigger = " + triggerName));
						LOG.warn("{}: Cannot find Node: {} for trigger {}", new Object[] {currentType, value.getLabel(), triggerName});
					}
				}
				if(kbNode != null)
					nodeBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} else if (triggerName!=null && (triggerName.equals("capability") || triggerName.equals("requirement"))) {
				IRI req_cap = GetResources.getReqCapFromEventFilter(kb, value.getLabel());
				if (req_cap != null) {
					nodeBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), req_cap);
				} else {
					mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.TRIGGERS), trigger.getLocalName(), "Cannot find " + value.getLabel() + " for trigger = " + triggerName));
					LOG.warn("{}: Cannot find: {} for interface {}", currentType, value.getLabel(), triggerName);
				}
		 	} else {
				Object i = null;
				if ((i = Ints.tryParse(value.toString())) != null)
					nodeBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				else 
					nodeBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
			}
		} else {
			Set<Resource> _parameters = Models.getPropertyResources(rmModel, trigger,
					factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));
			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createTriggerKBModel(parameter);
				nodeBuilder.add(triggerClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), _p);
			}
		}
		return triggerClassifierKB;
	}
	
	private Set<IRI> createCapabilityParameterKBModel(IRI capability) throws MappingException {
		Set <IRI> parameterClassifiers =  new HashSet<>();
		
		Set<Resource> _parameters = Models.getPropertyResources(rmModel, capability,
				factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));
					
		Optional<Resource> _parameterType  = Models.getPropertyResource(rmModel, capability,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		
		if (_parameters.isEmpty() && _parameterType.equals("Capability")) {
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.CAPABILITIES), capability.getLocalName(), "Cannot find parameters"));
		}
		
		for (Resource _parameter : _parameters) {
			IRI parameter = (IRI) _parameter;
			
			Optional<Literal> _parameterName = Models
					.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null));
			
			String parameterName = null;
			if (!_parameterName.isPresent())
				mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.CAPABILITIES), parameter.getLocalName(), "No 'name' defined for parameter"));
			else {
				parameterName = _parameterName.get().getLabel();
				subMappingPath += ErrorConsts.SLASH + parameterName;
			}
			
			// create classifier
			IRI parameterClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
			nodeBuilder.add(parameterClassifierKB, RDF.TYPE, "soda:SodaliteParameter");

			parameterClassifiers.add(parameterClassifierKB);
			if (parameterName != null) {
				IRI paramProperty = GetResources.getKBProperty(parameterName, this.namespacesOfType, kb);
				if (paramProperty == null || paramProperty.toString().equals(namespace + parameterName)) {
					//throw new MappingException("Cannot find requirement parameter: " + parameterName);
					paramProperty = factory.createIRI(namespace + parameterName);
					nodeBuilder.add(paramProperty, RDF.TYPE, "rdf:Property");
				}
				nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "classifies"), paramProperty);
			}

			// check for direct values of parameters
		/*	Literal value = Models
							.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "value"), null))
							.orElse(null);*/
			
			Set<String> _values = Models.getPropertyStrings(rmModel, parameter,
					factory.createIRI(KB.EXCHANGE + "value"));
			
			LOG.info("----_values---- {}", _values);
			Literal listValue = Models
					.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "listValue"), null))
					.orElse(null);

			LOG.info("-----ListValue---- = {}", listValue);
			
			if (!_values.isEmpty()) {
				if (_values.size() == 1) { // this means there is no parameters
					String value = _values.iterator().next();
					NamedResource n = GetResources.setNamedResource(nodews, value, kb);
					LOG.info("namespacews = {}", nodews);
					IRI kbNode = getKBNode(n);
					if (kbNode == null) {
						if (nodeNames.contains(n.getResource()))
							kbNode = factory.createIRI(namespace + n.getResource());
						else {
							mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.CAPABILITIES), parameter.getLocalName(), "Cannot find Node: " + value +" for parameter =" + parameterName));
							LOG.warn("{}: Cannot find: {} for parameter {}", currentType, value, parameterName);
						}
					}
					if (kbNode != null)
						nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
				} else {
					IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
					nodeBuilder.add(list, RDF.TYPE, "tosca:List");
					for (String string : _values) {
						NamedResource n = GetResources.setNamedResource(nodews, string, kb);
						IRI kbNode = getKBNode(n);
						if (kbNode == null) {
							if (nodeNames.contains(n.getResource()))
								kbNode = factory.createIRI(namespace + n.getResource());
							else {
								mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.CAPABILITIES), parameter.getLocalName(), "Cannot find Node: " + string +" for parameter =" + parameterName));
								LOG.warn("{}: Cannot find: {} for parameter {}", currentType, string, parameterName);
							}
							if (kbNode != null)
								nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
						}
						nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
					}
					
				}
			} else if (listValue != null) { 
				IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
				nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
				nodeBuilder.add(list, RDF.TYPE, "tosca:List");
				NamedResource n = GetResources.setNamedResource(nodews, listValue.getLabel(), kb);
				IRI kbNode = getKBNode(n);
				if (kbNode == null) {
					if (nodeNames.contains(n.getResource()))
						kbNode = factory.createIRI(namespace + n.getResource());
					else {
						mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.CAPABILITIES), parameter.getLocalName(), "Cannot find Node: " + listValue.getLabel() +" for parameter =" + parameterName));
						LOG.warn("{}: Cannot find Node: {} for parameter {}", currentType, listValue.getLabel(), parameterName);
					}
				}
				if(kbNode != null)
					nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} else {
				Set<IRI> parameterList = createCapabilityParameterKBModel(parameter);
				for (IRI parameter_1 : parameterList) {
					nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), parameter_1);
				}
			}
		}
	
		return parameterClassifiers;
	}
	
	private IRI createPropertyOrAttributeKBModel(IRI exchangeParameter) throws MappingException {
		Optional<Literal> _propertyName = Models
				.objectLiteral(rmModel.filter(exchangeParameter, factory.createIRI(KB.EXCHANGE + "name"), null));
		String propertyName = null;
		if (!_propertyName.isPresent())
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.CAPABILITIES), exchangeParameter.getLocalName(), "No 'name' defined for property"));
		else {
			propertyName = _propertyName.get().getLabel();
			subMappingPath += ErrorConsts.SLASH + propertyName;
		}
		
//		Optional<Literal> _value = Models
//				.objectLiteral(aadmModel.filter(exchangeParameter, factory.createIRI(KB.EXCHANGE + "value"), null));


		Set<String> _values = Models.getPropertyStrings(rmModel, exchangeParameter,
				factory.createIRI(KB.EXCHANGE + "value"));

		Set<String> listValues = Models.getPropertyStrings(rmModel, exchangeParameter,
				factory.createIRI(KB.EXCHANGE + "listValue"));
		
		LOG.info( "-------------- {}", _values);
		LOG.info("------ListValues------- {}", listValues);

		if (_values.isEmpty() && listValues.isEmpty()) {
			LOG.warn("No value found for property: {}", exchangeParameter.getLocalName());
		}

//		String value = _value.isPresent() ? _value.get().stringValue() : null;

		LOG.info("Property name: {}, value: {}", propertyName, _values);

		Optional<Resource> _parameterType  = Models.getPropertyResource(rmModel, exchangeParameter,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String parameterType = MyUtils.getStringValue(_parameterType.get());
		LOG.info("parameterType: {}", parameterType);
		
		IRI propertyClassifierKB = null;
		switch (parameterType) {
			case KBConsts.ATTRIBUTE:
				propertyClassifierKB = factory.createIRI(namespace + KBConsts.ATTRIBUTE + MyUtils.randomString());
				nodeBuilder.add(propertyClassifierKB, RDF.TYPE, "tosca:Attribute");
				break;
			case KBConsts.PARAMETER:
				propertyClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
				nodeBuilder.add(propertyClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			case KBConsts.PROPERTY:
				propertyClassifierKB = factory.createIRI(namespace + KBConsts.PROP_CLASSIFIER + MyUtils.randomString());
				nodeBuilder.add(propertyClassifierKB, RDF.TYPE, "tosca:Property");
				break;
			default:
				LOG.info( "parameterType: {} does not exist", parameterType);
		}
		
		// create rdf:property
		if(propertyName != null) {
			IRI kbProperty = GetResources.getKBProperty(propertyName, this.namespacesOfType, kb);
		
			if (kbProperty == null || kbProperty.toString().equals(namespace + propertyName)) {
				kbProperty = factory.createIRI(namespace + propertyName);
				nodeBuilder.add(kbProperty, RDF.TYPE, "rdf:Property");
			}
			nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.DUL + "classifies"), kbProperty);
		}
		
		Optional<String> description = Models.getPropertyString(rmModel, exchangeParameter,
		factory.createIRI(KB.EXCHANGE + KBConsts.DESCRIPTION));
		if (description.isPresent())
			nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.DCTERMS + KBConsts.DESCRIPTION), description.get());
		
		// handle values
		if (!_values.isEmpty()) {

			if (_values.size() == 1) {
				Object i = null;
				String value = _values.iterator().next();

				IRI kbNode = null;
				NamedResource n = GetResources.setNamedResource(nodews, value, kb);
				//Check if the node object exists in this local resource model
				if (propertyName.equals("type") && nodeNames.contains(n.getResource())) {
					kbNode = factory.createIRI(namespace + factory.createLiteral(n.getResource()).getLabel());
					nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
				} else if (propertyName.equals("type") && !value.isEmpty() && (kbNode = getKBNode(n)) != null) {
					nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
				} else if ((i = Ints.tryParse(value)) != null) {
					nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				} else if ((i = BooleanUtils.toBooleanObject(value)) != null) {
					nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (boolean) i);
				} else {
					nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
				}
			} else {
				IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
				nodeBuilder.add(list, RDF.TYPE, "tosca:List");
				for (String string : _values) {
					Object i = null;
					if ((i = Ints.tryParse(string)) != null) {
						nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
					} else if ((i = BooleanUtils.toBooleanObject(string)) != null) {
						nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (boolean) i);
					} else
						nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), string);
				}
				nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
			}

		} else if (!listValues.isEmpty()) {
			LOG.info("*****************************} else if (!listValues.isEmpty()) {");
			IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
			nodeBuilder.add(list, RDF.TYPE, "tosca:List");
			for (String string : listValues) {
				Object i = null;
				if ((i = Ints.tryParse(string)) != null) {
					nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				} else if ((i = BooleanUtils.toBooleanObject(string)) != null) {
					nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (boolean) i);
				} else
					nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), string);
			}
			nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
		} else {
			Set<Resource> _parameters = Models.getPropertyResources(rmModel, exchangeParameter,
					factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));

			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createPropertyOrAttributeKBModel(parameter);

				nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), _p);
			}

//			IRI root = createPropertyOrAttributeKBModel(exchangeParameter);
//			builder.add(propertyClassifierKB, factory.createIRI("dul:hasParameter"), root);
		}

		return propertyClassifierKB;

	}
	
	private IRI createTargetKBModel(IRI parameter) throws MappingException {
		LOG.info( "createTargetKBModel: {}", parameter);
		
		Set<Literal> listValues= Models.objectLiterals(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "listValue"), null));
		LOG.info("-----ListValues----: {}", listValues);

		IRI parameterClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
		
		IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
		nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
		nodeBuilder.add(list, RDF.TYPE, "tosca:List");
		
		for (Literal l:listValues) {
			nodeBuilder.add(parameterClassifierKB, RDF.TYPE, "soda:SodaliteParameter");			
			
			NamedResource n = GetResources.setNamedResource(nodews, l.getLabel(), kb);
			IRI kbNode = getKBNode(n);
			if (kbNode == null) {
				if (nodeNames.contains(n.getResource()))
					kbNode = factory.createIRI(namespace + n.getResource());
				else {
					mappingModels.add(new MappingValidationModel(currentPrefixType + ErrorConsts.SLASH + currentType + ErrorConsts.SLASH + ErrorConsts.TARGETS, "targets", "Cannot find target: " + l.getLabel()));
					LOG.warn( "{}: Cannot find Node: {}", l.getLabel());
				}
			}
			if(kbNode != null)
				nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
		}
		
		return parameterClassifierKB;		
	}
	
	private IRI createOperationKBModel(IRI operation) throws MappingException {
		LOG.info( "createOperationKBModel: {}", operation);
		Optional<Literal> _operationName = Models
				.objectLiteral(rmModel.filter(operation, factory.createIRI(KB.EXCHANGE + "name"), null));
		
		String operationName = null;
		if (!_operationName.isPresent())
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.OPERATIONS), operation.getLocalName(), "No 'name' defined for operation"));
		else {
			operationName = _operationName.get().getLabel();
			subMappingPath += ErrorConsts.SLASH + operationName;
		}
		
		IRI operationProperty = null;
		if (operationName != null) {
			operationProperty = GetResources.getKBProperty(operationName, this.namespacesOfType, kb);
			if (operationProperty == null || operationProperty.toString().equals(namespace + operationName)) {
				operationProperty = factory.createIRI(namespace + operationName);
				nodeBuilder.add(operationProperty, RDF.TYPE, "rdf:Property");
			}
		}
		
		Optional<Resource> _type  = Models.getPropertyResource(rmModel, operation,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String type = MyUtils.getStringValue(_type.get());
		
		IRI operationClassifierKB = null;
		switch (type) {
			case KBConsts.OPERATION:
				operationClassifierKB = factory.createIRI(namespace + "OperationClassifer_" + MyUtils.randomString());
				nodeBuilder.add(operationClassifierKB, RDF.TYPE, "tosca:Operation");
				break;
			case KBConsts.PARAMETER:
				operationClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
				nodeBuilder.add(operationClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			default:
				LOG.warn("type = {} does not exist", type);
		}
		
		if (operationProperty != null)
			nodeBuilder.add(operationClassifierKB, factory.createIRI(KB.DUL + "classifies"), operationProperty);
		
		//description is added for triggers
		Optional<String> description = Models.getPropertyString(rmModel, operation,
						factory.createIRI(KB.EXCHANGE + KBConsts.DESCRIPTION));
		if (description.isPresent())
				nodeBuilder.add(operationClassifierKB, factory.createIRI(KB.DCTERMS + KBConsts.DESCRIPTION), description.get());

		// check for direct values of parameters
		Literal value = Models
						.objectLiteral(rmModel.filter(operation, factory.createIRI(KB.EXCHANGE + "value"), null))
						.orElse(null);
	
		if (value != null) { // this means there is no parameters
			Object i = null;
			if ((i = Ints.tryParse(value.toString())) != null)
				nodeBuilder.add(operationClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
			else 
				nodeBuilder.add(operationClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
			
		} else {
			Set<Resource> _parameters = Models.getPropertyResources(rmModel, operation,
					factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));
			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createOperationKBModel(parameter);
				nodeBuilder.add(operationClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), _p);
			}
		}
		return operationClassifierKB;
	}
	
	//for the context path of Mapping errors
	private  String getContextPath(String entity) {
		return currentPrefixType + ErrorConsts.SLASH + currentType + ErrorConsts.SLASH + entity + subMappingPath;
	}
	
	private IRI getKBNode(NamedResource n) {
		String _namespace = n.getNamespace();
		String _resource = n.getResource();
		
		LOG.info("getKBNode namespace = {}, resource = {}", _namespace, _resource);
		
		String sparql = "select ?x { \r\n" +
						"  {\r\n " +
						"    ?x rdf:type owl:Class .\r\n" +
						"   FILTER NOT EXISTS\r\n" +
						"   { \r\n" +
						"     GRAPH ?g { ?x ?p ?o } \r\n" +
						"   }\r\n" +
						"  }\r\n";
		
		if (_namespace != null && !_namespace.contains("global"))
			sparql += 	" UNION {\r\n" +
						"     GRAPH " + "<"+ _namespace + ">\r\n" +
						"     {\r\n" +
						"	     ?x rdf:type owl:Class . \r\n" +
						"     }\r\n" +
						" }\r\n";
		
		sparql += " ?x rdfs:subClassOf ?class .\r\n" +
				  " FILTER (?class IN (tosca:tosca.entity.Root, tosca:DataType ))\r\n"+
				  " FILTER (strends(str(?x), \"" + _resource + "\")). \r\n" +
				  "}";
		LOG.info(sparql);
		String query = KB.PREFIXES + sparql;

		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query);

		IRI x = null;
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			x = (IRI) bindingSet.getBinding("x").getValue();
		}
		result.close();
		return x;
	}
	
	public Set<String> getNodeNames() {
		return this.nodeNames;
	}
	
	public IRI getNamespace() {
		return this.namespace;
	}
	
	public void shutDown() {
		LOG.info("shutting down");
		if (kb != null) {
			kb.shutDown();
		}
	}

	public void save() {
		Model rmodel = resourceBuilder.build();
		Model nmodel = nodeBuilder.build();

		kb.connection.add(rmodel);
		if (namespace.toString().contains("global"))
			kb.connection.add(nmodel);
		else
			kb.connection.add(nmodel,namespace);
	}

}
