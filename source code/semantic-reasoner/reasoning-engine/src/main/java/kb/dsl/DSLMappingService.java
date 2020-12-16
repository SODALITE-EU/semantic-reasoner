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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.joda.time.DateTime;
import org.joda.time.base.AbstractDateTime;

import com.google.common.primitives.Ints;

import kb.clean.ModifyKB;
import kb.dsl.exceptions.MappingException;
import kb.dsl.exceptions.models.DslValidationModel;
import kb.dsl.exceptions.models.MappingValidationModel;
import kb.dsl.utils.GetResources;
import kb.dsl.utils.NamedResource;
import kb.dsl.verify.singularity.VerifySingularity;
import kb.repository.KB;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.RequirementExistenceValidation;
import kb.validation.RequirementValidation;
import kb.validation.ValidationService;
import kb.validation.exceptions.CapabilityMismatchValidationException;
import kb.validation.exceptions.NoRequirementDefinitionValidationException;
import kb.validation.exceptions.NodeMismatchValidationException;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.RequiredPropertyAttributeModel;
import kb.validation.exceptions.models.ValidationModel;
import kb.validation.required.RequiredPropertyValidation;

public class DSLMappingService {

	public KB kb;
	public ValueFactory factory;

	public Model aadmModel;

	public ModelBuilder aadmBuilder;
	public ModelBuilder templateBuilder;

	IRI aadmContainer;

	String base = "https://www.sodalite.eu/ontologies/";
//	String ws = base + "workspace/woq2a8g0gscuqos88bn2p7rvlq3/";
//	String ws = "http://";
	String ws = base + "workspace/1/";
	String namespacews = base + "workspace/1/";
	
	IRI aadmKB;
	String aadmURI;
	String aadmDSL;
	IRI namespace;
	String name;
	
	boolean complete;
	
	String currentTemplate;
	List<String> namespacesOfType = new ArrayList<String>();;
	

	// Validation
	Set<String> definedPropertiesForValidation = new HashSet<String>(),
			definedAttributesForValidation = new HashSet<String>();


	List<ValidationModel> validationModels = new ArrayList<>();
	List<ValidationModel> modifiedModels = new ArrayList<>();
	List<ValidationModel> suggestedModels = new ArrayList<>();
	List<DslValidationModel>  mappingModels =  new ArrayList<>();
	
	Set<String> templateNames = new HashSet<>();
	

	public DSLMappingService(KB kb, String aadmTTL, String aadmURI, boolean complete, String namespace, String aadmDSL, String name)
			throws RDFParseException, UnsupportedRDFormatException, IOException {
		super();
		this.kb = kb;
		this.factory = SimpleValueFactory.getInstance();

		aadmBuilder = new ModelBuilder();
		aadmBuilder.setNamespace("soda", KB.SODA).setNamespace("dul", KB.DUL).setNamespace("dcterms", KB.DCTERMS)
				.setNamespace("exchange", KB.EXCHANGE).setNamespace("tosca", KB.TOSCA);
		
		templateBuilder = new ModelBuilder();
		templateBuilder.setNamespace("soda", KB.SODA).setNamespace("dul", KB.DUL).setNamespace("dcterms", KB.DCTERMS)
				.setNamespace("exchange", KB.EXCHANGE).setNamespace("tosca", KB.TOSCA);

		//System.out.println(aadmTTL);
		InputStream targetStream = IOUtils.toInputStream(aadmTTL, Charset.defaultCharset());
		aadmModel = Rio.parse(targetStream, "", RDFFormat.TURTLE);
		targetStream.close();

		this.aadmURI = aadmURI;
	//	context = kb.factory.createIRI("http://" + submissionId);
		//ws += MyUtils.randomString() + "/";
		this.complete = complete;
		
		if (!"".equals(namespace))
			this.namespace = factory.createIRI(ws + namespace + "/");
		else
			this.namespace = factory.createIRI(ws + "global/");
		
		this.aadmDSL = aadmDSL;
		this.name = name;
	}

	public void retrieveLocalTemplates() throws MappingException {
		for (Resource _template : aadmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "Template"))
				.subjects()) {
			IRI template = (IRI) _template;

			Optional<Literal> templateName = Models
					.objectLiteral(aadmModel.filter(template, factory.createIRI(KB.EXCHANGE + "name"), null));
			
			if (templateName.isPresent()) {
				templateNames.add(templateName.get().getLabel());
			}
		}
	}
	
	public IRI start() throws MappingException, ValidationException  {

		// AADM
		aadmKB = null;

		for (Resource _aadm : aadmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "AADM")).subjects()) {
			Optional<Literal> _userId = Models
					.objectLiteral(aadmModel.filter(_aadm, factory.createIRI(KB.EXCHANGE + "userId"), null));
			
			String userId = null;
			if (!_userId.isPresent())
				mappingModels.add(new MappingValidationModel("AADM", "", "No 'userId' defined for AADM"));
			else
				userId = _userId.get().getLabel();
					
			ws += (aadmURI.isEmpty())? MyUtils.randomString() + "/" : MyUtils.getStringPattern(aadmURI, ".*/(.*)/AADM_.*") + "/";
			System.out.println("namespace = " + ws);
			aadmBuilder.setNamespace("ws", ws);

			aadmKB = (aadmURI.isEmpty()) ? factory.createIRI(ws + "AADM_" + MyUtils.randomString()) : factory.createIRI(aadmURI);
			//context = aadmKB;
			aadmBuilder.add(aadmKB, RDF.TYPE, "soda:AbstractApplicationDeployment");

			if (userId != null) {
				IRI user = factory.createIRI(ws + userId);
				aadmBuilder.add(user, RDF.TYPE, "soda:User");
				aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "createdBy"), user);
			}
			aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "createdAt"), DateTime.now());
			
			if ("".equals(aadmDSL)) {
				mappingModels.add(new MappingValidationModel("AADM", "aadmDSL", "No 'DSL' defined for the aadm model"));
			} else {
				aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "hasDSL"), aadmDSL);
			}
			
			aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "hasName"), name);
			
			break;
		}

		if (aadmKB == null) {
			mappingModels.add(new MappingValidationModel("AADM", "", "No AADM container found."));
		}

		retrieveLocalTemplates();	
		
		// TEMPLATES
		for (Resource _template : aadmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "Template"))
				.subjects()) {
			IRI template = (IRI) _template;

			Optional<Literal> _templateName = Models
					.objectLiteral(aadmModel.filter(template, factory.createIRI(KB.EXCHANGE + "name"), null));
			String templateName = null;
			
			if (!_templateName.isPresent())
				mappingModels.add(new MappingValidationModel("", template.getLocalName(), "No 'name' defined for template: "));
			else 
				templateName = _templateName.get().getLabel();
			
			currentTemplate = templateName;
			
			Optional<Literal> _templateType = Models
					.objectLiteral(aadmModel.filter(template, factory.createIRI(KB.EXCHANGE + "type"), null));
			String templateType = null;
			
			//errors like this are syntactic errors - prevented from ide
			if (!_templateType.isPresent()) {
				mappingModels.add(new MappingValidationModel("type", currentTemplate, "No 'type' defined for template: "));
				throw new MappingException(mappingModels);
			}
			else
				templateType = _templateType.get().getLabel();
			
			System.out.println(String.format("Name: %s, type: %s", templateName, templateType));
			
			NamedResource n = GetResources.setNamedResource(namespacews, templateType);
			//this.namespaceOfType = n.getNamespace();
			
			templateType = n.getResource();
			String resourceIRI = n.getResourceURI() ;
			if (resourceIRI != null)
				namespacesOfType = GetResources.getInheritedNamespacesFromType(kb, resourceIRI);
			
			System.out.println("namespaceOfType=" + this.namespacesOfType + ", templateType=" + templateType);
			
			IRI templateDescriptionKB = null;
			// add template to the aadm container instance
			if (templateName != null && templateType != null) {
				IRI templateKB = factory.createIRI(namespace + templateName); // this will be always new
				templateBuilder.add(templateKB, factory.createIRI(KB.SODA + "hasName"), templateName);
				
				IRI kbNodeType = GetResources.getKBNodeType(n, "tosca:tosca.entity.Root", kb);

				if (kbNodeType == null) {
					mappingModels.add(new MappingValidationModel(templateName, templateType, "'type' not found "));
				} else {
					templateBuilder.add(templateKB, RDF.TYPE, kbNodeType);
				}
				
				if (aadmKB != null) 
					aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "includesTemplate"), templateKB);

				// create description
				templateDescriptionKB = factory.createIRI(namespace + "Desc_" + MyUtils.randomString());
				templateBuilder.add(templateDescriptionKB, RDF.TYPE, "soda:SodaliteDescription");
				templateBuilder.add(templateKB, factory.createIRI(KB.SODA + "hasContext"), templateDescriptionKB);
			}
			// properties
			Set<Resource> _properties = Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "properties"));
			definedPropertiesForValidation.clear();
			for (Resource _property : _properties) {
				IRI property = (IRI) _property;
				IRI propertyClassifierKB = createPropertyOrAttributeKBModel(property);

				// add property classifiers to the template context
				if (templateDescriptionKB != null)
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "properties"), propertyClassifierKB);
			}
			// validation
			RequiredPropertyValidation v = new RequiredPropertyValidation(templateName,
					factory.createLiteral(templateType), definedPropertiesForValidation, kb);
			validationModels.addAll(v.validate());

			// attributes
			definedAttributesForValidation.clear();
			for (Resource _attribute : Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "attributes"))) {
				IRI attribute = (IRI) _attribute;
				IRI attributesClassifierKB = createPropertyOrAttributeKBModel(attribute);
				if (templateDescriptionKB != null)
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "attributes"), attributesClassifierKB);
			}

			// requirements
			for (Resource _requirement : Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "requirements"))) {
				IRI requirement = (IRI) _requirement;
				IRI requirementClassifierKB = createRequirementKBModel(requirement);

				// add attribute classifiers to the template context
				if (templateDescriptionKB != null)
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "requirements"),
							requirementClassifierKB);

			}

			// capabilities
			for (Resource _capability : Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "capabilities"))) {
				IRI capability = (IRI) _capability;
				IRI capabilityClassifierKB = createCapabilityKBModel(capability);

				// add attribute classifiers to the template context
				if (templateDescriptionKB != null)
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "capabilities"),
							capabilityClassifierKB);
			
			}
			
			// optimizations
			Optional<String> _optimizations = Models.getPropertyString(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "optimization"));

			if (_optimizations.isPresent()) {
				if (templateDescriptionKB != null)
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "optimization"),
							_optimizations.get());
			}
			
			// triggers
			for (Resource _trigger : Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "triggers"))) {
				IRI trigger = (IRI) _trigger;
				IRI triggerClassifierKB = createTriggerKBModel(trigger);
				// add property classifiers to the template context
				if (templateDescriptionKB != null)
					aadmBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "triggers"), triggerClassifierKB);
			}
			
			// targets
			Optional<Resource> _targets = Models.getPropertyResource(aadmModel, _template,
								factory.createIRI(KB.EXCHANGE + "targets"));
						
			if (!_targets.isEmpty()) {
				IRI targetClassifierKB = createTargetKBModel((IRI)_targets.get());
				if (templateDescriptionKB != null)	
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "targets"),
								targetClassifierKB);
			}

		}

		// Inputs
		Set<Resource> inputs = aadmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "Input")).subjects();
		if (inputs.size() > 0) {
			IRI inputKB = factory.createIRI(namespace + "topology_template_inputs_" + MyUtils.randomString());
			templateBuilder.add(inputKB, RDF.TYPE, factory.createIRI(KB.TOSCA + "Input"));
			if (aadmKB!=null)
				aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "includesInput"), inputKB);

			// create description
			IRI inputDescriptionKB = factory.createIRI(namespace + "Desc_" + MyUtils.randomString());
			templateBuilder.add(inputDescriptionKB, RDF.TYPE, "soda:SodaliteDescription");
			templateBuilder.add(inputKB, factory.createIRI(KB.SODA + "hasContext"), inputDescriptionKB);
			templateBuilder.add(inputKB, factory.createIRI(KB.SODA + "hasName"), "topology_template_inputs");
			
			for (Resource _input : inputs) {
				IRI input = (IRI) _input;
				Optional<Literal> _inputName = Models
						.objectLiteral(aadmModel.filter(input, factory.createIRI(KB.EXCHANGE + "name"), null));
				
				String inputName = null;
				if (!_inputName.isPresent())
					mappingModels.add(new MappingValidationModel(currentTemplate, input.getLocalName(), "No 'name' defined for input"));
				else {
					inputName = _inputName.get().getLabel();
					System.out.println(String.format("Input name: %s", inputName));
				}

				// for each tosca input we need to have a Feature
				IRI inputFeatureKB = factory.createIRI(namespace + "Input_" + MyUtils.randomString());
				templateBuilder.add(inputFeatureKB, RDF.TYPE, "tosca:Feature");

				if (inputName != null) {
					IRI kbProperty = GetResources.getKBProperty(inputName, this.namespacesOfType, kb);
					if (kbProperty == null) {
						kbProperty = factory.createIRI(namespace + inputName);
						templateBuilder.add(kbProperty, RDF.TYPE, "rdf:Property");
					}
					templateBuilder.add(inputFeatureKB, factory.createIRI(KB.DUL + "classifies"), kbProperty);
				}
				templateBuilder.add(inputDescriptionKB, factory.createIRI(KB.TOSCA + "input"), inputFeatureKB);

				Set<Resource> _parameters = Models.getPropertyResources(aadmModel, _input,
						factory.createIRI(KB.EXCHANGE + "hasParameter"));

				// TODO: HERE WE NEED TO IMPLEMENT RECURSION
				for (Resource _parameter : _parameters) {
					IRI parameter = (IRI) _parameter;
					IRI propertyClassifierKB = createPropertyOrAttributeKBModel(parameter);

					templateBuilder.add(inputFeatureKB, factory.createIRI(KB.DUL + "hasParameter"), propertyClassifierKB);
				}

			}
		}

		
		System.err.println("Mapping error models:");

		for (DslValidationModel m:mappingModels) {
			System.err.println(m.toString());
		}
		
		if (!mappingModels.isEmpty()) {
			throw new MappingException(mappingModels);
		}
		if (!validationModels.isEmpty()) {
			throw new ValidationException(validationModels);
		}
		
		try {
			VerifySingularity.removeExistingDefinitions(kb, templateNames, namespace.toString());
			if (!aadmURI.isEmpty())
				VerifySingularity.removeInputs(kb, aadmURI);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return aadmKB;

	}

	private IRI createRequirementKBModel(IRI requirement) throws MappingException {
		Optional<Literal> _requirementName = Models
				.objectLiteral(aadmModel.filter(requirement, factory.createIRI(KB.EXCHANGE + "name"), null));
		
		String requirementName = null;
		if (!_requirementName.isPresent())
			mappingModels.add(new MappingValidationModel(currentTemplate, requirement.getLocalName(), "No 'name' defined for requirement"));
		else
			requirementName = _requirementName.get().getLabel();

		// create classifier
		IRI requirementClassifierKB = factory.createIRI(namespace + "ReqClassifier_" + MyUtils.randomString());
		templateBuilder.add(requirementClassifierKB, RDF.TYPE, "tosca:Requirement");

		IRI requirementProperty = null;
		if (requirementName != null) {
			requirementProperty = GetResources.getKBProperty(requirementName, this.namespacesOfType, kb);
			if (requirementProperty == null) {
				mappingModels.add(new MappingValidationModel(currentTemplate, requirementName, "Cannot find requirement property"));
			}
		}
		
		if (requirementProperty != null)
			templateBuilder.add(requirementClassifierKB, factory.createIRI(KB.DUL + "classifies"), requirementProperty);

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(aadmModel.filter(requirement, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			NamedResource n = GetResources.setNamedResource(namespacews, value.getLabel());			
			IRI kbTemplate = getKBTemplate(n);
			if (kbTemplate == null) {
				if (templateNames.contains(n.getResource()))
					kbTemplate = factory.createIRI(namespace + n.getResource());
				else
					mappingModels.add(new MappingValidationModel(currentTemplate, requirement.getLocalName(), "Cannot find Template: " + value.getLabel()));
			}
			if (kbTemplate != null)
				templateBuilder.add(requirementClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbTemplate);
		} else {

			IRI root = createParameterKBModel(requirement);
			if (root != null)
				templateBuilder.add(requirementClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), root);
		}

		return requirementClassifierKB;

	}

	private IRI createParameterKBModel(IRI requirement) throws MappingException {
		IRI parameterClassifierKB = null;
		
		Optional<Resource> _parameter = Models.getPropertyResource(aadmModel, requirement,
				factory.createIRI(KB.EXCHANGE + "hasParameter"));
		IRI parameter = null;
		if (!_parameter.isPresent()) {
			mappingModels.add(new MappingValidationModel(currentTemplate, requirement.getLocalName(), "Cannot find requirement parameter"));
		} else {
			parameter = (IRI) _parameter.get();
		

			String parameterName = null;
			Optional<Literal> _parameterName = Models
					.objectLiteral(aadmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null));
			if (!_parameter.isPresent()) {
				mappingModels.add(new MappingValidationModel(currentTemplate, parameter.getLocalName() , "No 'name' defined for requirement parameter"));
			} else {
				parameterName = _parameterName.get().getLabel();
			}
		
			// create classifier
			parameterClassifierKB = factory.createIRI(namespace + "ParamClassifier_" + MyUtils.randomString());
			templateBuilder.add(parameterClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
			if (parameterName != null) {
				IRI paramProperty = GetResources.getKBProperty(parameterName, this.namespacesOfType, kb);
				if (paramProperty == null) 
					mappingModels.add(new MappingValidationModel(currentTemplate, parameterName , "Cannot find requirement parameter"));
				else 
					templateBuilder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "classifies"), paramProperty);
			}
			
			// check for direct values of parameters
			Literal value = Models
					.objectLiteral(aadmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "value"), null))
					.orElse(null);

			if (value != null) { // this means there is no parameters
				NamedResource n = GetResources.setNamedResource(namespacews, value.getLabel());
				IRI kbTemplate = getKBTemplate(n);
				if (kbTemplate == null) {
					if (templateNames.contains(n.getResource()))
						kbTemplate = factory.createIRI(namespace + n.getResource());
					else
						mappingModels.add(new MappingValidationModel(currentTemplate, requirement.getLocalName(), "Cannot find Template: " + value.getLabel()));
				}
				if (kbTemplate != null)
					templateBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbTemplate);
			} else {

				IRI root = createParameterKBModel(parameter);
				templateBuilder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), root);

			}
		}
		return parameterClassifierKB;
	}

	private IRI createPropertyOrAttributeKBModel(IRI exchangeParameter) throws MappingException {
		Optional<Literal> _propertyName = Models
				.objectLiteral(aadmModel.filter(exchangeParameter, factory.createIRI(KB.EXCHANGE + "name"), null));

		String propertyName = null;
		if (!_propertyName.isPresent())
			mappingModels.add(new MappingValidationModel(currentTemplate, exchangeParameter.getLocalName(), "No 'name' defined for property"));
		else {
			propertyName = _propertyName.get().getLabel();
		}
//		Optional<Literal> _value = Models
//				.objectLiteral(aadmModel.filter(exchangeParameter, factory.createIRI(KB.EXCHANGE + "value"), null));

		Set<String> _values = Models.getPropertyStrings(aadmModel, exchangeParameter,
				factory.createIRI(KB.EXCHANGE + "value"));

		Set<String> listValues = Models.getPropertyStrings(aadmModel, exchangeParameter,
				factory.createIRI(KB.EXCHANGE + "listValue"));

		System.err.println("------------------" + _values);
		System.err.println("-----ListValues---" + listValues);

		if (_values.isEmpty() && listValues.isEmpty()) {
			System.err.println("No value found for property: " + exchangeParameter.getLocalName());
		}

//		String value = _value.isPresent() ? _value.get().stringValue() : null;
		if (propertyName != null)
			definedPropertiesForValidation.add(propertyName);

		System.out.println(String.format("Property name: %s, value: %s", propertyName, _values));
		
		Optional<Resource> _parameterType  = Models.getPropertyResource(aadmModel, exchangeParameter,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String parameterType = MyUtils.getStringValue(_parameterType.get());
		System.out.println("parameterType = " + parameterType);
		
		IRI propertyClassifierKB = null;
		switch (parameterType) {
			case "Attribute":
				propertyClassifierKB = factory.createIRI(namespace + "AttrClassifer_" + MyUtils.randomString());
				templateBuilder.add(propertyClassifierKB, RDF.TYPE, "tosca:Attribute");
				break;
			case "Parameter":
				propertyClassifierKB = factory.createIRI(namespace + "ParamClassifer_" + MyUtils.randomString());
				templateBuilder.add(propertyClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			case "Property" :
				propertyClassifierKB = factory.createIRI(namespace + "PropClassifer_" + MyUtils.randomString());
				templateBuilder.add(propertyClassifierKB, RDF.TYPE, "tosca:Property");
				break;
			default:
				System.err.println("parameterType = " + parameterType + " does not exist");
		}

		// create rdf:property
		if (propertyName != null) {
			//maybe local namespace should be passed in type
			IRI kbProperty = GetResources.getKBProperty(propertyName, this.namespacesOfType, kb);
			if (kbProperty == null) {
				kbProperty = factory.createIRI(namespace + propertyName);
				templateBuilder.add(kbProperty, RDF.TYPE, "rdf:Property");
			}
			templateBuilder.add(propertyClassifierKB, factory.createIRI(KB.DUL + "classifies"), kbProperty);
		}
		
		if (!_values.isEmpty()) {

			if (_values.size() == 1) {
				Object i = null;
				String value = _values.iterator().next();
				if ((i = Ints.tryParse(value)) != null) {
					templateBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				} else if ((i = BooleanUtils.toBooleanObject(value)) != null) {
					templateBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (boolean) i);
				} else
					templateBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
			} else {
				IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
				for (String string : _values) {
					Object i = null;
					if ((i = Ints.tryParse(string)) != null) {
						templateBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
					} else if ((i = BooleanUtils.toBooleanObject(string)) != null) {
						templateBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (boolean) i);
					} else
						templateBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), string);
				}
				templateBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
			}

		} else if (!listValues.isEmpty()) {
			System.err.println("*****************************} else if (!listValues.isEmpty()) {");
			IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
			for (String string : listValues) {
				Object i = null;
				if ((i = Ints.tryParse(string)) != null) {
					templateBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				} else if ((i = BooleanUtils.toBooleanObject(string)) != null) {
					templateBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), (boolean) i);
				} else
					templateBuilder.add(list, factory.createIRI(KB.TOSCA + "hasDataValue"), string);
			}
			templateBuilder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
		} else {
			Set<Resource> _parameters = Models.getPropertyResources(aadmModel, exchangeParameter,
					factory.createIRI(KB.EXCHANGE + "hasParameter"));

			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createPropertyOrAttributeKBModel(parameter);

				templateBuilder.add(propertyClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), _p);
			}

//			IRI root = createPropertyOrAttributeKBModel(exchangeParameter);
//			builder.add(propertyClassifierKB, factory.createIRI("dul:hasParameter"), root);
		}

		return propertyClassifierKB;

	}
	
	private IRI createCapabilityKBModel(IRI capability) throws MappingException {
		String capabilityName = Models
				.objectLiteral(aadmModel.filter(capability, factory.createIRI(KB.EXCHANGE + "name"), null))
				.orElseThrow(
						() -> new MappingException("No 'name' defined for capability: " + capability.getLocalName()))
				.stringValue();

		// create classifier
		IRI capabilityClassifierKB = factory.createIRI(namespace + "CapClassifier_" + MyUtils.randomString());
		templateBuilder.add(capabilityClassifierKB, RDF.TYPE, "tosca:Capability");

		IRI capabilityProperty = GetResources.getKBProperty(capabilityName, this.namespacesOfType, kb);
		if (capabilityProperty == null) {
			throw new MappingException("Cannot find capability property: " + capabilityName);
		}
		templateBuilder.add(capabilityClassifierKB, factory.createIRI(KB.DUL + "classifies"), capabilityProperty);

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(aadmModel.filter(capability, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			NamedResource n = GetResources.setNamedResource(namespacews, value.getLabel());
			IRI kbTemplate = getKBTemplate(n);
			if (kbTemplate == null) {
				if (templateNames.contains(n.getResource()))
					kbTemplate = factory.createIRI(namespace + n.getResource());
				else
					mappingModels.add(new MappingValidationModel(currentTemplate, capability.getLocalName(), "Cannot find Template: " + value.getLabel()));
			}
			if (kbTemplate != null)
				templateBuilder.add(capabilityClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbTemplate);
		} else {
			Set<Resource> _properties = Models.getPropertyResources(aadmModel, capability,
			factory.createIRI(KB.EXCHANGE + "properties"));
			//definedPropertiesForValidation.clear();
			if (_properties.isEmpty()) {
				IRI root = createParameterKBModel(capability);
				templateBuilder.add(capabilityClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), root);
			} else {
				for (Resource _property : _properties) {
					IRI property = (IRI) _property;
					IRI propertyClassifierKB = createPropertyOrAttributeKBModel(property);

					// add property classifiers to the template context
					templateBuilder.add(capabilityClassifierKB, factory.createIRI(KB.TOSCA + "properties"), propertyClassifierKB);
				}
			}
		}

		return capabilityClassifierKB;

	}
	
	private IRI createTriggerKBModel(IRI trigger_iri) throws MappingException {
		Optional<Literal> _triggerName = Models
				.objectLiteral(aadmModel.filter(trigger_iri, factory.createIRI(KB.EXCHANGE + "name"), null));
		
		String triggerName = null;
		if (!_triggerName.isPresent())
			mappingModels.add(new MappingValidationModel(currentTemplate, trigger_iri.getLocalName(), "No 'name' defined for interface"));
		else 
			triggerName = _triggerName.get().getLabel();
		
		IRI interfaceProperty = null;
		if (triggerName != null) {
			interfaceProperty = GetResources.getKBProperty(triggerName, this.namespacesOfType, kb);
			if (interfaceProperty == null || interfaceProperty.toString().equals(namespace + triggerName)) {
				interfaceProperty = factory.createIRI(namespace + triggerName);
				aadmBuilder.add(interfaceProperty, RDF.TYPE, "rdf:Property");
			}
		}
		
		Optional<Resource> _type  = Models.getPropertyResource(aadmModel, trigger_iri,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String type = MyUtils.getStringValue(_type.get());
		
		IRI triggerClassifierKB = null;
		switch (type) {
			case "Trigger":
				triggerClassifierKB = factory.createIRI(namespace + "TriggerClassifer_" + MyUtils.randomString());
				aadmBuilder.add(triggerClassifierKB, RDF.TYPE, "tosca:Trigger");
				break;
			case "Parameter":
				triggerClassifierKB = factory.createIRI(namespace + "ParamClassifer_" + MyUtils.randomString());
				aadmBuilder.add(triggerClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			default:
				System.err.println("type = " + type + " does not exist");
		}
		
		if (interfaceProperty != null)
			aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.DUL + "classifies"), interfaceProperty);
		
		Optional<String> description = Models.getPropertyString(aadmModel, trigger_iri,
				factory.createIRI(KB.EXCHANGE + "description"));
		if (description.isPresent())
			aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.DCTERMS + "description"), description.get());

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(aadmModel.filter(trigger_iri, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			if (triggerName != null && (triggerName.equals("node") || triggerName.equals("requirement") || triggerName.equals("capability"))) {
				NamedResource n = GetResources.setNamedResource(namespacews, value.getLabel());
				IRI kbNode = getKBTemplate(n);
				if (kbNode == null) {
					if (templateNames.contains(n.getResource()))
						kbNode = factory.createIRI(namespace + n.getResource());
					else {
						mappingModels.add(new MappingValidationModel(currentTemplate, trigger_iri.getLocalName(), "Cannot find Template: " + value.getLabel() + " for trigger = " + triggerName));
						System.err.println(currentTemplate + ": Cannot find template: " + value.getLabel() + " for trigger " + triggerName);
					}
				}
				if(kbNode != null)
					aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} else {
				Object i = null;
				if ((i = Ints.tryParse(value.toString())) != null)
					aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				else 
					aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
			}
		} else {
			Set<Resource> _parameters = Models.getPropertyResources(aadmModel, trigger_iri,
					factory.createIRI(KB.EXCHANGE + "hasParameter"));
			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createTriggerKBModel(parameter);
				aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.DUL + "hasParameter"), _p);
			}
		}
		return triggerClassifierKB;
	}

	private Object mapValue(String value) {
		Object i = null;
		if ((i = Ints.tryParse(value)) != null) {
			return (int) i;
		} else if ((i = BooleanUtils.toBooleanObject(value)) != null) {
			return (boolean) i;
		} else
			return value;
	}

	private IRI createTargetKBModel(IRI parameter) throws MappingException {
		System.out.println("createTargetKBModel:" + parameter);
		Set<Literal> listValues= Models.objectLiterals(aadmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "listValue"), null));
		
		System.err.println("-----ListValue---" + listValues);
		IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
		
		IRI parameterClassifierKB = factory.createIRI(namespace + "ParamClassifer_" + MyUtils.randomString());
		
		aadmBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
		aadmBuilder.add(list, RDF.TYPE, "tosca:List");
		
		for (Literal l:listValues) {
			aadmBuilder.add(parameterClassifierKB, RDF.TYPE, "tosca:Target");
			aadmBuilder.add(list, RDF.TYPE, "tosca:List");
			
			
			NamedResource n = GetResources.setNamedResource(namespacews, l.getLabel());
			IRI kbNode = getKBTemplate(n);
			if (kbNode == null) {
				if (templateNames.contains(n.getResource()))
					kbNode = factory.createIRI(namespace + n.getResource());
				else {
					mappingModels.add(new MappingValidationModel(currentTemplate, "targets", "Cannot find target: " + l.getLabel()));
					System.err.println(currentTemplate + ": Cannot find Node: " + l.getLabel());
				}
			}
			if(kbNode != null)
				aadmBuilder.add(list, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
		}
		
		return parameterClassifierKB;		
	}

	private IRI getKBTemplate(NamedResource n) {
		System.out.println("getKBTemplate label = " + n.getResource() + ", namespace = " + n.getNamespace());
		String namespace = n.getNamespace();
		String resource = n.getResource();
		String sparql = "select distinct ?x { \r\n" + 
						"   ?m DUL:isSettingFor ?x .\r\n" + 
						"	{        \r\n" + 
						"		?x rdf:type ?type .\r\n" + 
						"         FILTER NOT EXISTS\r\n" + 
						"         { \r\n" + 
						"           GRAPH ?g { ?x ?p ?o } \r\n" + 
						"         }\r\n" + 
						"	} ";
		if (namespace != null && !namespace.contains("global"))
			sparql +=	"     UNION {\r\n" + 
						"        GRAPH " + "<"+ namespace + ">\r\n" +
						"         {\r\n" +
						"			?x soda:hasName ?name .\r\n" + 
						"		   }\r\n" + 
						"     }\r\n";
		
		sparql += 	" FILTER (strends(str(?x), \"" + resource + "\")). " +
					"}";
		
		
		System.out.println(sparql);
		String query = KB.PREFIXES + sparql;

		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query);

		Set<IRI> xSet = new HashSet<IRI>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			xSet.add((IRI) bindingSet.getBinding("x").getValue());
		}
		result.close();
		
		IRI x = null;
		if (!xSet.isEmpty()) 
			x = xSet.iterator().next();
		
		if(xSet.size() > 1) {
			for (IRI t: xSet) {
				//property of a named namespace overrides
				if (!t.toString().contains("global"))
					x = t;
			}
		}
			
		return x;
	}
	
	
	public IRI getNamespace() {
		return this.namespace;
	}
	
	public Set<String> getTemplateNames() {
		return this.templateNames;
	}
	
	public Set<IRI> getTemplatesIRIs(IRI namespace, String resourceName) {
		Set<IRI> templatesIRIs = new HashSet<>();
		for (String t: this.templateNames) {
			templatesIRIs.add(kb.factory.createIRI(namespace + t));
		}
		
		return templatesIRIs;
	}
	
	public List<ValidationModel> getModifiedModels() {
		return modifiedModels;
	}
	
	public List<ValidationModel> getSuggestedModels() {
		return suggestedModels;
	}
	
	public void shutDown() {
		System.out.println("shutting down");
		if (kb != null) {
			kb.shutDown();
		}
	}

	public void save() throws ValidationException, IOException {
		Model amodel = aadmBuilder.build();
		Model tmodel = templateBuilder.build();
		String aadmId = MyUtils.getStringPattern(this.aadmKB.stringValue(), ".*/(AADM_.*).*");
		//Requirement first check about existence, and (complete = true) update models if matching nodes found
		IRI context = namespace.toString().contains("global") ? null : namespace;
		RequirementExistenceValidation r = new RequirementExistenceValidation(aadmId, complete, kb, namespace.toString(), context);
		//Check for required omitted requirements
		validationModels.addAll(r.validate());
		if (!validationModels.isEmpty()) {
			Set<IRI> templatesIRIs = MyUtils.getResourceIRIs(this.kb, this.namespace, this.templateNames);
			new ModifyKB(kb).deleteNodes(templatesIRIs);
			throw new ValidationException(validationModels);
		}
		
		suggestedModels.addAll(r.getSuggestions());
		modifiedModels.addAll(r.getModifiedModels());
		
		//Sommelier validations
		//ValidationService v = new ValidationService(MyUtils.getStringPattern(this.aadmKB.stringValue(), ".*/(AADM_.*).*"));
		/*validationModels.addAll(v.validate());
		if (!validationModels.isEmpty()) {
			kb.connection.clear(context);
			throw new ValidationException(validationModels);
		}*/
		
		kb.connection.add(amodel);
		if (namespace.toString().contains("global"))
			kb.connection.add(tmodel);
		else
			kb.connection.add(tmodel,namespace);
	}

	public static void main(String[] args)
			throws RDFParseException, UnsupportedRDFormatException, IOException, Exception {
		String aadmTTL = MyUtils.fileToString("dsl/ide_snow_v3.ttl");

		KB kb = new KB("TOSCA_automated");
		DSLMappingService m = new DSLMappingService(kb, aadmTTL,"test", false, "", "", "");
		m.start();
		m.save();
		m.shutDown();

	}

}
