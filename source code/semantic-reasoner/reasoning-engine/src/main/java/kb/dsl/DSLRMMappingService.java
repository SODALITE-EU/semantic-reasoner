package kb.dsl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
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
import kb.repository.KB;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.exceptions.ValidationException;

public class DSLRMMappingService {
	public KB kb;
	public ValueFactory factory;

	public Model rmModel;

	public ModelBuilder builder;

	IRI rmContainer;

	String base = "https://www.sodalite.eu/ontologies/";
//	String ws = base + "workspace/woq2a8g0gscuqos88bn2p7rvlq3/";
//	String ws = "http://";
	String ws = base + "workspace/1/";

	IRI rmKB;
	IRI context;
	String rmURI;
	
	static final String[] TYPES = {"Node", "DataType", "RelationshipType"};


	public DSLRMMappingService(KB kb, String rmTTL, String rmURI) throws RDFParseException, UnsupportedRDFormatException, IOException {
		super();
		this.kb = kb;
		this.factory = SimpleValueFactory.getInstance();

		builder = new ModelBuilder();
		builder.setNamespace("soda", KB.SODA).setNamespace("dul", KB.DUL).setNamespace("dcterms", KB.DCTERMS)
				.setNamespace("exchange", KB.EXCHANGE).setNamespace("tosca", KB.TOSCA);

		//System.out.println(rmTTL);
		InputStream targetStream = IOUtils.toInputStream(rmTTL, Charset.defaultCharset());
		rmModel = Rio.parse(targetStream, "", RDFFormat.TURTLE);
		targetStream.close();

		this.rmURI = rmURI;
	//	context = kb.factory.createIRI("http://" + submissionId);
		//ws += MyUtils.randomString() + "/";
	}

	
	public IRI start() throws MappingException, ValidationException  {

		// AADM
		rmKB = null;
		
		for (Resource _rm : rmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "RM")).subjects()) {
			String userId = Models
					.objectLiteral(rmModel.filter(_rm, factory.createIRI(KB.EXCHANGE + "userId"), null))
					.orElseThrow(
							() -> new MappingException("No 'userId' defined for RM: " + ((IRI) _rm).getLocalName()))
					.stringValue();
			
			
			ws += (rmURI.isEmpty())? MyUtils.randomString() + "/" : MyUtils.getStringPattern(rmURI, ".*/(.*)/RM_.*") + "/";
			System.out.println("namespace = " + ws);
			builder.setNamespace("ws", ws);

			rmKB = (rmURI.isEmpty()) ? factory.createIRI(ws + "RM_" + MyUtils.randomString()) : factory.createIRI(rmURI);
			context = rmKB;
			System.out.println("context =" + context );
			builder.add(rmKB, RDF.TYPE, "soda:ResourceModel");

			IRI user = factory.createIRI(ws + userId);
			builder.add(user, RDF.TYPE, "soda:User");

			builder.add(rmKB, factory.createIRI(KB.SODA + "createdBy"), user);
			builder.add(rmKB, factory.createIRI(KB.SODA + "createdAt"), DateTime.now());
			break;
		}

		if (rmKB == null) {
			throw new MappingException("No RM container found.");
		}
		
		
		// NODES
		for (String type : TYPES) {
			createTypes(type);
		}
		
		return rmKB;

	}
	
	private void createTypes (String type) throws MappingException {
		System.out.println("Add type = " + type + " to the Resource Model");
		for (Resource _node : rmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + type))
				.subjects()) {
			IRI node = (IRI) _node;
			
			String nodeName = Models
					.objectLiteral(rmModel.filter(node, factory.createIRI(KB.EXCHANGE + "name"), null))
					.orElseThrow(
							() -> new MappingException("No 'name' defined for node: " + node.getLocalName()))
					.stringValue();
			
			String nodeType = Models
					.objectLiteral(rmModel.filter(node, factory.createIRI(KB.EXCHANGE + "derivesFrom"), null))
					.orElseThrow(
							() -> new MappingException("No 'derivesFrom' defined for node: " + node.getLocalName()))
					.stringValue();
			
			System.out.println(String.format("Name: %s, type: %s", nodeName, nodeType));

			// add node to the rm container instance
			IRI nodeKB = factory.createIRI(ws + nodeName); // this will be always new
			IRI kbNodeType = getKBNodeType(nodeType, "tosca:tosca.entity.Root");
			
			builder.add(nodeKB, RDF.TYPE, "owl:Class");
			builder.add(nodeKB, RDFS.SUBCLASSOF, kbNodeType);
			builder.add(rmKB, factory.createIRI(KB.SODA + "includes" + type), nodeKB);
			
			// create description
			IRI nodeDescriptionKB = factory.createIRI(ws + "Desc_" + MyUtils.randomString());
			builder.add(nodeDescriptionKB, RDF.TYPE, "soda:SodaliteDescription");
			builder.add(nodeKB, factory.createIRI(KB.SODA + "hasContext"), nodeDescriptionKB);
			
			// properties
			Set<Resource> _properties = Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "properties"));
			//definedPropertiesForValidation.clear();
			for (Resource _property : _properties) {
				IRI property = (IRI) _property;
				IRI propertyClassifierKB = createPropertyOrAttributeKBModel(property);

				// add property classifiers to the template context
				builder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "properties"), propertyClassifierKB);
			}
			
			//attributes
			for (Resource _attribute : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "attributes"))) {
				IRI attribute = (IRI) _attribute;
				IRI attributesClassifierKB = createPropertyOrAttributeKBModel(attribute);

				// add attribute classifiers to the template context
				builder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "attributes"), attributesClassifierKB);

			}
			
			// requirements
			for (Resource _requirement : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "requirements"))) {
				IRI requirement = (IRI) _requirement;
				IRI requirementClassifierKB = createRequirementKBModel(requirement);

				// add requirement classifiers to the template context
				builder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "requirements"),
						requirementClassifierKB);
			}
			
			// capabilities
			for (Resource _capability : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "capabilities"))) {
				IRI capability = (IRI) _capability;
				IRI capabilityClassifierKB = createCapabilityKBModel(capability);

				// add attribute classifiers to the template context
				builder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "capabilities"),
						capabilityClassifierKB);
			}
			
			// interfaces
			for (Resource _interface : Models.getPropertyResources(rmModel, _node,
					factory.createIRI(KB.EXCHANGE + "interfaces"))) {
				IRI interface_iri = (IRI) _interface;
				IRI interfaceClassifierKB = createInterfaceKBModel(interface_iri);

				// add attribute classifiers to the template context
				builder.add(nodeDescriptionKB, factory.createIRI(KB.TOSCA + "interfaces"),
						interfaceClassifierKB);
			}
			
		}
	}
	
	private IRI createRequirementKBModel(IRI requirement) throws MappingException {
		String requirementName = Models
				.objectLiteral(rmModel.filter(requirement, factory.createIRI(KB.EXCHANGE + "name"), null))
				.orElseThrow(
						() -> new MappingException("No 'name' defined for requirement: " + requirement.getLocalName()))
				.stringValue();

		// create classifier
		IRI requirementClassifierKB = factory.createIRI(ws + "ReqClassifier_" + MyUtils.randomString());
		builder.add(requirementClassifierKB, RDF.TYPE, "tosca:Requirement");

		IRI requirementProperty = getKBProperty(requirementName);
		if (requirementProperty == null) {
			//throw new MappingException("Cannot find requirement property: " + requirementName);
			requirementProperty = factory.createIRI(ws + requirementName);
			builder.add(requirementProperty, RDF.TYPE, "rdf:Property");
		}
		
		builder.add(requirementClassifierKB, factory.createIRI(KB.DUL + "classifies"), requirementProperty);

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(rmModel.filter(requirement, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			IRI kbNode = getKBNode(value.getLabel());
			if (kbNode == null) {
				// throw new Exception("Cannot find node: " + value);
				kbNode = factory.createIRI(ws + value.getLabel());
			}
			builder.add(requirementClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
		} else {

			Set<IRI> parameters = createrRequirementParameterKBModel(requirement);
			
			for (IRI parameter : parameters) {
				builder.add(requirementClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), parameter);
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
			throw new MappingException("Cannot find parameters for: " + requirement.getLocalName());
		}
		
		//Requirements can have more than one parameter
		for (Resource _parameter : _parameters) {
			IRI parameter = (IRI) _parameter;
			
			String parameterName = Models
					.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null))
					.orElseThrow(() -> new MappingException("No 'name' defined for parameter: " + parameter.getLocalName()))
					.stringValue();

			// create classifier
			IRI parameterClassifierKB = factory.createIRI(ws + "ParamClassifier_" + MyUtils.randomString());
			builder.add(parameterClassifierKB, RDF.TYPE, "soda:SodaliteParameter");

			parameterClassifiers.add(parameterClassifierKB);
		
			IRI paramProperty = getKBProperty(parameterName);
			if (paramProperty == null) {
				//throw new MappingException("Cannot find requirement parameter: " + parameterName);
				paramProperty = factory.createIRI(ws + parameterName);
				builder.add(paramProperty, RDF.TYPE, "rdf:Property");
			}
			builder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "classifies"), paramProperty);

			// check for direct values of parameters
			Literal value = Models
							.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "value"), null))
							.orElse(null);

			if (value != null) { // this means there is no parameters
				Object i = null;
				//the int check was added for occurrences
				if ((i = Ints.tryParse(value.stringValue())) != null) {
					builder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				} else {
						IRI kbNode = getKBNode(value.getLabel());
						if (kbNode == null) {
							// throw new Exception("Cannot find node: " + value);
							kbNode = factory.createIRI(ws + value.getLabel());
						}
						builder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
					}
			} else {
				Set<IRI> parameterList = createrRequirementParameterKBModel(parameter);
				for (IRI parameter_1 : parameterList) {
					builder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), parameter_1);
				}
			}
		}
	
		return parameterClassifiers;
	}
	
	
	private IRI createCapabilityKBModel(IRI capability) throws MappingException {
		String capabilityName = Models
				.objectLiteral(rmModel.filter(capability, factory.createIRI(KB.EXCHANGE + "name"), null))
				.orElseThrow(
						() -> new MappingException("No 'name' defined for capability: " + capability.getLocalName()))
				.stringValue();

		// create classifier
		IRI capabilityClassifierKB = factory.createIRI(ws + "CapClassifier_" + MyUtils.randomString());
		builder.add(capabilityClassifierKB, RDF.TYPE, "tosca:Capability");

		IRI capabilityProperty = getKBProperty(capabilityName);
		if (capabilityProperty == null) {
			//throw new MappingException("Cannot find capability property: " + capabilityName);
			capabilityProperty = factory.createIRI(ws + capabilityName);
			builder.add(capabilityProperty, RDF.TYPE, "rdf:Property");
		}
		builder.add(capabilityClassifierKB, factory.createIRI(KB.DUL + "classifies"), capabilityProperty);

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(rmModel.filter(capability, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			IRI kbNode = getKBNode(value.getLabel());
			if (kbNode == null) {
				// throw new Exception("Cannot find node: " + value);
				kbNode = factory.createIRI(ws + value.getLabel());
			}
			builder.add(capabilityClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
		} else {
			Set<IRI> parameters = createCapabilityParameterKBModel(capability);
			for (IRI parameter : parameters) {
				builder.add(capabilityClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), parameter);
			}
		}
		return capabilityClassifierKB;
	}	
	
	/*private IRI createInterfaceKBModel(IRI interface_iri) throws MappingException {
		String interfaceName = Models
				.objectLiteral(rmModel.filter(interface_iri, factory.createIRI(KB.EXCHANGE + "name"), null))
				.orElseThrow(
						() -> new MappingException("No 'name' defined for interface: " + interface_iri.getLocalName()))
				.stringValue();

		// create classifier
		IRI interfaceClassifierKB = factory.createIRI(ws + "InterfaceClassifier_" + MyUtils.randomString());
		builder.add(interfaceClassifierKB, RDF.TYPE, "tosca:Interface");

		IRI interfaceProperty = getKBProperty(interfaceName);
		if (interfaceProperty == null) {
			//throw new MappingException("Cannot find interface property: " + interfaceName);
			interfaceProperty = factory.createIRI(ws + interfaceName);
			builder.add(interfaceProperty, RDF.TYPE, "rdf:Property");
		}
		builder.add(interfaceClassifierKB, factory.createIRI(KB.DUL + "classifies"), interfaceProperty);

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(rmModel.filter(interface_iri, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			IRI kbNode = getKBNode(value.getLabel());
			if (kbNode == null) {
				// throw new Exception("Cannot find node: " + value);
				kbNode = factory.createIRI(ws + value.getLabel());
			}
			if (interfaceName == "type") {
				builder.add(interfaceClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} else {
				builder.add(interfaceClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
			}
		} else {
			Set<IRI> parameters = createInterfaceParameterKBModel(interface_iri);
			for (IRI parameter : parameters) {
				builder.add(interfaceClassifierKB, factory.createIRI("dul:hasParameter"), parameter);
			}
		}
		return interfaceClassifierKB;
	}*/
	
	private IRI createInterfaceKBModel(IRI interface_iri) throws MappingException {
		String interfaceName = Models
				.objectLiteral(rmModel.filter(interface_iri, factory.createIRI(KB.EXCHANGE + "name"), null))
				.orElseThrow(
						() -> new MappingException("No 'name' defined for interface: " + interface_iri.getLocalName()))
				.stringValue();

		IRI interfaceProperty = getKBProperty(interfaceName);
		if (interfaceProperty == null) {
			//throw new MappingException("Cannot find interface property: " + interfaceName);
			interfaceProperty = factory.createIRI(ws + interfaceName);
			builder.add(interfaceProperty, RDF.TYPE, "rdf:Property");
		}
		
		Optional<Resource> _type  = Models.getPropertyResource(rmModel, interface_iri,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String type = MyUtils.getStringValue(_type.get());
		
		IRI interfaceClassifierKB = null;
		switch (type) {
			case "Interface":
				interfaceClassifierKB = factory.createIRI(ws + "InterfaceClassifer_" + MyUtils.randomString());
				builder.add(interfaceClassifierKB, RDF.TYPE, "tosca:Interface");
				break;
			case "Parameter":
				interfaceClassifierKB = factory.createIRI(ws + "ParamClassifer_" + MyUtils.randomString());
				builder.add(interfaceClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			default:
				System.err.println("type = " + type + " does not exist");
		}
		builder.add(interfaceClassifierKB, factory.createIRI(KB.DUL + "classifies"), interfaceProperty);

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(rmModel.filter(interface_iri, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			if (interfaceName.equals("type")) {
				IRI kbNode = getKBNode(value.getLabel());
				if (kbNode == null) {
					// throw new Exception("Cannot find node: " + value);
					kbNode = factory.createIRI(ws + value.getLabel());
				}
				builder.add(interfaceClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} else {
				builder.add(interfaceClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
			}
		} else {
			Set<Resource> _parameters = Models.getPropertyResources(rmModel, interface_iri,
					factory.createIRI(KB.EXCHANGE + "hasParameter"));
			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createInterfaceKBModel(parameter);
				builder.add(interfaceClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), _p);
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
			throw new MappingException("Cannot find parameters for: " + capability.getLocalName());
		}
		
		//Requirements or Interfaces can have more than one parameter
		for (Resource _parameter : _parameters) {
			IRI parameter = (IRI) _parameter;
			
			String parameterName = Models
					.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null))
					.orElseThrow(() -> new MappingException("No 'name' defined for parameter: " + parameter.getLocalName()))
					.stringValue();

			// create classifier
			IRI parameterClassifierKB = factory.createIRI(ws + "ParamClassifier_" + MyUtils.randomString());
			builder.add(parameterClassifierKB, RDF.TYPE, "soda:SodaliteParameter");

			parameterClassifiers.add(parameterClassifierKB);
		
			IRI paramProperty = getKBProperty(parameterName);
			if (paramProperty == null) {
				//throw new MappingException("Cannot find requirement parameter: " + parameterName);
				paramProperty = factory.createIRI(ws + parameterName);
				builder.add(paramProperty, RDF.TYPE, "rdf:Property");
			}
			builder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "classifies"), paramProperty);

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
				IRI kbNode = getKBNode(value.getLabel());
				if (kbNode == null) {
					// throw new Exception("Cannot find node: " + value);
					kbNode = factory.createIRI(ws + value.getLabel());
				}
				builder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} else if (listValue != null) { 
				IRI list = factory.createIRI(ws + "List_" + MyUtils.randomString());
				builder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
				builder.add(list, RDF.TYPE, "tosca:List");
				IRI kbNode = getKBNode(listValue.getLabel());
				if (kbNode == null) {
					// throw new Exception("Cannot find node: " + value);
					kbNode = factory.createIRI(ws + listValue.getLabel());
				}
				builder.add(list, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} else {
				Set<IRI> parameterList = createCapabilityParameterKBModel(parameter);
				for (IRI parameter_1 : parameterList) {
					builder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), parameter_1);
				}
			}
		}
	
		return parameterClassifiers;
	}
	
	/*private Set<IRI> createInterfaceParameterKBModel(IRI interfaceParameter) throws MappingException {
		Set <IRI> parameterClassifiers =  new HashSet<>();
		
		Set<Resource> _parameters = Models.getPropertyResources(rmModel, interfaceParameter,
				factory.createIRI(KB.EXCHANGE + "hasParameter"));
					
		Optional<Resource> _parameterType  = Models.getPropertyResource(rmModel, interfaceParameter,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String parameterType = MyUtils.getStringValue(_parameterType.get());
		System.out.println("parameterType = " + parameterType);
		
		//if (_parameters.isEmpty() && _parameterType.equals("Capability")) {
		//	throw new MappingException("Cannot find parameters for: " + capability.getLocalName());
	//	}
		
		//Interfaces can have more than one parameter
		for (Resource _parameter : _parameters) {
			IRI parameter = (IRI) _parameter;
			
			String parameterName = Models
					.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null))
					.orElseThrow(() -> new MappingException("No 'name' defined for parameter: " + parameter.getLocalName()))
					.stringValue();

			// create classifier
			IRI parameterClassifierKB = factory.createIRI(ws + "ParamClassifier_" + MyUtils.randomString());
			builder.add(parameterClassifierKB, RDF.TYPE, "soda:SodaliteParameter");

			parameterClassifiers.add(parameterClassifierKB);
		
			IRI paramProperty = getKBProperty(parameterName);
			if (paramProperty == null) {
				//throw new MappingException("Cannot find requirement parameter: " + parameterName);
				paramProperty = factory.createIRI(ws + parameterName);
				builder.add(paramProperty, RDF.TYPE, "rdf:Property");
			}
			builder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "classifies"), paramProperty);

			// check for direct values of parameters
			Literal value = Models
							.objectLiteral(rmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "value"), null))
							.orElse(null);
			
			if (value != null) { // this means there is no parameters
				if (parameterName.equals("type")) {
					IRI kbNode = getKBNode(value.getLabel());
					if (kbNode == null) {
						// throw new Exception("Cannot find node: " + value);
						kbNode = factory.createIRI(ws + value.getLabel());
					}
					builder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
				} else {
					builder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
				}
			}  else {
				Set<IRI> parameterList = createInterfaceParameterKBModel(parameter);
				for (IRI parameter_1 : parameterList) {
					builder.add(parameterClassifierKB, factory.createIRI("dul:hasParameter"), parameter_1);
				}
			}
		}
	
		return parameterClassifiers;
	}*/
	
	private IRI createPropertyOrAttributeKBModel(IRI exchangeParameter) throws MappingException {
		String propertyName = Models
				.objectLiteral(rmModel.filter(exchangeParameter, factory.createIRI(KB.EXCHANGE + "name"), null))
				.orElseThrow(() -> new MappingException(
						"No 'name' defined for property: " + exchangeParameter.getLocalName()))
				.stringValue();
	
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

	//	definedPropertiesForValidation.add(propertyName);

		System.out.println(String.format("Property name: %s, value: %s", propertyName, _values));

		Optional<Resource> _parameterType  = Models.getPropertyResource(rmModel, exchangeParameter,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String parameterType = MyUtils.getStringValue(_parameterType.get());
		System.out.println("parameterType = " + parameterType);
		
		IRI propertyClassifierKB = null;
		switch (parameterType) {
			case "Attribute":
				propertyClassifierKB = factory.createIRI(ws + "AttrClassifer_" + MyUtils.randomString());
				builder.add(propertyClassifierKB, RDF.TYPE, "tosca:Attribute");
				break;
			case "Parameter":
				propertyClassifierKB = factory.createIRI(ws + "ParamClassifer_" + MyUtils.randomString());
				builder.add(propertyClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			case "Property" :
				propertyClassifierKB = factory.createIRI(ws + "PropClassifer_" + MyUtils.randomString());
				builder.add(propertyClassifierKB, RDF.TYPE, "tosca:Property");
				break;
			default:
				System.err.println("parameterType = " + parameterType + " does not exist");
		}
		
		// create rdf:property
		IRI kbProperty = getKBProperty(propertyName);
		if (kbProperty == null) {
			kbProperty = factory.createIRI(ws + propertyName);
			builder.add(kbProperty, RDF.TYPE, "rdf:Property");
		}
		builder.add(propertyClassifierKB, factory.createIRI(KB.DUL + "classifies"), kbProperty);
		
		Optional<String> description = Models.getPropertyString(rmModel, exchangeParameter,
		factory.createIRI(KB.EXCHANGE + "description"));
		if (description.isPresent())
			builder.add(propertyClassifierKB, factory.createIRI(KB.DCTERMS + "description"), description.get());
		
		// handle values
		if (!_values.isEmpty()) {

			if (_values.size() == 1) {
				Object i = null;
				String value = _values.iterator().next();
				if ((i = Ints.tryParse(value)) != null) {
					builder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				} else if ((i = BooleanUtils.toBooleanObject(value)) != null) {
					builder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (boolean) i);
				} else
					builder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
			} else {
				IRI list = factory.createIRI(ws + "List_" + MyUtils.randomString());
				for (String string : _values) {
					Object i = null;
					if ((i = Ints.tryParse(string)) != null) {
						builder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
					} else if ((i = BooleanUtils.toBooleanObject(string)) != null) {
						builder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (boolean) i);
					} else
						builder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), string);
				}
				builder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
			}

		} else if (!listValues.isEmpty()) {
			System.err.println("*****************************} else if (!listValues.isEmpty()) {");
			IRI list = factory.createIRI(ws + "List_" + MyUtils.randomString());
			for (String string : listValues) {
				Object i = null;
				if ((i = Ints.tryParse(string)) != null) {
					builder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				} else if ((i = BooleanUtils.toBooleanObject(string)) != null) {
					builder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (boolean) i);
				} else
					builder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), string);
			}
			builder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
		} else {
			Set<Resource> _parameters = Models.getPropertyResources(rmModel, exchangeParameter,
					factory.createIRI(KB.EXCHANGE + "hasParameter"));

			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createPropertyOrAttributeKBModel(parameter);

				builder.add(propertyClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), _p);
			}

//			IRI root = createPropertyOrAttributeKBModel(exchangeParameter);
//			builder.add(propertyClassifierKB, factory.createIRI("dul:hasParameter"), root);
		}

		return propertyClassifierKB;

	}
	
	
	private IRI getKBNodeType(String label, String type) {
		String sparql = "select ?x { ?x rdfs:subClassOf " + type + " . FILTER (strends(str(?x), \"/" + label
				+ "\")). }";
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
	
	private IRI getKBProperty(String label) {
		String sparql = "select ?x { ?x a rdf:Property . FILTER (strends(str(?x), \"/" + label + "\")). }";
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
	
	private IRI getKBNode(String label) {
		String sparql = "select ?x { ?x a owl:Thing . FILTER (strends(str(?x), \"" + label + "\")). }";
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
	
	public IRI getContext() {
		return this.context;
	}
	
	public void shutDown() {
		System.out.println("shutting down");
		if (kb != null) {
			kb.shutDown();
		}
	}

	public void save() {
		Model model = builder.build();

		for (Statement string : model) {
			System.err.println(String.format("%-50s %-20s %-40s", MyUtils.getStringValue(string.getSubject()),
					MyUtils.getStringValue(string.getPredicate()), MyUtils.getStringValue(string.getObject())));
		}

		Set<Resource> nodes = Models
				.objectResources(rmModel.filter(rmKB, factory.createIRI(KB.SODA + "includesNode"), null));
		String sparql = "select ?x ?n " + "{ ?x a soda:ResourceModel; "
				+ " 	soda:includesNode ?n ." + "}";
		String query = KB.PREFIXES + sparql;
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query);
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI n = (IRI) bindingSet.getBinding("n").getValue();
			if (nodes.contains(n)) {
				System.out.println("node is = " + n);
			}
		}
		result.close();

		if (this.currentContextExists()) {
			System.out.println(String.format("Context %s exists.", context));
			kb.connection.clear(context);
		} else {
			System.out.println(String.format("New context %s.", context));
		}

		// Rio.write(model, System.out, RDFFormat.TURTLE);
		kb.connection.add(model, context);
	}

	private boolean currentContextExists() {
		return Iterations.asList(kb.connection.getContextIDs()).contains(context);
	}

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
