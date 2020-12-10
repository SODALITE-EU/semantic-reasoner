package kb.dsl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

import kb.dsl.exceptions.MappingException;
import kb.dsl.exceptions.models.DslValidationModel;
import kb.dsl.exceptions.models.MappingValidationModel;
import kb.dsl.utils.GetResources;
import kb.dsl.utils.NamedResource;
import kb.dsl.verify.singularity.VerifySingularity;
import kb.repository.KB;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;

public class DSLRMMappingService {
	public KB kb;
	public ValueFactory factory;

	public Model rmModel;

	public ModelBuilder resourceBuilder;
	public ModelBuilder nodeBuilder;


	String base = "https://www.sodalite.eu/ontologies/";
//	String ws = base + "workspace/woq2a8g0gscuqos88bn2p7rvlq3/";
//	String ws = "http://";
	String ws = base + "workspace/1/";
	final String namespacews = base + "workspace/1/";
	

	IRI rmKB;
	String rmURI;
	IRI namespace;
	String rmDSL;
	String name;
	
	List<DslValidationModel>  mappingModels =  new ArrayList<>();
	
	String currentType;
	List<String> namespacesOfType = new ArrayList<String>();
	
	Set<String> nodeNames = new HashSet<>();


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
		

		//System.out.println(rmTTL);
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

	
	public IRI start() throws MappingException, ValidationException  {

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
			
			ws += (rmURI.isEmpty())? MyUtils.randomString() + "/" : MyUtils.getStringPattern(rmURI, ".*/(.*)/RM_.*") + "/";
			System.out.println("namespace = " + ws);
			resourceBuilder.setNamespace("ws", ws);

			rmKB = (rmURI.isEmpty()) ? factory.createIRI(ws + "RM_" + MyUtils.randomString()) : factory.createIRI(rmURI);
			//context = rmKB;
			//System.out.println("context =" + context );
			resourceBuilder.add(rmKB, RDF.TYPE, "soda:ResourceModel");
			if (userId != null) {
				IRI user = factory.createIRI(ws + userId);
				resourceBuilder.add(user, RDF.TYPE, "soda:User");
				resourceBuilder.add(rmKB, factory.createIRI(KB.SODA + "createdBy"), user);
			}
			resourceBuilder.add(rmKB, factory.createIRI(KB.SODA + "createdAt"), DateTime.now());
			
			if ("".equals(rmDSL)) {
				mappingModels.add(new MappingValidationModel("RM", "rmDSL", "No 'DSL' defined for the rm model"));
			} else {
				resourceBuilder.add(rmKB, factory.createIRI(KB.SODA + "hasDSL"), rmDSL);
			}
			
			resourceBuilder.add(rmKB, factory.createIRI(KB.SODA + "hasName"), name);
			
			break;
		}

		if (rmKB == null) {
			mappingModels.add(new MappingValidationModel("RM", "", "No RM container found."));
		}
		
		retrieveLocalNodeNames();

		createTypes();
		
		System.out.println("Mapping errors = " + mappingModels.toString());
		for (DslValidationModel m:mappingModels) {
			System.err.println(m.toString());
		}
		
		if (!mappingModels.isEmpty())
			throw new MappingException(mappingModels);
		
		try {
			VerifySingularity.removeExistingDefinitions(kb, nodeNames, namespace.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return rmKB;

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
		
	private void createTypes () throws MappingException {
		for (Resource _node : rmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "Type"))
				.subjects()) {
			IRI node = (IRI) _node;
			
			Optional<Literal> _nodeName = Models
					.objectLiteral(rmModel.filter(node, factory.createIRI(KB.EXCHANGE + "name"), null));
			
			String nodeName = null;
			if (!_nodeName.isPresent()) {				
				mappingModels.add(new MappingValidationModel("", node.getLocalName(), "No 'name' defined for type "));
				throw new MappingException(mappingModels);
			}
			else 
				nodeName = _nodeName.get().getLabel();
			
			currentType = nodeName;
			
			Optional<Literal> _nodeType = Models
					.objectLiteral(rmModel.filter(node, factory.createIRI(KB.EXCHANGE + "derivesFrom"), null));
			
			String nodeType = null;
			if (!_nodeType.isPresent()) {
				mappingModels.add(new MappingValidationModel(currentType, node.getLocalName(), "No 'derivesFrom' defined for node"));
				throw new MappingException(mappingModels);
			}
			else 
				nodeType = _nodeType.get().getLabel();
			
			System.out.println(String.format("Name: %s, type: %s", nodeName, nodeType));
			
			NamedResource n = GetResources.setNamedResource(namespacews, nodeType);
			String resourceIRI = n.getResourceURI() ;
			if (resourceIRI != null)
				namespacesOfType = GetResources.getInheritedNamespacesFromType(kb, resourceIRI);
			nodeType = n.getResource();
			System.out.println("namespaceOfType=" + this.namespacesOfType + ", nodeType=" + nodeType);

			IRI nodeDescriptionKB = null;
			if (nodeName != null && nodeType != null) {
				// add node to the rm container instance
				IRI nodeKB = factory.createIRI(namespace + nodeName); // this will be always new
				nodeBuilder.add(nodeKB, factory.createIRI(KB.SODA + "hasName"), nodeName);
			
				IRI kbNodeType = GetResources.getKBNodeType(n , "tosca:tosca.entity.Root", kb);
			
				if (kbNodeType == null) {
					if (nodeNames.contains(nodeType))
						kbNodeType = factory.createIRI(namespace + nodeType);
					else {
						System.err.println("Cannot find Node type, currentType = " + currentType + ", nodeType = "  +  nodeType);
						mappingModels.add(new MappingValidationModel(currentType, nodeType, "Cannot find Node type"));
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

				// add property classifiers to the template context
				if (nodeDescriptionKB != null)
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "properties"), propertyClassifierKB);
			}
			
			//attributes
			for (Resource _attribute : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "attributes"))) {
				IRI attribute = (IRI) _attribute;
				IRI attributesClassifierKB = createPropertyOrAttributeKBModel(attribute);

				// add attribute classifiers to the template context
				if (nodeDescriptionKB != null)
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "attributes"), attributesClassifierKB);

			}
			
			// requirements
			for (Resource _requirement : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "requirements"))) {
				IRI requirement = (IRI) _requirement;
				IRI requirementClassifierKB = createRequirementKBModel(requirement);

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

				// add attribute classifiers to the template context
				if (nodeDescriptionKB != null)
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "capabilities"),
						capabilityClassifierKB);
			}
			
			// interfaces
			for (Resource _interface : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "interfaces"))) {
				IRI interface_iri = (IRI) _interface;
				IRI interfaceClassifierKB = createInterfaceOrTriggerKBModel(interface_iri);

				// add attribute classifiers to the template context
				if (nodeDescriptionKB != null)	
					nodeBuilder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "interfaces"),
						interfaceClassifierKB);
			}
			
			// triggers
			for (Resource _trigger : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "triggers"))) {
				IRI trigger = (IRI) _trigger;
				IRI triggerClassifierKB = createInterfaceOrTriggerKBModel(trigger);
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
			mappingModels.add(new MappingValidationModel(currentType, requirement.getLocalName(), "No 'name' defined for requirement"));
		else
			requirementName = _requirementName.get().getLabel();

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
			NamedResource n = GetResources.setNamedResource(namespacews, value.getLabel());
			IRI kbNode = getKBNode(n);
			if (kbNode == null) {
				if (nodeNames.contains(n.getResource()))
					kbNode = factory.createIRI(namespace + n.getResource());
				else {

					mappingModels.add(new MappingValidationModel(currentType, requirement.getLocalName(), "Cannot find Node: " + value.getLabel() + " for requirement =" + requirement));
					System.err.println(currentType + ": Cannot find Node: " + value.getLabel() + " for requirement =" + requirement);				
				}
			}
			if(kbNode != null)
				nodeBuilder.add(requirementClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
		} else {

			Set<IRI> parameters = createrRequirementParameterKBModel(requirement);
			
			for (IRI parameter : parameters) {
				nodeBuilder.add(requirementClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), parameter);
			}
			//builder.add(requirementClassifierKB, factory.createIRI("dul:hasParameter"), root);
		}

		return requirementClassifierKB;

	}

	private Set<IRI> createrRequirementParameterKBModel(IRI requirement) throws MappingException {
		Set <IRI> parameterClassifiers =  new HashSet<>();
		
		Set<Resource> _parameters = Models.getPropertyResources(rmModel, requirement,
				factory.createIRI(KB.EXCHANGE + "hasParameter"));
		if (_parameters.isEmpty()) {
			mappingModels.add(new MappingValidationModel(currentType, requirement.getLocalName(), "Cannot find parameters"));
		}
		
		//Requirements can have more than one parameter
		for (Resource _parameter : _parameters) {
			IRI parameter = (IRI) _parameter;
			
			Optional<Literal> _parameterName = Models
					.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null));
			
			String parameterName = null;
			if (!_parameterName.isPresent()) {
				mappingModels.add(new MappingValidationModel(currentType, parameter.getLocalName(), "No 'name' defined for parameter"));
			} else {
				parameterName = _parameterName.get().getLabel();
			}		
	
			// create classifier
			IRI parameterClassifierKB = factory.createIRI(namespace + "ParamClassifier_" + MyUtils.randomString());
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
						NamedResource n = GetResources.setNamedResource(namespacews, value.getLabel());
						IRI kbNode = getKBNode(n);
						if (kbNode == null) {
							// throw new Exception("Cannot find node: " + value);
							if (nodeNames.contains(n.getResource()))
								kbNode = factory.createIRI(namespace + n.getResource());
							else {

								mappingModels.add(new MappingValidationModel(currentType, requirement.getLocalName(), "Cannot find Node: " + value.getLabel() + " for requirement parameter =" + parameterName));
								System.err.println(currentType + ": Cannot find Node: " + value.getLabel() + " for requirement parameter =" + requirement.getLocalName());							
							}
						}
						if(kbNode != null)
							nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
					}
			} else {
				Set<IRI> parameterList = createrRequirementParameterKBModel(parameter);
				for (IRI parameter_1 : parameterList) {
					nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), parameter_1);
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
			mappingModels.add(new MappingValidationModel(currentType, capability.getLocalName(), "No 'name' defined for capability"));
		else
			capabilityName = _capabilityName.get().getLabel();

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
			NamedResource n = GetResources.setNamedResource(namespacews, value.getLabel());
			IRI kbNode = getKBNode(n);
			if (kbNode == null) {
				if (nodeNames.contains(n.getResource()))
					kbNode = factory.createIRI(namespace + n.getResource());
				else {

					mappingModels.add(new MappingValidationModel(currentType, capability.getLocalName(), "Cannot find Node: " + value.getLabel() + " for capability"));
					System.err.println(currentType + ": Cannot find Node: " + value.getLabel() + " for capability");
				}
			}
			if(kbNode != null)
				nodeBuilder.add(capabilityClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
		} else {
			Set<IRI> parameters = createCapabilityParameterKBModel(capability);
			for (IRI parameter : parameters) {
				nodeBuilder.add(capabilityClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), parameter);
			}
		}
		return capabilityClassifierKB;
	}	
	
	private IRI createInterfaceOrTriggerKBModel(IRI interface_iri) throws MappingException {
		Optional<Literal> _interfaceName = Models
				.objectLiteral(rmModel.filter(interface_iri, factory.createIRI(KB.EXCHANGE + "name"), null));
		
		String interfaceName = null;
		if (!_interfaceName.isPresent())
			mappingModels.add(new MappingValidationModel(currentType, interface_iri.getLocalName(), "No 'name' defined for interface"));
		else 
			interfaceName = _interfaceName.get().getLabel();
		
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
			case "Interface":
				interfaceClassifierKB = factory.createIRI(namespace + "InterfaceClassifer_" + MyUtils.randomString());
				nodeBuilder.add(interfaceClassifierKB, RDF.TYPE, "tosca:Interface");
				break;
			case "Trigger":
				interfaceClassifierKB = factory.createIRI(namespace + "TriggerClassifer_" + MyUtils.randomString());
				nodeBuilder.add(interfaceClassifierKB, RDF.TYPE, "tosca:Trigger");
				break;
			case "Parameter":
				interfaceClassifierKB = factory.createIRI(namespace + "ParamClassifer_" + MyUtils.randomString());
				nodeBuilder.add(interfaceClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			default:
				System.err.println("type = " + type + " does not exist");
		}
		
		if (interfaceProperty != null)
			nodeBuilder.add(interfaceClassifierKB, factory.createIRI(KB.DUL + "classifies"), interfaceProperty);

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(rmModel.filter(interface_iri, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			if (interfaceName!=null && (interfaceName.equals("type") || interfaceName.equals("node"))) {
				NamedResource n = GetResources.setNamedResource(namespacews, value.getLabel());
				IRI kbNode = getKBNode(n);
				if (kbNode == null) {
					if (nodeNames.contains(n.getResource()))
						kbNode = factory.createIRI(namespace + n.getResource());
					else {
						mappingModels.add(new MappingValidationModel(currentType, interface_iri.getLocalName(), "Cannot find Node: " + value.getLabel() + " for interface = " + interfaceName));
						System.err.println(currentType + ": Cannot find Node: " + value.getLabel() + " for interface " +interfaceName);
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
					factory.createIRI(KB.EXCHANGE + "hasParameter"));
			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createInterfaceOrTriggerKBModel(parameter);
				nodeBuilder.add(interfaceClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), _p);
			}
		}
		return interfaceClassifierKB;
	}
	
	private Set<IRI> createCapabilityParameterKBModel(IRI capability) throws MappingException {
		Set <IRI> parameterClassifiers =  new HashSet<>();
		
		Set<Resource> _parameters = Models.getPropertyResources(rmModel, capability,
				factory.createIRI(KB.EXCHANGE + "hasParameter"));
					
		Optional<Resource> _parameterType  = Models.getPropertyResource(rmModel, capability,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String parameterType = MyUtils.getStringValue(_parameterType.get());
		System.out.println("parameterType = " + parameterType);
		
		if (_parameters.isEmpty() && _parameterType.equals("Capability")) {
			mappingModels.add(new MappingValidationModel(currentType, capability.getLocalName(), "Cannot find parameters"));
		}
		
		for (Resource _parameter : _parameters) {
			IRI parameter = (IRI) _parameter;
			
			Optional<Literal> _parameterName = Models
					.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null));
			
			String parameterName = null;
			if (!_parameterName.isPresent())
				mappingModels.add(new MappingValidationModel(currentType, parameter.getLocalName(), "No 'name' defined for parameter"));
			else
				parameterName = _parameterName.get().getLabel();
			//ZOEE
			// create classifier
			IRI parameterClassifierKB = factory.createIRI(namespace + "ParamClassifier_" + MyUtils.randomString());
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
			Literal value = Models
							.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "value"), null))
							.orElse(null);
			
			System.err.println("-----Value---" + value);
			Literal listValue = Models
					.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "listValue"), null))
					.orElse(null);

			System.err.println("-----ListValue---" + listValue);
			
			if (value != null) { // this means there is no parameters
				NamedResource n = GetResources.setNamedResource(namespacews, value.getLabel());
				System.out.println("namespacews = " + namespacews);
				IRI kbNode = getKBNode(n);
				if (kbNode == null) {
					if (nodeNames.contains(n.getResource()))
						kbNode = factory.createIRI(namespace + n.getResource());
					else {
						mappingModels.add(new MappingValidationModel(currentType, parameter.getLocalName(), "Cannot find Node: " + value.getLabel() +" for parameter =" + parameterName));
						System.err.println(currentType+ ": Cannot find Node: " + value.getLabel() +" for parameter =" + parameterName);
					}
				}
				if (kbNode != null)
					nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} else if (listValue != null) { 
				IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
				nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
				nodeBuilder.add(list, RDF.TYPE, "tosca:List");
				NamedResource n = GetResources.setNamedResource(namespacews, listValue.getLabel());
				IRI kbNode = getKBNode(n);
				if (kbNode == null) {
					if (nodeNames.contains(n.getResource()))
						kbNode = factory.createIRI(namespace + n.getResource());
					else {
						mappingModels.add(new MappingValidationModel(currentType, parameter.getLocalName(), "Cannot find Node: " + listValue.getLabel() +" for parameter =" + parameterName));
						System.err.println(currentType + ": Cannot find Node: " + listValue.getLabel() +" for parameter =" + parameterName);
					}
				}
				if(kbNode != null)
					nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} else {
				Set<IRI> parameterList = createCapabilityParameterKBModel(parameter);
				for (IRI parameter_1 : parameterList) {
					nodeBuilder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), parameter_1);
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
			mappingModels.add(new MappingValidationModel(currentType, exchangeParameter.getLocalName(), "No 'name' defined for property"));
		else
			propertyName = _propertyName.get().getLabel();
		
//		Optional<Literal> _value = Models
//				.objectLiteral(aadmModel.filter(exchangeParameter, factory.createIRI(KB.EXCHANGE + "value"), null));


		Set<String> _values = Models.getPropertyStrings(rmModel, exchangeParameter,
				factory.createIRI(KB.EXCHANGE + "value"));

		Set<String> listValues = Models.getPropertyStrings(rmModel, exchangeParameter,
				factory.createIRI(KB.EXCHANGE + "listValue"));

		System.err.println("------------------" + _values);
		System.err.println("-----ListValues---" + listValues);

		if (_values.isEmpty() && listValues.isEmpty()) {
			System.err.println("No value found for property: " + exchangeParameter.getLocalName());
		}

//		String value = _value.isPresent() ? _value.get().stringValue() : null;

		System.out.println(String.format("Property name: %s, value: %s", propertyName, _values));

		Optional<Resource> _parameterType  = Models.getPropertyResource(rmModel, exchangeParameter,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String parameterType = MyUtils.getStringValue(_parameterType.get());
		System.out.println("parameterType = " + parameterType);
		
		IRI propertyClassifierKB = null;
		switch (parameterType) {
			case "Attribute":
				propertyClassifierKB = factory.createIRI(namespace + "AttrClassifer_" + MyUtils.randomString());
				nodeBuilder.add(propertyClassifierKB, RDF.TYPE, "tosca:Attribute");
				break;
			case "Parameter":
				propertyClassifierKB = factory.createIRI(namespace + "ParamClassifer_" + MyUtils.randomString());
				nodeBuilder.add(propertyClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			case "Property" :
				propertyClassifierKB = factory.createIRI(namespace + "PropClassifer_" + MyUtils.randomString());
				nodeBuilder.add(propertyClassifierKB, RDF.TYPE, "tosca:Property");
				break;
			default:
				System.err.println("parameterType = " + parameterType + " does not exist");
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
		factory.createIRI(KB.EXCHANGE + "description"));
		if (description.isPresent())
			nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.DCTERMS + "description"), description.get());
		
		// handle values
		if (!_values.isEmpty()) {

			if (_values.size() == 1) {
				Object i = null;
				String value = _values.iterator().next();

				IRI kbNode = null;
				NamedResource n = GetResources.setNamedResource(namespacews, value);
				//Check if the node object exists in this local resource model
				if (nodeNames.contains(n.getResource())) {
					kbNode = factory.createIRI(namespace + factory.createLiteral(n.getResource()).getLabel());
					nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
				} else if (!value.isEmpty() && (kbNode = getKBNode(n)) != null) {
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
			System.err.println("*****************************} else if (!listValues.isEmpty()) {");
			IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
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
					factory.createIRI(KB.EXCHANGE + "hasParameter"));

			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createPropertyOrAttributeKBModel(parameter);

				nodeBuilder.add(propertyClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), _p);
			}

//			IRI root = createPropertyOrAttributeKBModel(exchangeParameter);
//			builder.add(propertyClassifierKB, factory.createIRI("dul:hasParameter"), root);
		}

		return propertyClassifierKB;

	}
	
	private IRI createTargetKBModel(IRI target) throws MappingException {
		Set<Literal> listValues= Models.objectLiterals(rmModel.filter(target, factory.createIRI(KB.EXCHANGE + "listValue"), null));
		
		IRI targetClassifierKB = null;
		System.err.println("-----ListValue---" + listValues);
		IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
		
		for (Literal l:listValues) {
			targetClassifierKB = factory.createIRI(namespace + "TargetClassifer_" + MyUtils.randomString());
			nodeBuilder.add(targetClassifierKB, RDF.TYPE, "tosca:Target");
			nodeBuilder.add(list, RDF.TYPE, "tosca:List");
			
			
			NamedResource n = GetResources.setNamedResource(namespacews, l.getLabel());
			IRI kbNode = getKBNode(n);
			if (kbNode == null) {
				if (nodeNames.contains(n.getResource()))
					kbNode = factory.createIRI(namespace + n.getResource());
				else {
					mappingModels.add(new MappingValidationModel(currentType, "targets", "Cannot find target: " + l.getLabel()));
					System.err.println(currentType + ": Cannot find Node: " + l.getLabel());
				}
			}
			if(kbNode != null)
				nodeBuilder.add(list, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
		}
		if (targetClassifierKB != null)
			nodeBuilder.add(targetClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
		
		return targetClassifierKB;		
	}
	
	private IRI getKBNode(NamedResource n) {
		String _namespace = n.getNamespace();
		String _resource = n.getResource();
		
		System.out.println("getKBNode namespace= " + _namespace + ", resource=" + _resource);
		
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
		System.out.println(sparql);
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
		System.out.println("shutting down");
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
