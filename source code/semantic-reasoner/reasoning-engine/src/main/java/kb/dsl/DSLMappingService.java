package kb.dsl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import kb.KBApi;
import kb.clean.ModifyKB;
import kb.dsl.exceptions.MappingException;
import kb.dsl.exceptions.models.DslValidationModel;
import kb.dsl.exceptions.models.MappingValidationModel;
import kb.dsl.utils.ErrorConsts;
import kb.dsl.utils.GetResources;
import kb.dsl.utils.NamedResource;
import kb.dsl.verify.singularity.VerifySingularity;
import kb.dto.PropertyMap;
import kb.repository.KB;
import kb.repository.KBConsts;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.RequirementExistenceValidation;
import kb.validation.RequirementValidation;
import kb.validation.ValidationService;
import kb.validation.constraints.ConstraintsPropertyValidation;
import kb.validation.exceptions.CapabilityMismatchValidationException;
import kb.validation.exceptions.NoRequirementDefinitionValidationException;
import kb.validation.exceptions.NodeMismatchValidationException;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.RequiredPropertyAttributeModel;
import kb.validation.exceptions.models.RequirementExistenceModel;
import kb.validation.exceptions.models.ValidationModel;
import kb.validation.required.RequiredPropertyValidation;

public class DSLMappingService {
	private static final Logger LOG = LoggerFactory.getLogger(DSLMappingService.class.getName());
	
	public KB kb;
	public ValueFactory factory;

	public Model aadmModel;

	public ModelBuilder aadmBuilder;
	public ModelBuilder templateBuilder;

	IRI aadmContainer;

	String base = "https://www.sodalite.eu/ontologies/";
//	String ws = base + "workspace/woq2a8g0gscuqos88bn2p7rvlq3/";
//	String ws = "http://";
	String aadmws = KB.BASE_NAMESPACE;
	String templatews = KB.BASE_NAMESPACE;
	
	public IRI aadmKB;
	public String aadmURI;
	public String aadmDSL;
	
	//e.g.docker or openstack
	public IRI namespace;
	public String name;
	
	boolean complete;
	
	private String currentTemplate;
	private IRI currentType;
	public List<String> namespacesOfType = new ArrayList<>();
	
	//for mapping errors, contains e.g. node_templates
	String currentPrefixTemplate;
	String subMappingPath = "";
	

	// Validation
	public Set<String> definedPropertiesForValidation = new HashSet<String>(),
			definedAttributesForValidation = new HashSet<String>();
	//for constraints
	public Map<String, String> propertyValuesForValidation = new HashMap<String, String>();
	
	//properties of type: map
	/*Example:
	* ports:  
	*  component_ports:
	*	port_range_max: 8081 */
	public List<PropertyMap> propertyMapValuesForValidation = new ArrayList<PropertyMap>();
	PropertyMap tempPropertyMap;
	String currentPropertyMap;
	
	//for requirements Sommelier validation;
	Set<HashMap<String, IRI>> templateRequirements = new HashSet<>();
	HashMap<IRI, IRI> templateTypes = new HashMap<>();
	HashMap<String, IRI> tempReq;

	public List<ValidationModel> validationModels = new ArrayList<>();
	public List<ValidationModel> modifiedModels = new ArrayList<>();
	public List<ValidationModel> suggestedModels = new ArrayList<>();
	public List<DslValidationModel>  mappingModels =  new ArrayList<>();
	
	public Set<String> templateNames = new HashSet<>();
	

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

		InputStream targetStream = IOUtils.toInputStream(aadmTTL, Charset.defaultCharset());
		aadmModel = Rio.parse(targetStream, "", RDFFormat.TURTLE);
		targetStream.close();

		this.aadmURI = aadmURI;
	//	context = kb.factory.createIRI("http://" + submissionId);
		//ws += MyUtils.randomString() + "/";
		this.complete = complete;
		
		if (!"".equals(namespace))
			this.namespace = factory.createIRI(templatews + namespace + "/");
		else
			this.namespace = factory.createIRI(templatews + "global/");
		
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
	
	public IRI start() throws MappingException, ValidationException, IOException  {

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
					
			aadmws += (aadmURI.isEmpty())? MyUtils.randomString() + "/" : MyUtils.getStringPattern(aadmURI, ".*/(.*)/AADM_.*") + "/";
			LOG.info("namespace = {}", aadmws);
			aadmBuilder.setNamespace("ws", aadmws);

			aadmKB = (aadmURI.isEmpty()) ? factory.createIRI(aadmws + "AADM_" + MyUtils.randomString()) : factory.createIRI(aadmURI);
			//context = aadmKB;
			aadmBuilder.add(aadmKB, RDF.TYPE, "soda:AbstractApplicationDeployment");

			if (userId != null) {
				IRI user = factory.createIRI(aadmws + userId);
				aadmBuilder.add(user, RDF.TYPE, "soda:User");
				aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "createdBy"), user);
			}
			aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "createdAt"), DateTime.now());
			
			if ("".equals(aadmDSL)) {
				//mappingModels.add(new MappingValidationModel("AADM", "aadmDSL", "No 'DSL' defined for the aadm model"));
				LOG.info("No 'DSL' defined for the rm model");
			} 
			aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "hasDSL"), aadmDSL);
			
			aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "hasName"), name);
			aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + "hasNamespace"), MyUtils.getNamespaceFromContext(namespace.toString()));
			
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
				mappingModels.add(new MappingValidationModel(currentPrefixTemplate + ErrorConsts.SLASH, template.getLocalName(), "No 'name' defined for template: "));
			else 
				templateName = _templateName.get().getLabel();
			
			currentTemplate = templateName;
			
			Optional<Literal> _templateType = Models
					.objectLiteral(aadmModel.filter(template, factory.createIRI(KB.EXCHANGE + "type"), null));
			String templateType = null;
			
			//errors like this are syntactic errors - prevented from ide
			if (!_templateType.isPresent()) {
				mappingModels.add(new MappingValidationModel(currentPrefixTemplate + ErrorConsts.SLASH + templateName, currentTemplate, "No 'type' defined for template: "));
				throw new MappingException(mappingModels);
			}
			else
				templateType = _templateType.get().getLabel();
			
			LOG.info("Name: {}, type: {}", templateName, templateType);
			
			NamedResource fullTemplateType = GetResources.setNamedResource(templatews, templateType, kb);
			//this.namespaceOfType = n.getNamespace();
			
			templateType = fullTemplateType.getResource();
			String resourceIRI = fullTemplateType.getResourceURI() ;
			LOG.info("resourceIRI: {}", resourceIRI);
			if (resourceIRI != null)
				namespacesOfType = GetResources.getInheritedNamespacesFromType(kb, resourceIRI);
			
			LOG.info("namespacesOfType={} that are inherited from templateType={}", this.namespacesOfType, templateType);
			
			IRI templateDescriptionKB = null;
			// add template to the aadm container instance
			if (templateName != null && templateType != null) {
				IRI templateKB = factory.createIRI(namespace + templateName); // this will be always new
				templateBuilder.add(templateKB, factory.createIRI(KB.SODA + "hasName"), templateName);
				
				String kindOfTtemplate = MyUtils.getStringPattern(template.getLocalName(), "([A-Za-z]+)_\\d+");
				currentPrefixTemplate = KBConsts.TEMPLATE_CLASSES.get(kindOfTtemplate);
				
				IRI kbNodeType = GetResources.getKBNodeType(fullTemplateType, "tosca:tosca.entity.Root", kb);

				if (kbNodeType == null) {
					mappingModels.add(new MappingValidationModel(currentPrefixTemplate + ErrorConsts.SLASH + templateName, templateType, "'type' not found "));
				} else {
					templateBuilder.add(templateKB, RDF.TYPE, kbNodeType);
					//needed for requirement Validation
			
					templateTypes.put(templateKB, kbNodeType);
					currentType = kbNodeType;
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
			propertyValuesForValidation.clear();
			propertyMapValuesForValidation.clear();
			for (Resource _property : _properties) {
				IRI property = (IRI) _property;
				IRI propertyClassifierKB = createPropertyOrAttributeKBModel(property);
				
				//clear context path needed for Mapping errors
				subMappingPath = "";

				// add property classifiers to the template context
				if (templateDescriptionKB != null)
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "properties"), propertyClassifierKB);
			}
			
			for (PropertyMap pm: propertyMapValuesForValidation) {
				LOG.info("propertyMapValuesForValidation: ");
				LOG.info("propertyName: {}", pm.getName());
				LOG.info("getMapProperties: {}", pm.getMapProperties());
			}
			
			// validation
			RequiredPropertyValidation v = new RequiredPropertyValidation(templateName,
					factory.createIRI(resourceIRI), definedPropertiesForValidation, kb);
			validationModels.addAll(v.validate());
			
			ConstraintsPropertyValidation c = new ConstraintsPropertyValidation(currentPrefixTemplate, templateName, fullTemplateType, propertyValuesForValidation, propertyMapValuesForValidation, kb);
			validationModels.addAll(c.validate());
			
			// attributes
			definedAttributesForValidation.clear();
			for (Resource _attribute : Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "attributes"))) {
				IRI attribute = (IRI) _attribute;
				IRI attributesClassifierKB = createPropertyOrAttributeKBModel(attribute);
				
				//clear context path needed for Mapping errors
				subMappingPath = "";
				
				if (templateDescriptionKB != null)
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "attributes"), attributesClassifierKB);
			}

			// requirements
			for (Resource _requirement : Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "requirements"))) {
				IRI requirement = (IRI) _requirement;
				IRI requirementClassifierKB = createRequirementKBModel(requirement);
				
				//clear context path needed for Mapping errors
				subMappingPath = "";

				// add attribute classifiers to the template context
				if (templateDescriptionKB != null)
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "requirements"),
							requirementClassifierKB);

				this.templateRequirements.add(tempReq);

			}

			// capabilities
			for (Resource _capability : Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "capabilities"))) {
				IRI capability = (IRI) _capability;
				IRI capabilityClassifierKB = createCapabilityKBModel(capability);
				
				//clear context path needed for Mapping errors
				subMappingPath = "";

				// add attribute classifiers to the template context
				if (templateDescriptionKB != null)
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "capabilities"),
							capabilityClassifierKB);
			
			}
			
			// optimizations
			Optional<String> _optimizations = Models.getPropertyString(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "optimization"));

			if (_optimizations.isPresent() && templateDescriptionKB != null) {
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "optimization"),
							_optimizations.get());
			}
			
			// triggers
			for (Resource _trigger : Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "triggers"))) {
				IRI trigger = (IRI) _trigger;
				IRI triggerClassifierKB = createTriggerKBModel(trigger);
				
				//clear context path needed for Mapping errors
				subMappingPath = "";
				// add property classifiers to the template context
				if (templateDescriptionKB != null)
					aadmBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "triggers"), triggerClassifierKB);
			}
			
			// targets
			Optional<Resource> _targets = Models.getPropertyResource(aadmModel, _template,
								factory.createIRI(KB.EXCHANGE + "targets"));
						
			if (!_targets.isEmpty()) {
				IRI targetClassifierKB = createTargetKBModel((IRI)_targets.get());
				//clear context path needed for Mapping errors
				subMappingPath = "";
				if (templateDescriptionKB != null)	
					templateBuilder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "targets"),
								targetClassifierKB);
			}

		}

		// Inputs
		createInputOutputKBModel(KBConsts.IS_INPUT);
		//clear context path needed for Mapping errors
		subMappingPath = "";
		// Outputs
		createInputOutputKBModel(KBConsts.IS_OUTPUT);
		
		
		LOG.info("Mapping error models:");

		for (DslValidationModel m:mappingModels) {
			LOG.info(m.toString());
		}
		
		if (!mappingModels.isEmpty()) {
			throw new MappingException(mappingModels);
		}
		
		LOG.info("templateRequirements");
		for (HashMap<String, IRI> t: templateRequirements) {
			LOG.info("template = {}", t.get("template"));
			LOG.info("type = {}", t.get("templateType"));
			LOG.info("r_a = {}", t.get("r_a"));
			LOG.info("node = {}", t.get("node"));
			LOG.info("capability = {}", t.get("capability"));
		}
		
		
		//Sommelier validations
		ValidationService v = new ValidationService(MyUtils.getStringPattern(this.aadmKB.stringValue(), ".*/(AADM_.*).*"), this.templateRequirements, this.templateTypes, kb);
		validationModels.addAll(v.validate());
				
		if (!validationModels.isEmpty()) {
			throw new ValidationException(validationModels);
		}
		
		try {
			
			//THIS SHOULD BE CORRECTED
			if (!aadmURI.isEmpty()) {
				//VerifySingularity.removeInputs(kb, aadmURI);
				KBApi api = new KBApi(kb);
				//deleteModel also deletes the inputs
				api.deleteModel(aadmURI);
			}
			VerifySingularity.removeExistingDefinitions(kb, templateNames, namespace.toString(), aadmKB);
		} catch (IOException e) {
			LOG.error(e.getMessage(),e);
		}
		
		return aadmKB;

	}

	private IRI createRequirementKBModel(IRI requirement) throws MappingException {
		tempReq = new HashMap<String, IRI>();
		Optional<Literal> _requirementName = Models
				.objectLiteral(aadmModel.filter(requirement, factory.createIRI(KB.EXCHANGE + "name"), null));
		
		String requirementName = null;
		if (!_requirementName.isPresent())
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS), requirement.getLocalName(), "No 'name' defined for requirement"));
		else {
			requirementName = _requirementName.get().getLabel();
			subMappingPath += ErrorConsts.SLASH + requirementName;
		}
		
		tempReq.put("template", kb.factory.createIRI(this.templatews + this.currentTemplate));
		tempReq.put("templateType", this.currentType);
		//base IRI has been added only because other type is not permitted. It is converted to String in RequirementValidation
		tempReq.put("kindOfTemplate", kb.factory.createIRI(KB.BASE_NAMESPACE + this.currentPrefixTemplate));

		// create classifier
		IRI requirementClassifierKB = factory.createIRI(namespace + "ReqClassifier_" + MyUtils.randomString());
		templateBuilder.add(requirementClassifierKB, RDF.TYPE, "tosca:Requirement");

		IRI requirementProperty = null;
		if (requirementName != null) {
			requirementProperty = GetResources.getKBProperty(requirementName, this.namespacesOfType, kb);
			if (requirementProperty == null) {
				mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS), requirementName, "Cannot find requirement property"));
			}
			tempReq.put("r_a", requirementProperty);
		}
		
		if (requirementProperty != null)
			templateBuilder.add(requirementClassifierKB, factory.createIRI(KB.DUL + "classifies"), requirementProperty);

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(aadmModel.filter(requirement, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			NamedResource n = GetResources.setNamedResource(templatews, value.getLabel(), kb);			
			IRI kbTemplate = getKBTemplate(n);
			if (kbTemplate == null) {
				if (templateNames.contains(n.getResource()))
					kbTemplate = factory.createIRI(namespace + n.getResource());
				else
					mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS), requirement.getLocalName(), "Cannot find Template: " + value.getLabel()));
			}
			if (kbTemplate != null)
				templateBuilder.add(requirementClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbTemplate);
		} else {

			IRI root = createParameterKBModel(requirement);
			if (root != null)
				templateBuilder.add(requirementClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), root);
		}

		return requirementClassifierKB;

	}

	private IRI createParameterKBModel(IRI requirement) throws MappingException {
		IRI parameterClassifierKB = null;
		
		Optional<Resource> _parameter = Models.getPropertyResource(aadmModel, requirement,
				factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));
		IRI parameter = null;
		if (!_parameter.isPresent()) {
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS), requirement.getLocalName(), "Cannot find requirement parameter"));
		} else {
			parameter = (IRI) _parameter.get();
		

			String parameterName = null;
			Optional<Literal> _parameterName = Models
					.objectLiteral(aadmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null));
			if (!_parameter.isPresent()) {
				mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS), parameter.getLocalName() , "No 'name' defined for requirement parameter"));
			} else {
				parameterName = _parameterName.get().getLabel();
				subMappingPath += ErrorConsts.SLASH + parameterName;
			}
		
			// create classifier
			parameterClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
			templateBuilder.add(parameterClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
			if (parameterName != null) {
				IRI paramProperty = GetResources.getKBProperty(parameterName, this.namespacesOfType, kb);
				if (paramProperty == null) 
					mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS), parameterName , "Cannot find requirement parameter"));
				else 
					templateBuilder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "classifies"), paramProperty);
			}
			
			// check for direct values of parameters
			Literal value = Models
					.objectLiteral(aadmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "value"), null))
					.orElse(null);

			if (value != null) { // this means there is no parameters
				NamedResource n = GetResources.setNamedResource(templatews, value.getLabel(), kb);
				IRI kbTemplate = getKBTemplate(n);
				if (kbTemplate == null) {
					if (templateNames.contains(n.getResource()))
						kbTemplate = factory.createIRI(namespace + n.getResource());
					else
						mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.REQUIREMENTS), requirement.getLocalName(), "Cannot find Template: " + value.getLabel()));
				}
				if (kbTemplate != null)
					templateBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbTemplate);
				
				//assign values for requirement validation
				if (parameterName.equals("node"))
					tempReq.put("node", kbTemplate);
				else if (parameterName.equals("capability"))
					tempReq.put("capability", kbTemplate);
			} else {

				IRI root = createParameterKBModel(parameter);
				templateBuilder.add(parameterClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), root);

			}
		}
		return parameterClassifierKB;
	}

	private IRI createPropertyOrAttributeKBModel(IRI exchangeParameter) throws MappingException {
		Optional<Literal> _propertyName = Models
				.objectLiteral(aadmModel.filter(exchangeParameter, factory.createIRI(KB.EXCHANGE + "name"), null));

		String propertyName = null;
		if (!_propertyName.isPresent())
			mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.PROPERTIES), exchangeParameter.getLocalName(), "No 'name' defined for property"));
		else {
			propertyName = _propertyName.get().getLabel();
			subMappingPath += ErrorConsts.SLASH + propertyName;
		}
//		Optional<Literal> _value = Models
//				.objectLiteral(aadmModel.filter(exchangeParameter, factory.createIRI(KB.EXCHANGE + "value"), null));
		IRI propertyClassifierKB = null;

		Set<String> _values = Models.getPropertyStrings(aadmModel, exchangeParameter,
				factory.createIRI(KB.EXCHANGE + "value"));

		Set<String> listValues = Models.getPropertyStrings(aadmModel, exchangeParameter,
				factory.createIRI(KB.EXCHANGE + "listValue"));

		LOG.info("------------ {}", _values);
		LOG.info("-----ListValues----- {}", listValues);

		if (_values.isEmpty() && listValues.isEmpty()) {
			LOG.info("No value found for property: {}", exchangeParameter.getLocalName());
		}

//		String value = _value.isPresent() ? _value.get().stringValue() : null;
		if (propertyName != null)
			definedPropertiesForValidation.add(propertyName);

		LOG.info("Property name: {}, value: {}", new Object[] {propertyName, _values});
		
		Optional<Resource> _parameterType  = Models.getPropertyResource(aadmModel, exchangeParameter,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String parameterType = MyUtils.getStringValue(_parameterType.get());
		
		switch (parameterType) {
			case KBConsts.ATTRIBUTE:
				propertyClassifierKB = factory.createIRI(namespace + KBConsts.ATTR_CLASSIFIER + MyUtils.randomString());
				templateBuilder.add(propertyClassifierKB, RDF.TYPE, "tosca:Attribute");
				break;
			case KBConsts.PARAMETER:
				propertyClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
				templateBuilder.add(propertyClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			case KBConsts.PROPERTY :
				propertyClassifierKB = factory.createIRI(namespace + KBConsts.PROP_CLASSIFIER + MyUtils.randomString());
				templateBuilder.add(propertyClassifierKB, RDF.TYPE, "tosca:Property");
				break;
			default:
				LOG.info("parameterType = {} does not exist", parameterType);
		}

		Optional<String> description = Models.getPropertyString(aadmModel, exchangeParameter,
				factory.createIRI(KB.EXCHANGE + KBConsts.DESCRIPTION));
		if (description.isPresent())
			aadmBuilder.add(propertyClassifierKB, factory.createIRI(KB.DCTERMS + KBConsts.DESCRIPTION), description.get());
		
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
		
		/*Just assign all properties to property maps, so as to detect properties of type map that have no parameter.
		 IDE assigns an empty value "" to the property.*/
		if (tempPropertyMap == null && parameterType.equals(KBConsts.PROPERTY)) {
			tempPropertyMap = new PropertyMap(propertyName);
			propertyMapValuesForValidation.add(tempPropertyMap);
			tempPropertyMap = null;
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
				propertyValuesForValidation.put(propertyName, value);
				
				// for constraints property map validation, assign the nested parameters
				if (tempPropertyMap != null && tempPropertyMap.getMapProperties() != null && parameterType.equals(KBConsts.PARAMETER)) {
					HashMap<String, String> _inPropertyMap = tempPropertyMap.getMapProperties().get(currentPropertyMap);
					_inPropertyMap.put(propertyName, value);
				}
			} else {
				IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
				templateBuilder.add(list, RDF.TYPE, "tosca:List");
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
			LOG.info("*****************************} else if (!listValues.isEmpty()) {");
			IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
			templateBuilder.add(list, RDF.TYPE, "tosca:List");
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
					factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));
			
			for (Resource _parameter : _parameters) {
				if (tempPropertyMap != null && parameterType.equals(KBConsts.PROPERTY)) {
					Optional<Literal> propName = Models
							.objectLiteral(aadmModel.filter(_parameter, factory.createIRI(KB.EXCHANGE + "name"), null));
					tempPropertyMap.getMapProperties().put(propName.get().getLabel(), new HashMap<String, String>());
					this.currentPropertyMap = propName.get().getLabel();
				}
				
				IRI parameter = (IRI) _parameter;
				IRI _p = createPropertyOrAttributeKBModel(parameter);
				
				templateBuilder.add(propertyClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), _p);
			}

			if (tempPropertyMap != null && parameterType.equals(KBConsts.PROPERTY)) {			
				propertyMapValuesForValidation.add(tempPropertyMap);
				tempPropertyMap = null;
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
		IRI capabilityClassifierKB = null;
		Optional<Resource> _type  = Models.getPropertyResource(aadmModel, capability,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String type = MyUtils.getStringValue(_type.get());
		
		switch (type) {
			case KBConsts.CAPABILITY:
				capabilityClassifierKB = factory.createIRI(namespace + "CapabilityClassifier_" + MyUtils.randomString());
				aadmBuilder.add(capabilityClassifierKB, RDF.TYPE, "tosca:Capability");
				break;
			case KBConsts.PARAMETER:
				capabilityClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
				aadmBuilder.add(capabilityClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			default:
				LOG.warn("type = {} does not exist", type);
		}
		

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
			
			String valueString = value.getLabel();
			
			//in type:, there is a node type or a string. e.g. capabilities/network/type/tosca.capabilities.Network, or capabilities/os/type/linux, so no error to be thrown
			if (capabilityProperty.getLocalName().equals("type")) {			
				NamedResource n = GetResources.setNamedResource(templatews, value.getLabel(), kb);
				IRI kbTemplate = getKBTemplate(n);
				if (kbTemplate == null) {
					if (templateNames.contains(n.getResource())) {
						kbTemplate = factory.createIRI(namespace + n.getResource());
						templateBuilder.add(capabilityClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbTemplate);
					}
					else {
						//e.g. os/type/linux
						templateBuilder.add(capabilityClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), valueString);
						//mappingModels.add(new MappingValidationModel(currentTemplate, capability.getLocalName(), "Cannot find Template: " + value.getLabel()));
					}
				} 
			} else if (Ints.tryParse(valueString) != null) {
				templateBuilder.add(capabilityClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), Integer.parseInt(valueString));
			} 
			
		} else {
			Set<Resource> _properties = Models.getPropertyResources(aadmModel, capability,
			factory.createIRI(KB.EXCHANGE + "properties"));
			//definedPropertiesForValidation.clear();
			if (_properties.isEmpty()) {
				/*IRI root = createParameterKBModel(capability);
				templateBuilder.add(capabilityClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), root);*/
				Set<Resource> _parameters = Models.getPropertyResources(aadmModel, capability,
						factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));
				for (Resource _parameter : _parameters) {
					IRI parameter = (IRI) _parameter;
					IRI _p = createCapabilityKBModel(parameter);
					templateBuilder.add(capabilityClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), _p);
				}
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
	
	private IRI createTriggerKBModel(IRI trigger) throws MappingException {
		Optional<Literal> _triggerName = Models
				.objectLiteral(aadmModel.filter(trigger, factory.createIRI(KB.EXCHANGE + "name"), null));
		
		String triggerName = _triggerName.get().getLabel();
		subMappingPath += ErrorConsts.SLASH + triggerName;
		
		IRI triggerProperty = null;
		triggerProperty = GetResources.getKBProperty(triggerName, this.namespacesOfType, kb);
		if (triggerProperty == null || triggerProperty.toString().equals(namespace + triggerName)) {
				triggerProperty = factory.createIRI(namespace + triggerName);
				aadmBuilder.add(triggerProperty, RDF.TYPE, "rdf:Property");
		}
		
		Optional<Resource> _type  = Models.getPropertyResource(aadmModel, trigger,
				factory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		String type = MyUtils.getStringValue(_type.get());
		
		IRI triggerClassifierKB = null;
		switch (type) {
			case KBConsts.TRIGGER:
				triggerClassifierKB = factory.createIRI(namespace + "TriggerClassifer_" + MyUtils.randomString());
				aadmBuilder.add(triggerClassifierKB, RDF.TYPE, "tosca:Trigger");
				break;
			case KBConsts.PARAMETER:
				triggerClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
				aadmBuilder.add(triggerClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
				break;
			default:
				LOG.warn("type = " + type + " does not exist");
		}
		
		if (triggerProperty != null)
			aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.DUL + "classifies"), triggerProperty);
		
		Optional<String> description = Models.getPropertyString(aadmModel, trigger,
				factory.createIRI(KB.EXCHANGE + KBConsts.DESCRIPTION));
		if (description.isPresent())
			aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.DCTERMS + KBConsts.DESCRIPTION), description.get());

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(aadmModel.filter(trigger, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			if (triggerName.equals("node")) {
				NamedResource n = GetResources.setNamedResource(templatews, value.getLabel(), kb);
				IRI kbNode = getKBTemplate(n);
				if (kbNode == null) {
					if (templateNames.contains(n.getResource()))
						kbNode = factory.createIRI(namespace + n.getResource());
					else {
						mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.TRIGGERS), trigger.getLocalName(), "Cannot find Template: " + value.getLabel() + " for trigger = " + triggerName));
						LOG.warn("{}: Cannot find template: {} for trigger {}", currentTemplate, value.getLabel(), triggerName);
					}
				}
				if(kbNode != null)
					aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
			} /*else if (triggerName.equals("capability") || triggerName.equals("requirement")) {
				IRI req_cap = GetResources.getReqCapFromEventFilter(kb, value.getLabel());
				if (req_cap != null) {
					aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), req_cap);
				} else {
					mappingModels.add(new MappingValidationModel(currentTemplate, trigger.getLocalName(), "Cannot find " + value.getLabel() + " for trigger = " + triggerName));
					LOG.log(Level.WARNING, "{0}: Cannot find template: {1} for trigger {2}", new Object[] {currentTemplate, value.getLabel(), triggerName});
				}		
			} */else {
				Object i = null;
				if ((i = Ints.tryParse(value.toString())) != null)
					aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
				else 
					aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);
			}
		} else {
			Set<Resource> _parameters = Models.getPropertyResources(aadmModel, trigger,
					factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));
			for (Resource _parameter : _parameters) {
				IRI parameter = (IRI) _parameter;
				IRI _p = createTriggerKBModel(parameter);
				aadmBuilder.add(triggerClassifierKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), _p);
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
		LOG.info("createTargetKBModel {}", parameter);
		Set<Literal> listValues= Models.objectLiterals(aadmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "listValue"), null));
		
		LOG.info("-----ListValue--- {}", listValues);
		IRI list = factory.createIRI(namespace + "List_" + MyUtils.randomString());
		
		IRI parameterClassifierKB = factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
		
		aadmBuilder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), list);
		aadmBuilder.add(list, RDF.TYPE, "tosca:List");
		
		for (Literal l:listValues) {
			aadmBuilder.add(parameterClassifierKB, RDF.TYPE, "tosca:Target");
			aadmBuilder.add(list, RDF.TYPE, "tosca:List");
			
			
			NamedResource n = GetResources.setNamedResource(templatews, l.getLabel(), kb);
			IRI kbNode = getKBTemplate(n);
			if (kbNode == null) {
				if (templateNames.contains(n.getResource()))
					kbNode = factory.createIRI(namespace + n.getResource());
				else {
					mappingModels.add(new MappingValidationModel(getContextPath(ErrorConsts.TARGETS), "targets", "Cannot find target: " + l.getLabel()));
					LOG.warn("{}: Cannot find Node: {} ", currentTemplate, l.getLabel());
				}
			}
			if(kbNode != null)
				aadmBuilder.add(list, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbNode);
		}
		
		return parameterClassifierKB;		
	}
	
	private void createInputOutputKBModel(boolean isInput) throws MappingException {
		LOG.info("isInput: {}",  isInput);
		final String classUsed = isInput ? "Input" : "Output";
		final String id = isInput ? "topology_template_inputs_" : "topology_template_outputs_";
		final String specificationProperty = isInput ? "includesInput" : "includesOutput";
		final String conceptProperty = isInput ? "input" : "output";
		
		Set<Resource> inputs = aadmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + classUsed)).subjects();
		if (inputs.size() > 0) {
			IRI inputKB = factory.createIRI(namespace + id + MyUtils.randomString());
			templateBuilder.add(inputKB, RDF.TYPE, factory.createIRI(KB.TOSCA + classUsed));
			if (aadmKB!=null)
				aadmBuilder.add(aadmKB, factory.createIRI(KB.SODA + specificationProperty), inputKB);

			// create description
			IRI inputDescriptionKB = factory.createIRI(namespace + "Desc_" + MyUtils.randomString());
			templateBuilder.add(inputDescriptionKB, RDF.TYPE, "soda:SodaliteDescription");
			templateBuilder.add(inputKB, factory.createIRI(KB.SODA + "hasContext"), inputDescriptionKB);
			templateBuilder.add(inputKB, factory.createIRI(KB.SODA + "hasName"), id);
			
			for (Resource _input : inputs) {
				IRI input = (IRI) _input;
				Optional<Literal> _inputName = Models
						.objectLiteral(aadmModel.filter(input, factory.createIRI(KB.EXCHANGE + "name"), null));
				
				String inputName = null;
				if (!_inputName.isPresent())
					mappingModels.add(new MappingValidationModel(isInput ? "inputs" : "outputs", input.getLocalName(), "No 'name' defined for " + conceptProperty));
				else {
					inputName = _inputName.get().getLabel();
					LOG.info("Input/Output name: {}",  inputName);
				}
				
				// for each tosca input we need to have a Feature
				IRI inputFeatureKB = factory.createIRI(namespace + classUsed + "_" + MyUtils.randomString());
				templateBuilder.add(inputFeatureKB, RDF.TYPE, "tosca:Feature");

				if (inputName != null) {
					IRI kbProperty = GetResources.getKBProperty(inputName, this.namespacesOfType, kb);
					if (kbProperty == null) {
						kbProperty = factory.createIRI(namespace + inputName);
						templateBuilder.add(kbProperty, RDF.TYPE, "rdf:Property");
					}
					templateBuilder.add(inputFeatureKB, factory.createIRI(KB.DUL + "classifies"), kbProperty);
				}
				templateBuilder.add(inputDescriptionKB, factory.createIRI(KB.TOSCA + conceptProperty), inputFeatureKB);
				
				Optional<String> description = Models.getPropertyString(aadmModel, input,
						factory.createIRI(KB.EXCHANGE + KBConsts.DESCRIPTION));
				if (description.isPresent())
					templateBuilder.add(inputFeatureKB, factory.createIRI(KB.DCTERMS + KBConsts.DESCRIPTION), description.get());

				Set<Resource> _parameters = Models.getPropertyResources(aadmModel, _input,
						factory.createIRI(KB.EXCHANGE + KBConsts.HAS_PARAMETER));

				// TODO: HERE WE NEED TO IMPLEMENT RECURSION
				for (Resource _parameter : _parameters) {
					IRI parameter = (IRI) _parameter;
					IRI propertyClassifierKB = createPropertyOrAttributeKBModel(parameter);

					templateBuilder.add(inputFeatureKB, factory.createIRI(KB.DUL + KBConsts.HAS_PARAMETER), propertyClassifierKB);
				}

			}
		}
		
	}
	
	//for the context path of Mapping errors
	private  String getContextPath(String entity) {
		return currentPrefixTemplate + ErrorConsts.SLASH + currentTemplate + ErrorConsts.SLASH + entity + subMappingPath;
	}
	
	private IRI getKBTemplate(NamedResource n) {
		LOG.info("getKBTemplate label={}, namespace ={} ", n.getResource(), n.getNamespace());
		String namespace = n.getNamespace();
		String resource = n.getResource();
		String sparql = "select distinct ?x { \r\n" + 
						"   ?m DUL:isSettingFor ?x .\r\n" + 
						"	{        \r\n" + 
						"		?x rdf:type ?type .\r\n" +
						"        ?x soda:hasName ?name ." + 
						"        FILTER NOT EXISTS\r\n" + 
						"        { \r\n" + 
						"          GRAPH ?g { ?x ?p ?o } \r\n" + 
						"        }\r\n" + 
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
		
		
		LOG.info(sparql);
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
	
	/*public Set<IRI> getTemplatesIRIs(IRI namespace, String resourceName) {
		Set<IRI> templatesIRIs = new HashSet<>();
		for (String t: this.templateNames) {
			templatesIRIs.add(kb.factory.createIRI(namespace + t));
		}
		
		return templatesIRIs;
	}*/
	
	public List<ValidationModel> getModifiedModels() {
		return modifiedModels;
	}
	
	public List<ValidationModel> getSuggestedModels() {
		return suggestedModels;
	}
	
	public void shutDown() {
		LOG.info("shutting down");
		if (kb != null) {
			kb.shutDown();
		}
	}

	public void save() throws ValidationException, IOException {
		Model amodel = aadmBuilder.build();
		Model tmodel = templateBuilder.build();
		
		kb.connection.add(amodel);
		if (namespace.toString().contains("global"))
			kb.connection.add(tmodel);
		else
			kb.connection.add(tmodel,namespace);
		
		String aadmId = MyUtils.getStringPattern(this.aadmKB.stringValue(), ".*/(AADM_.*).*");
		//Requirement first check about existence, and (complete = true) update models if matching nodes found
		IRI context = namespace.toString().contains("global") ? null : namespace;
		RequirementExistenceValidation r = new RequirementExistenceValidation(aadmId, complete, kb, namespace.toString(), context);
		//Check for required omitted requirements
		validationModels.addAll(r.validate());
		//if (!validationModels.isEmpty()) {
			//Set<IRI> templatesIRIs = MyUtils.getResourceIRIs(this.kb, this.namespace, this.templateNames);
	/*		KBApi api = new KBApi(kb);
			api.deleteModel(aadmId);
			throw new ValidationException(validationModels);
		}*/
		
		suggestedModels.addAll(r.getSuggestions());
		modifiedModels.addAll(r.getModifiedModels());
		
		//Sommelier validations
		/*ValidationService v = new ValidationService(aadmId, this.templateRequirements, this.templateTypes);
		validationModels.addAll(v.validate());*/
		
		if (!validationModels.isEmpty()) {
			KBApi api = new KBApi(kb);
			api.deleteModel(this.aadmKB.toString());
			throw new ValidationException(validationModels);
		}
		
	}
}
