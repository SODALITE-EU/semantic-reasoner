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

import com.google.common.primitives.Ints;

import kb.dsl.exceptions.MappingException;
import kb.repository.KB;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
import kb.validation.required.RequiredPropertyValidation;

public class DSLMappingService {

	public KB kb;
	public ValueFactory factory;

	public Model aadmModel;

	public ModelBuilder builder;

	IRI aadmContainer;

	String base = "https://www.sodalite.eu/ontologies/";
//	String ws = base + "workspace/woq2a8g0gscuqos88bn2p7rvlq3/";
//	String ws = "http://";
	String ws = base + "workspace/1/";

	IRI aadmKB;
	IRI context;

	// Validation
	Set<String> definedPropertiesForValidation = new HashSet<String>(),
			definedAttributesForValidation = new HashSet<String>();

	List<ValidationModel> validationModels = new ArrayList<>();

	public DSLMappingService(KB kb, String aadmTTL, String submissionId)
			throws RDFParseException, UnsupportedRDFormatException, IOException {
		super();
		this.kb = kb;
		this.factory = SimpleValueFactory.getInstance();

		builder = new ModelBuilder();
		builder.setNamespace("soda", KB.SODA)
				.setNamespace("dul", KB.DUL)
				.setNamespace("dcterms", KB.DCTERMS).setNamespace("exchange", KB.EXCHANGE)
				.setNamespace("tosca", KB.TOSCA);

//		System.out.println(aadmTTL);
		InputStream targetStream = IOUtils.toInputStream(aadmTTL, Charset.defaultCharset());
		aadmModel = Rio.parse(targetStream, "", RDFFormat.TURTLE);
		targetStream.close();

		context = kb.factory.createIRI("http://" + submissionId);

	}

	public IRI start() throws MappingException, ValidationException {

		// AADM
		aadmKB = null;

		for (Resource _aadm : aadmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "AADM")).subjects()) {
			String userId = Models
					.objectLiteral(aadmModel.filter(_aadm, factory.createIRI(KB.EXCHANGE + "userId"), null))
					.orElseThrow(
							() -> new MappingException("No 'userId' defined for AADM: " + ((IRI) _aadm).getLocalName()))
					.stringValue();

			builder.setNamespace("ws", ws);
			aadmKB = factory.createIRI(ws + "AADM_" + MyUtils.randomString());
			builder.add(aadmKB, RDF.TYPE, "soda:AbstractApplicationDeployment");

			IRI user = factory.createIRI(ws + userId);
			builder.add(user, RDF.TYPE, "soda:User");

			builder.add(aadmKB, factory.createIRI(KB.SODA + "createdBy"), user);
			builder.add(aadmKB, factory.createIRI(KB.SODA + "createdAt"), DateTime.now());
			break;
		}

		if (aadmKB == null) {
			throw new MappingException("No AADM container found.");
		}

		// TEMPLATES
		for (Resource _template : aadmModel.filter(null, RDF.TYPE, factory.createIRI(KB.EXCHANGE + "Template"))
				.subjects()) {
			IRI template = (IRI) _template;

			String templateName = Models
					.objectLiteral(aadmModel.filter(template, factory.createIRI(KB.EXCHANGE + "name"), null))
					.orElseThrow(
							() -> new MappingException("No 'name' defined for template: " + template.getLocalName()))
					.stringValue();
			String templateType = Models
					.objectLiteral(aadmModel.filter(template, factory.createIRI(KB.EXCHANGE + "type"), null))
					.orElseThrow(
							() -> new MappingException("No 'type' defined for template: " + template.getLocalName()))
					.stringValue();

			System.out.println(String.format("Name: %s, type: %s", templateName, templateType));

			// add template to the aadm container instance
			IRI templateKB = factory.createIRI(ws + templateName); // this will be always new
			IRI kbNodeType = getKBNodeType(templateType, "tosca:tosca.entity.Root");

			builder.add(templateKB, RDF.TYPE, kbNodeType);
			builder.add(aadmKB, factory.createIRI(KB.SODA + "includesTemplate"), templateKB);

			// create description
			IRI templateDescriptionKB = factory.createIRI(ws + "Desc_" + MyUtils.randomString());
			builder.add(templateDescriptionKB, RDF.TYPE, "soda:SodaliteDescription");
			builder.add(templateKB, factory.createIRI(KB.SODA + "hasContext"), templateDescriptionKB);

			// properties
			Set<Resource> _properties = Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "properties"));
			definedPropertiesForValidation.clear();
			for (Resource _property : _properties) {
				IRI property = (IRI) _property;
				IRI propertyClassifierKB = createPropertyOrAttributeKBModel(property);

				// add property classifiers to the template context
				builder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "properties"), propertyClassifierKB);
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

				// add attribute classifiers to the template context
				builder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "attributes"), attributesClassifierKB);

			}

			// requirements
			for (Resource _requirement : Models.getPropertyResources(aadmModel, _template,
					factory.createIRI(KB.EXCHANGE + "requirements"))) {
				IRI requirement = (IRI) _requirement;
				IRI requirementClassifierKB = createRequirementKBModel(requirement);

				// add attribute classifiers to the template context
				builder.add(templateDescriptionKB, factory.createIRI(KB.TOSCA + "requirements"),
						requirementClassifierKB);

			}

			// capabilities

			// misc

		}
		if (!validationModels.isEmpty()) {
			throw new ValidationException(validationModels);
		}
		return aadmKB;

	}

	private IRI createRequirementKBModel(IRI requirement) throws MappingException {
		String requirementName = Models
				.objectLiteral(aadmModel.filter(requirement, factory.createIRI(KB.EXCHANGE + "name"), null))
				.orElseThrow(
						() -> new MappingException("No 'name' defined for requirement: " + requirement.getLocalName()))
				.stringValue();

		// create classifier
		IRI requirementClassifierKB = factory.createIRI(ws + "ReqClassifier_" + MyUtils.randomString());
		builder.add(requirementClassifierKB, RDF.TYPE, "tosca:Requirement");

		IRI requirementProperty = getKBProperty(requirementName);
		if (requirementProperty == null) {
			throw new MappingException("Cannot find requirement property: " + requirementName);
		}
		builder.add(requirementClassifierKB, factory.createIRI(KB.DUL + "classifies"), requirementProperty);

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(aadmModel.filter(requirement, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			IRI kbTemplate = getKBTemplate(value.getLabel());
			if (kbTemplate == null) {
				// throw new Exception("Cannot find node: " + value);
				kbTemplate = factory.createIRI(ws + value.getLabel());
			}
			builder.add(requirementClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbTemplate);
		} else {

			IRI root = createParameterKBModel(requirement);
			builder.add(requirementClassifierKB, factory.createIRI("dul:hasParameter"), root);
		}

		return requirementClassifierKB;

	}

	private IRI createParameterKBModel(IRI requirement) throws MappingException {
		Optional<Resource> _parameter = Models.getPropertyResource(aadmModel, requirement,
				factory.createIRI(KB.EXCHANGE + "hasParameter"));
		if (!_parameter.isPresent()) {
			throw new MappingException("Cannot find parameter for: " + requirement.getLocalName());
		}
		IRI parameter = (IRI) _parameter.get();

		String parameterName = Models
				.objectLiteral(aadmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null))
				.orElseThrow(() -> new MappingException("No 'name' defined for parameter: " + parameter.getLocalName()))
				.stringValue();

		// create classifier
		IRI parameterClassifierKB = factory.createIRI(ws + "ParamClassifier_" + MyUtils.randomString());
		builder.add(parameterClassifierKB, RDF.TYPE, "soda:SodaliteParameter");

		IRI paramProperty = getKBProperty(parameterName);
		if (paramProperty == null) {
			throw new MappingException("Cannot find requirement parameter: " + parameterName);
		}
		builder.add(parameterClassifierKB, factory.createIRI(KB.DUL + "classifies"), paramProperty);

		// check for direct values of parameters
		Literal value = Models
				.objectLiteral(aadmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);

		if (value != null) { // this means there is no parameters
			IRI kbTemplate = getKBTemplate(value.getLabel());
			if (kbTemplate == null) {
				// throw new Exception("Cannot find node: " + value);
				kbTemplate = factory.createIRI(ws + value.getLabel());
			}
			builder.add(parameterClassifierKB, factory.createIRI(KB.TOSCA + "hasObjectValue"), kbTemplate);
		} else {

			IRI root = createParameterKBModel(parameter);
			builder.add(parameterClassifierKB, factory.createIRI("dul:hasParamater"), root);

		}
		return parameterClassifierKB;
	}

	private IRI createPropertyOrAttributeKBModel(IRI parameter) throws MappingException {
		String propertyName = Models
				.objectLiteral(aadmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "name"), null))
				.orElseThrow(() -> new MappingException("No 'name' defined for property: " + parameter.getLocalName()))
				.stringValue();

		String value = Models
				.objectLiteral(aadmModel.filter(parameter, factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElseThrow(() -> new MappingException("No 'value' defined for property: " + parameter.getLocalName()))
				.stringValue();

		if (value == null) {
			System.err.println("No value found for property: " + parameter.getLocalName());
		}

		definedPropertiesForValidation.add(propertyName);

		System.out.println(String.format("Property name: %s, value: %s", propertyName, value));

		// create classifier
		IRI propertyClassifierKB = factory.createIRI(ws + "PropClassifer_" + MyUtils.randomString());
		builder.add(propertyClassifierKB, RDF.TYPE, "tosca:Property");

		// create rdf:property
		IRI kbProperty = getKBProperty(propertyName);
		if (kbProperty == null) {
			kbProperty = factory.createIRI(ws + "Prop_" + propertyName);
			builder.add(kbProperty, RDF.TYPE, "rdf:Property");
		}
		builder.add(propertyClassifierKB, factory.createIRI(KB.DUL + "classifies"), kbProperty);

		// handle values
		Object i = null;
		if ((i = Ints.tryParse(value)) != null) {
			builder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (int) i);
		} else if ((i = BooleanUtils.toBooleanObject(value)) != null) {
			builder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), (boolean) i);
		} else
			builder.add(propertyClassifierKB, factory.createIRI(KB.TOSCA + "hasDataValue"), value);

		return propertyClassifierKB;

	}

	private IRI getKBNodeType(String label, String type) {
		String sparql = "select ?x { ?x rdfs:subClassOf " + type + " . FILTER (strends(str(?x), \"" + label + "\")). }";
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
		String sparql = "select ?x { ?x a rdf:Property . FILTER (strends(str(?x), \"" + label + "\")). }";
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

	private IRI getKBTemplate(String label) {
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

		Set<Resource> templates = Models
				.objectResources(aadmModel.filter(aadmKB, factory.createIRI(KB.SODA + "includesTemplate"), null));
		String sparql = "select ?x ?t "
				+ "{ ?x a soda:AbstractApplicationDeployment; "
				+ " 	soda:includesTemplate ?t ."
				+ "}";
		String query = KB.PREFIXES + sparql;
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query);
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI t = (IRI) bindingSet.getBinding("t").getValue();
			if (templates.contains(t)) {

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

	public static void main(String[] args)
			throws RDFParseException, UnsupportedRDFormatException, IOException, Exception {
		String aadmTTL = MyUtils.fileToString("dsl/ide_snow_v3.ttl");

		KB kb = new KB("TOSCA_automated");
		DSLMappingService m = new DSLMappingService(kb, aadmTTL, "test");
		m.start();
		m.save();
		m.shutDown();

	}

}
