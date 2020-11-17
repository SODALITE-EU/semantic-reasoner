package kb;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.repository.RepositoryResult;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kb.clean.ModifyKB;
import kb.dto.AADM;
import kb.dto.Attribute;
import kb.dto.Capability;
import kb.dto.Interface;
import kb.dto.Node;
import kb.dto.NodeFull;
import kb.dto.NodeType;
import kb.dto.Operation;
import kb.dto.Optimization;
import kb.dto.Parameter;
import kb.dto.Property;
import kb.dto.Requirement;
import kb.dto.SodaliteAbstractModel;
import kb.repository.KB;
import kb.repository.KBConsts;
import kb.utils.ConfigsLoader;
import kb.utils.InferencesUtil;
import kb.utils.MyFileUtil;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;

import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
import kb.validation.exceptions.models.optimization.ApplicationTypeModel;
import kb.validation.exceptions.models.optimization.OptimizationMismatchModel;
import kb.validation.exceptions.models.optimization.OptimizationModel;

public class KBApi {

	public KB kb;
	static ConfigsLoader configInstance = ConfigsLoader.getInstance();
	static {
		configInstance.loadProperties();
	}
	
	public KBApi() {
		kb = new KB(configInstance.getGraphdb(), "TOSCA");
	}

	public void shutDown() {
		kb.shutDown();
	}
	
	public String getResourceIRI(String resource) {
		return MyUtils.getFullResourceIRI(resource, kb);
	}
	
	public Set<Attribute> getAttributes(String resource, boolean isTemplate) throws IOException {
		System.out.println("getAttributes: " + resource);
		
		Set<Attribute> attributes = new HashSet<>();
		String sparql = MyUtils
				.fileToString(!isTemplate ? "sparql/getAttributes.sparql" : "sparql/getAttributesTemplate.sparql");

		String query = KB.PREFIXES + sparql;

		// System.out.println(query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("resource", kb.getFactory().createIRI(resource)));

		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI attr = (IRI) bindingSet.getBinding("attribute").getValue();
			IRI concept = (IRI) bindingSet.getBinding("p").getValue();
			Value _value = bindingSet.hasBinding("value") ? bindingSet.getBinding("value").getValue() : null;

			Attribute a = new Attribute(attr);
			a.setClassifiedBy(concept);
			
			if (_value != null)
				a.setValue(_value, kb);

			attributes.add(a);
		}
		result.close();
		for (Attribute a : attributes) {
			a.build(this);
		}
		return attributes;
	}

	public Set<Property> getProperties(String resource, boolean isTemplate) throws IOException {
		System.out.println("getProperties: " +  resource);
		
		Set<Property> properties = new HashSet<>();
		String sparql = MyUtils
				.fileToString(!isTemplate ? "sparql/getProperties.sparql" : "sparql/getPropertiesTemplate.sparql");
		String query = KB.PREFIXES + sparql;

		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("resource",  kb.getFactory().createIRI(resource)));

		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI p1 = (IRI) bindingSet.getBinding("property").getValue();
			IRI concept = (IRI) bindingSet.getBinding("concept").getValue();
			Value _value = bindingSet.hasBinding("value") ? bindingSet.getBinding("value").getValue() : null;

			Property a = new Property(p1);
			a.setClassifiedBy(concept);
			if (_value != null)
				a.setValue(_value, kb);

			properties.add(a);
		}
		result.close();
		for (Property property : properties) {
			property.build(this);
		}

		return properties;
	}

	public Set<Property> getInputs(String resource, boolean isTemplate) throws IOException {
		System.out.println("getInputs: " + resource);
		
		Set<Property> inputs = new HashSet<>();
		String sparql = MyUtils.fileToString("sparql/getInputs.sparql");
		String query = KB.PREFIXES + sparql;

		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("resource",  kb.getFactory().createIRI(resource)));

		while (result.hasNext()) {
			System.err.println("in");
			BindingSet bindingSet = result.next();
			IRI p1 = (IRI) bindingSet.getBinding("property").getValue();
			IRI concept = (IRI) bindingSet.getBinding("concept").getValue();
			Value _value = bindingSet.hasBinding("value") ? bindingSet.getBinding("value").getValue() : null;

			Property a = new Property(p1);
			a.setClassifiedBy(concept);
			if (_value != null)
				a.setValue(_value, kb);

			inputs.add(a);
		}
		result.close();
		for (Property input : inputs) {
			input.build(this);
		}

		return inputs;
	}

	public Set<Capability> getCapabilities(String resource, boolean isTemplate) throws IOException {
		System.out.println("getCapabilities: " + resource);
		
		Set<Capability> capabilities = new HashSet<>();

		String sparql = MyUtils
				.fileToString(!isTemplate ? "sparql/getCapabilities.sparql" : "sparql/getCapabilitiesTemplate.sparql");
		String query = KB.PREFIXES + sparql;

		// System.out.println(query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("resource",  kb.getFactory().createIRI(resource)));

		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI p1 = (IRI) bindingSet.getBinding("capability").getValue();
			IRI concept = (IRI) bindingSet.getBinding("classifier").getValue();

			Capability c = new Capability(p1);
			c.setClassifiedBy(concept);

			capabilities.add(c);
		}
		result.close();

		for (Capability capability : capabilities) {
			capability.build(this);
		}

		return capabilities;
	}

	public Set<Requirement> getRequirements(String resource, boolean isTemplate) throws IOException {
		System.out.println("getRequirements: " + resource);
		
		Set<Requirement> requirements = new HashSet<>();

		String sparql = MyUtils
				.fileToString(!isTemplate ? "sparql/getRequirements.sparql" : "sparql/getRequirementsTemplate.sparql");
		String query = KB.PREFIXES + sparql;

		// System.out.println(query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("resource",  kb.getFactory().createIRI(resource)));

		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI p1 = (IRI) bindingSet.getBinding("requirement").getValue();
			IRI concept = (IRI) bindingSet.getBinding("classifier").getValue();
			Value _value = bindingSet.hasBinding("value") ? bindingSet.getBinding("value").getValue() : null;

			Requirement c = new Requirement(p1);
			c.setClassifiedBy(concept);
			if (_value != null)
				c.setValue(_value, kb);

			requirements.add(c);

		}
		result.close();

		for (Requirement requirement : requirements) {
			requirement.build(this);
		}

		return requirements;
	}

	public Set<Node> getNodes(List<String> imports, String type) throws IOException {
		Set<Node> nodes = new HashSet<>();

		//get types in global workspace
		String sparql = type.equals("data") ? MyUtils.fileToString("sparql/getDataTypes.sparql") : MyUtils.fileToString("sparql/getTypes.sparql");
		String query = KB.PREFIXES + sparql;

		System.out.println(query);
		String root_type = KBConsts.TYPES.get(type);
		TupleQueryResult result;
		if (!type.equals("data"))
			result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("root_type", kb.getFactory().createIRI(KB.TOSCA + root_type)));
		else
			result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query);
		
		_setNodeTypes(result, nodes);
		if (!imports.isEmpty()) {
			//get nodes from named namespaces
			String sparql2 = "select ?node ?description ?superclass ?g\r\n" +
								"FROM <http://www.ontotext.com/implicit>\r\n" + 
								"FROM <http://www.ontotext.com/explicit>\r\n";
		
			sparql2 += QueryUtil.namedGraphsForQuery(kb, imports);

			sparql2 += "where {\r\n" +
						"	GRAPH ?g {\r\n" +
						"		?node soda:hasContext ?c .\r\n" +
						"		?node sesame:directSubClassOf ?superclass .\r\n" +
						"		OPTIONAL {?node dcterms:description ?description .}\r\n" +
						"			FILTER (?node != owl:Nothing) .\r\n" +
						"	    }   \r\n";
			if (!type.equals("data"))
				sparql2 += "   ?node rdfs:subClassOf ?root_type .\r\n";
			else {
				sparql2 += "   ?node rdfs:subClassOf ?root_type .\r\n" +
				    		"   FILTER (?root_type IN (tosca:tosca.datatypes.Root, tosca:DataType))\r\n" +
				    		"   FILTER (?node != tosca:DataType)\r\n";
			}
			sparql2+="}\r\n";
			String query2 = KB.PREFIXES + sparql2;
		
			System.out.println(query2);
			TupleQueryResult result2 = QueryUtil.evaluateSelectQuery(kb.getConnection(), query2, new SimpleBinding("root_type", kb.getFactory().createIRI(KB.TOSCA + root_type)));
			_setNodeTypes(result2, nodes);
		}
		
		return nodes;
	}
	
	private void _setNodeTypes(TupleQueryResult result, Set<Node> nodes) {
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI node = (IRI) bindingSet.getBinding("node").getValue();
			String description = bindingSet.hasBinding("description")
					? bindingSet.getBinding("description").getValue().stringValue()
					: null;
			IRI superclass = (IRI) bindingSet.getBinding("superclass").getValue();
			IRI _namespace = bindingSet.hasBinding("g") ? (IRI) bindingSet.getBinding("g").getValue() : null;

			Node n = new Node(node);
			n.setDescription(description);
			n.setType(superclass);
			n.setNamespace(_namespace);

			nodes.add(n);
		}
		result.close();
	}

	public NodeFull getNode(String resource, boolean filterNormatives) throws IOException {
		if(filterNormatives) {
			if (resource.contains("/tosca."))
				return null;
		}
		String sparql = MyUtils.fileToString("sparql/getNode.sparql");
		String query = KB.PREFIXES + sparql;

		// System.out.println(resource);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("node", kb.getFactory().createIRI(resource)));

		NodeFull f = null;
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			String description = bindingSet.hasBinding("description")
					? bindingSet.getBinding("description").getValue().stringValue()
					: null;

			if (bindingSet.hasBinding("instanceType") && bindingSet.hasBinding("classType")) {
				f = new NodeFull(kb.factory.createIRI(resource), false);
				f.setType((IRI) bindingSet.getBinding("classType").getValue());
			} else if (bindingSet.hasBinding("instanceType")) {
				f = new NodeFull(kb.factory.createIRI(resource), true);
				f.setType((IRI) bindingSet.getBinding("instanceType").getValue());
			} else {
				f = new NodeFull(kb.factory.createIRI(resource), false);
				f.setType((IRI) bindingSet.getBinding("classType").getValue());
			}
			f.setDescription(description);
		}
		if (f != null)
			f.build(this);
		result.close();
		return f;
	}

	public Set<Interface> getInterfaces(String resource, boolean isTemplate) throws IOException {
		System.out.println("getInterfaces: " + resource);
		Set<Interface> interfaces = new HashSet<>();
		
		Set<IRI> nodes =  new HashSet<>();
		HashMap<IRI, IRI>  conceptMap = new HashMap<IRI, IRI>();
		HashMap<IRI, IRI>  interfaceMap = new HashMap<IRI, IRI>();
		
		String sparql = MyUtils
				.fileToString(!isTemplate ? "sparql/getInterfaces.sparql" : "sparql/getInterfacesTemplate.sparql");
		String query = KB.PREFIXES + sparql;
		
		// System.out.println(query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("resource",  kb.getFactory().createIRI(resource)));

		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI p1 = (IRI) bindingSet.getBinding("interface").getValue();
			IRI concept = (IRI) bindingSet.getBinding("classifier").getValue();

			IRI r = (IRI) bindingSet.getBinding("resource2").getValue();
				
			interfaceMap.put(r, p1);
			conceptMap.put(r, concept);
			nodes.add(r);
		}
		
		if (!nodes.isEmpty()) {
			IRI lowestNode = InferencesUtil.getLowestSubclass(kb, nodes);
			
			Interface c = new Interface(interfaceMap.get(lowestNode));
			c.setClassifiedBy(conceptMap.get(lowestNode));

			interfaces.add(c);

		}
		
		result.close();

		for (Interface _interface : interfaces) {
			_interface.build(this);
		}

		return interfaces;
	}


	private IRI getMostSpecificRequirementNode(String requirementName, String ofNode) throws IOException {
		//IRI requirement = null;
		Set<IRI> nodeTypes = new HashSet<>();
		
		String sparql = MyUtils.fileToString("sparql/getMostSpecificRequirementNode.sparql");
		String query = KB.PREFIXES + sparql;

		// System.out.println(query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding[] { new SimpleBinding("node", kb.getFactory().createIRI(ofNode)),
						new SimpleBinding("requirementName", kb.getFactory().createLiteral(requirementName)) });
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI p1 = (IRI) bindingSet.getBinding("v").getValue();
			nodeTypes.add(p1);
			//requirement = p1;
		}
		result.close();
		
		System.out.println("getMostSpecificRequirementNode nodeTypes = " + nodeTypes);
		IRI nodeType = null;
		if (!nodeTypes.isEmpty())
			nodeType = InferencesUtil.getLowestSubclass(kb, nodeTypes);
		
		return nodeType;
	}

	public Set<Node> getRequirementValidNodes(String requirement, String nodeType, List<String> imports) throws IOException {
		System.out.println("getRequirementValidNodes: " + MyUtils.getFullResourceIRI(nodeType, kb));
		
		Set<Node> nodes = new HashSet<>();

		IRI node = this.getMostSpecificRequirementNode(requirement, MyUtils.getFullResourceIRI(nodeType, kb));
		if (node == null) {	
			return nodes;
		}
		System.out.println("getMostSpecificRequirementNode: " + node);
		
		
		String sparqlg = MyUtils.fileToString("sparql/getGlobalRequirementValidNodes.sparql");
		String queryg = KB.PREFIXES + sparqlg;
		
		System.out.println(queryg);

		TupleQueryResult resultg = QueryUtil.evaluateSelectQuery(kb.getConnection(), queryg, new SimpleBinding("var", node));
		_setNodes(resultg, nodes);
		
		if(!imports.isEmpty()) {
			String query = _getQueryTemplates(true, imports);
			if (!query.isEmpty()) {		
				System.out.println(query);
				TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
											new SimpleBinding("var", node));
				_setNodes(result, nodes);
			}
		}
		
		return nodes;
	}

	private String _getQueryTemplates(boolean ofType, List<String> imports) {
		String query = "";
		String dataset = QueryUtil.namedGraphsForQuery(kb, imports);
		if (!dataset.isEmpty()) {
			query = KB.PREFIXES + "SELECT ?node ?description ?superclass ?g\r\n" +
								"FROM <http://www.ontotext.com/implicit>\r\n" + 
								"FROM <http://www.ontotext.com/explicit>\r\n";
	
			query += dataset;
	
			query += " WHERE {\r\n" +
					"	?node a tosca:tosca.nodes.Root .\r\n";
			if (ofType)		
					query += "	?node a ?var .   \r\n";
			query += "	?node sesame:directType ?superclass . \r\n " +
					"   ?superclass rdfs:subClassOf tosca:tosca.nodes.Root . \r\n" +
					"	OPTIONAL {?node dcterms:description ?description .} \r\n";
			query += "        GRAPH ?g {\r\n" + 
					"            ?node soda:hasContext ?c\r\n" + 
					"        }\r\n";
				
			query += "}";	
		}
		
		return query;
	}
	
	private void _setNodes(TupleQueryResult result, Set<Node> nodes) throws IOException {
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI node = (IRI) bindingSet.getBinding("node").getValue();
			String description = bindingSet.hasBinding("description")
					? bindingSet.getBinding("description").getValue().stringValue()
					: null;
			IRI superclass = (IRI) bindingSet.getBinding("superclass").getValue();
			IRI _namespace = bindingSet.hasBinding("g") ? (IRI) bindingSet.getBinding("g").getValue() : null;

			Node n = new Node(node);
			n.setDescription(description);
			n.setType(superclass);
			n.setNamespace(_namespace);
			
			
			/*IRI namespace = InferencesUtil.getNamespaceFromType(kb, superclass);*/
			String _superNamespace = MyUtils.getNamespaceFromIRI(superclass.toString());
			if (MyUtils.validNamespace(kb, _superNamespace))
				n.setNamespaceOfType(kb.factory.createIRI(_superNamespace));

			nodes.add(n);
		}

		result.close();

	}

	/*
	 * Getting the valid requirement node types, so as the IDE to know which are the compatible node types
	 * for also proposing local applicable templates of the aadm
	 */
	public Set<NodeType> getRequirementValidNodeType(String requirement, String nodeType) throws IOException {
		System.out.println("getRequirementValidNodeTypes: " + MyUtils.getFullResourceIRI(nodeType, kb));
		
		Set<NodeType> nodeTypes = new HashSet<>();
		
		IRI node = getMostSpecificRequirementNode(requirement, MyUtils.getFullResourceIRI(nodeType, kb));
		if (node != null) {
			NodeType n = new NodeType(node);
			String _namespace = MyUtils.getNamespaceFromIRI(node.toString());
			if (MyUtils.validNamespace(kb, _namespace))
				n.setNamespace(kb.factory.createIRI(_namespace));
		
			nodeTypes.add(n);
		}


		return nodeTypes;	
	}
	
	public Set<Node> getTemplates(List<String> imports) throws IOException {
		Set<Node> nodes = new HashSet<>();
		
		String sparqlg = MyUtils.fileToString("sparql/getGlobalTemplates.sparql");
		String queryg = KB.PREFIXES + sparqlg;
		
		System.out.println(queryg);

		TupleQueryResult resultg = QueryUtil.evaluateSelectQuery(kb.getConnection(), queryg);
		_setNodes(resultg, nodes);
		
		if(!imports.isEmpty()) {
			String query = _getQueryTemplates(false, imports);
			if (!query.isEmpty()) {		
				System.out.println(query);
				TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query);
				_setNodes(result, nodes);
			}
		}
		
		return nodes;		
	}
	
	public String getDescription(IRI uri) {
		RepositoryResult<Statement> result = kb.getConnection().getStatements(uri,
				kb.factory.createIRI(KB.DCTERMS + "description"), null);
		if (result.hasNext()) {
			String val = result.next().getObject().stringValue();
			result.close();
			return val;
		}
		result.close();
		return null;
	}

	public Set<String> isSubClassOf(List<String> nodeTypes, String superNodeType) {
		System.out.println("isSubClassOf: nodeType = " + nodeTypes.toString() + ", superNodeType = " + MyUtils.getFullResourceIRI(superNodeType, kb));
		Set<String> _nodeTypes = new HashSet<>();
		
		for(String n: nodeTypes) {
			IRI nodeTypeIRI = kb.factory.createIRI(MyUtils.getFullResourceIRI(n, kb));
		
			String _superNodeType = MyUtils.getFullResourceIRI(superNodeType, kb);
		
			Set<String> superNodeTypeSet = new HashSet<>();
			superNodeTypeSet.add(_superNodeType);
			
			if (InferencesUtil.checkSubclassList(kb, nodeTypeIRI, superNodeTypeSet))
				_nodeTypes.add(n);
		}
		
		return _nodeTypes;
	}
	
	public Set<Parameter> getParameters(IRI classifier) {
		Set<Parameter> parameters = new HashSet<>();

		String query = KB.PREFIXES + "select ?parameter ?classifier ?value ?rootClassifier " + " where {"
				+ "		?var DUL:hasParameter ?classifier. " + "		OPTIONAL {?classifier tosca:hasValue ?value .} "
				+ " 	?classifier DUL:classifies ?parameter . " 
				+ "     ?var DUL:classifies ?rootClassifier ."+ "}";

		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("var", classifier));
		
		
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI _classifier = (IRI) bindingSet.getBinding("classifier").getValue();
			IRI _parameter = (IRI) bindingSet.getBinding("parameter").getValue();
			IRI _rootClassifier = (IRI) bindingSet.getBinding("rootClassifier").getValue();
			Value _value = bindingSet.hasBinding("value") ? bindingSet.getBinding("value").getValue() : null;

			String parameter = MyUtils.getStringValue(_parameter);
			String rootParameter = MyUtils.getStringValue(_rootClassifier);
			
		
			Parameter p = null;
			if (parameter.equals("content")) {
				if ((rootParameter.equals("file") || rootParameter.equals("primary"))) {
						/*implementation:
							primary:
								content: "script content" //not returned in aadm json
							dependencies:
								file: content: "script content" //not returned in aadm json
					 	*/
						
						if (_value != null) {
							String content = _value.toString();
						
							String fileUrl = null;
							try {
								fileUrl = MyFileUtil.uploadFile(content);
							} catch (IOException e) {
								e.printStackTrace();
							}
						
							Value fileUrlValue = null;
							if (fileUrl != null)
								fileUrlValue = kb.getFactory().createLiteral(fileUrl);
							
							//This parameter is not added to the KB model, it is only added to aadm json
							//e.g. url: "http://160.40.52.200:8084/Ansibles/b035b421-3aba-4cfb-b856-dfc473e5c71d"
							String ws = MyUtils.getNamespaceFromIRI(classifier.toString());
							p = new Parameter(kb.getFactory().createIRI( ws +"url"));
							p.setClassifiedBy(kb.getFactory().createIRI(ws + "ParamClassifier_" + MyUtils.randomString()));
							
							if (fileUrlValue != null)
								p.setValue(fileUrlValue, kb);
						}

				}
			} else if (parameter.equals("occurrences")){
				Map<String, String> limitsMap = _getOccurrencesLimits(_classifier);
				
				p = new Parameter(_parameter);
				p.setClassifiedBy(_classifier);	
				p.setValue(kb.getFactory().createLiteral("["+limitsMap.get("min")+"," + limitsMap.get("max") +"]"), kb);
				
			} else {
				p = new Parameter(_parameter);
				p.setClassifiedBy(_classifier);		
				if (_value != null) {
					System.err.println(_value);
					p.setValue(_value, kb);
				}
				
			}
			parameters.add(p);
		}
		result.close();			
		
		for (Parameter parameter : parameters) {
			parameter.setParameters(getParameters(parameter.getClassifiedBy()));
		}
		return parameters;
	}
	
	private Map<String, String> _getOccurrencesLimits(IRI classifier) {
		
		Map<String, String> limitsMap = new HashMap<String, String>();
		String query = KB.PREFIXES + "select ?parameter ?value " + " where {"
				+ "		?var DUL:hasParameter ?classifier. " + "		OPTIONAL {?classifier tosca:hasValue ?value .} "
				+ " 	?classifier DUL:classifies ?parameter . " 
				+ " }";
		
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("var", classifier));
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI _parameter = (IRI) bindingSet.getBinding("parameter").getValue();
			Value _value = bindingSet.hasBinding("value") ? bindingSet.getBinding("value").getValue() : null;
		
			String parameter = MyUtils.getStringValue(_parameter);
			if (_value != null)
				limitsMap.put(parameter, _value.toString());
		}
		
		return limitsMap;
	}

	public Set<IRI> getValidTargetTypes(String resource, boolean isTemplate) throws IOException {
		System.out.println("getValidTargetTypes: " + resource);
		
		Set<IRI> results = new HashSet<>();
		String sparql = MyUtils.fileToString(
				!isTemplate ? "sparql/getValidTargetTypes.sparql" : "sparql/getValidTargetTypesTemplate.sparql");
		String query = KB.PREFIXES + sparql;

		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("resource", kb.getFactory().createIRI(resource)));
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI value = (IRI) bindingSet.getBinding("value").getValue();
			results.add(value);
		}
		result.close();
		return results;
	}

	public Set<Operation> getOperations(String resource, boolean isTemplate) throws IOException {
		System.out.println("getOperations: " + resource);
		Set<Operation> operations = new HashSet<>();
		String sparql = MyUtils
				.fileToString(!isTemplate ? "sparql/getOperations.sparql" : "sparql/getOperationsTemplate.sparql");
		String query = KB.PREFIXES + sparql;

		// System.out.println(query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("resource", kb.getFactory().createIRI(resource)));

		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI p1 = (IRI) bindingSet.getBinding("property").getValue();
			IRI concept = (IRI) bindingSet.getBinding("concept").getValue();

			Operation op = new Operation(p1);
			op.setClassifiedBy(concept);
			operations.add(op);
		}
		result.close();
		for (Operation op : operations) {
			op.build(this);
		}

		return operations;

	}
	
	public Optimization getOptimization(String resource) throws IOException {
		Optimization optimization = null;
		String sparql = MyUtils
				.fileToString("sparql/getOptimization.sparql");
		String query = KB.PREFIXES + sparql;

		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("var", kb.getFactory().createLiteral(resource)));

		if (result.hasNext()) {
			BindingSet bindingSet = result.next();
			Value _opt = bindingSet.getBinding("opt_json").getValue();
			optimization = new Optimization(_opt.toString());
		}
		result.close();
		
		return optimization;
	}
	
	public Set<ValidationModel> getOptimizationSuggestions(String aadmId) throws IOException, ValidationException {
		System.out.println("getOptimizations aadmid = " + aadmId);
		Set<ValidationModel> templateOptimizations = new HashSet<>();
		HashMap<IRI, Set<String>> resourceOptimizations = new HashMap<IRI, Set<String>>();
		HashMap<IRI, String>  resourceOptimizationJson = new HashMap<IRI, String>();//templates -  optimization json pairs
		
		ApplicationTypeModel a = null;
		List<ValidationModel> errorModels = new ArrayList<>();
		String optimization_json = null;
		
		//List<String> capabilityList = Arrays.asList("ngpu", "ncpu", "memsize", "disksize", "arch");
		List<String> capabilityList = Arrays.asList("ngpu", "memsize", "arch");
		
		//Initialize app type compatible node types - In the future, there will be many node types, we should find ANOTHER SOLUTION
		HashMap<String, Set<String>> appTypes = new HashMap<String, Set<String>>();
		appTypes.put("hpc", new HashSet<String>(){{
		    add("my.nodes.hpc.wm.torque");
		    add("my.nodes.hpc.job.torque");
		}});
		appTypes.put("ai_training", new HashSet<String>(){{
		    add("sodalite.nodes.OpenStack.VM");
		    add("sodalite.nodes.DockerizedComponent");
		    add("sodalite.nodes.DockerHost");
		}});
		
		String sparql_r = MyUtils
				.fileToString("sparql/capabilities/getNodeTemplateCapabilities.sparql");
		String query_r = KB.PREFIXES + sparql_r;
		
		//Check which resources have capabilities
		TupleQueryResult result_r = QueryUtil.evaluateSelectQuery(kb.getConnection(), query_r, new SimpleBinding("var_aadm_id", kb.getFactory().createLiteral(aadmId)));
		
		while (result_r.hasNext()) {
			BindingSet bindingSet_r = result_r.next();
			IRI r = (IRI) bindingSet_r.getBinding("resource").getValue();
			optimization_json = MyUtils.getStringValue(bindingSet_r.getBinding("optimizations").getValue());
			IRI  capability_iri =  (IRI) bindingSet_r.getBinding("capability").getValue();
			IRI nodeType_iri = (IRI) bindingSet_r.getBinding("templateType").getValue();
			String  nodeType = MyUtils.getStringValue(nodeType_iri);
			
			resourceOptimizationJson.put(r, optimization_json);
			
			System.out.println("Querying for resource =" + r.toString() + ", optimizations = " + optimization_json + ", capability = " + capability_iri.toString());
			JsonObject jsonObject = JsonParser.parseString(optimization_json).getAsJsonObject();
			String app_type = jsonObject.getAsJsonObject("optimization").get("app_type").getAsString();
			String ai_framework = null;
			if	(app_type.equals("ai_training"))
				ai_framework = jsonObject.getAsJsonObject("optimization").getAsJsonObject("app_type-" + app_type).getAsJsonObject("config").get("ai_framework").getAsString();
			System.out.println("app_type= " + app_type + ", ai_framework=" + ai_framework);
			
			//Check app type
			if(!appTypes.containsKey(app_type)) {
				errorModels.add(new ApplicationTypeModel(app_type, r));
			} else {
				//Check if the derived node type is compatible with the app type in optimization json
				if(!appTypes.get(app_type).contains(nodeType)) {
					InferencesUtil.checkSubclassList(kb, nodeType_iri, appTypes.get(app_type));
					errorModels.add(new ApplicationTypeModel(app_type, r, nodeType_iri));
				}
			}
			if (!errorModels.isEmpty()) {
				throw new ValidationException(errorModels);
			}
			
			for (String capability : capabilityList) {
				String sparql = MyUtils
								.fileToString("sparql/capabilities/getNodeTemplate_"+ capability +".sparql");
				String query = KB.PREFIXES + sparql;
				
				TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
								new SimpleBinding("capability", kb.getFactory().createIRI(capability_iri.toString())));
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					Set<String> optimizations=null;
					String capability_value = bindingSet.hasBinding(capability) ? MyUtils.getStringValue(bindingSet.getBinding(capability).getValue()) : null;
					if (capability_value != null) {
						System.out.println("Querying for capability = " + capability +", capability value = " + capability_value);
						String opt_element = (ai_framework != null) ? ai_framework : app_type;
						optimizations = _getOptimizationSuggestions(capability, capability_value, opt_element);
						
						if (optimizations != null) {
							if (resourceOptimizations.get(r)!= null) {
								resourceOptimizations.get(r).addAll(optimizations);
							} else {
								resourceOptimizations.put(r,optimizations);
							}
						}
					}
				}
				result.close();
			}
		}
		result_r.close();
		
		System.out.println("\nOptimizations: ");
		resourceOptimizations.forEach((r,o)->{
			System.out.println("Resource : " + r + " Optimizations : " + o);
			HashMap<String,String> targetValue = new HashMap <String,String>();
			//Validation of the returned optimizations compared with the given optimization json in the aadm
			for (String opt: o) {
				JsonObject jo = JsonParser.parseString(opt).getAsJsonObject();
				//e.g. {"path":{ "app_type-ai_training": {"ai_framework-tensorflow":{}}}, "jsonelement": xla, "value": true}
				String path = jo.getAsJsonObject("path").toString();//the path where the optimization will be added
				String jsonelement = jo.getAsJsonPrimitive("jsonelement").getAsString();//the key(e.g. xla) to which "value" will be added
				String expectedValue = null;//the value that will be added
				//e.g. "true" is json primitive for jsonelement "xla"
				if (jo.getAsJsonObject().get("value").isJsonPrimitive())
					expectedValue =	jo.getAsJsonPrimitive("value").toString();
				else
					//e.g. { "prefetch": 100, "cache": 100 } is jsonobject for jsonelement "etl" 
					expectedValue =	jo.getAsJsonObject("value").toString();
				
				JsonObject targetJson = new JsonObject();
				try {
					List userOptValue = MyUtils.getValueFromJson(resourceOptimizationJson.get(r), jsonelement.replace("\"", ""));
					if (!userOptValue.isEmpty()) {
						
						String user_opt_value = userOptValue.get(0).toString();
						System.out.println("Resource = " + r.toString() + " has user optimization " + jsonelement + ":" + user_opt_value);
						if (BooleanUtils.toBooleanObject(user_opt_value) != null) {
							if (!userOptValue.contains(expectedValue)) {
								targetJson.add(jsonelement, JsonParser.parseString(expectedValue).getAsJsonPrimitive());
								//e.g. if given xla: false, but ngpus > 0, then xla: true, exception is thrown 
								templateOptimizations.add(new OptimizationMismatchModel(r, path, targetJson.toString(), user_opt_value, expectedValue));
							} else //the expected optimization is already included in the opt json. Do not include it
								continue;
						}
						else {
							if (!MyUtils.equals(user_opt_value, expectedValue.replace("\"", ""))) {
								targetJson.add(jsonelement, JsonParser.parseString(expectedValue).getAsJsonObject());
								//e.g. if given xla: false, but ngpus > 0, then xla: true, exception is thrown 
								templateOptimizations.add(new OptimizationMismatchModel(r, path, targetJson.toString(), user_opt_value, expectedValue
));
							} else //the expected optimization is already included in the opt json. Do not include it
								continue;
						}
					} else {
						if (BooleanUtils.toBooleanObject(expectedValue) != null) {
							targetJson.add(jsonelement, JsonParser.parseString(expectedValue).getAsJsonPrimitive());
						} else {
							targetJson.add(jsonelement, JsonParser.parseString(expectedValue).getAsJsonObject());
						}
						targetValue.put(path, targetJson.toString());
					}			
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
			}
			if(!targetValue.isEmpty()) {
				OptimizationModel to = new OptimizationModel(r,targetValue);
				templateOptimizations.add(to);
			}
		});
		
		return templateOptimizations;
	}
	
	// This function is reasoning over optimization ontology for returning the applicable
	// optimizations according to the app_type/framework and capabilities
	private Set<String> _getOptimizationSuggestions (String capability, String capability_value, String opt_concept) throws IOException {
		String sparql = MyUtils
				.fileToString("sparql/optimization/getFrameworkOptimizations_" + capability + ".sparql");
		String query = KB.OPT_PREFIXES + sparql;
		
		if (Arrays.asList("memsize", "disksize").contains(capability))
			capability_value = MyUtils.getStringPattern(capability_value, "([0-9]+).*");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding[] { new SimpleBinding("var_1", kb.getFactory().createLiteral(opt_concept)),
						new SimpleBinding("var_2", kb.getFactory().createLiteral(capability_value))});
				
		Set <String> optimizations= new HashSet<String>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			String _opt = MyUtils.getStringValue(bindingSet.getBinding("optimization").getValue());
			optimizations.add(_opt.toString());
		}
		result.close();
		return optimizations.isEmpty() ? null : optimizations;
	}
	
	
	public AADM getAADM(String aadmId) throws IOException {
		System.out.println("AADM: " + aadmId);
		String sparql = MyUtils.fileToString("sparql/getAADM.sparql");
		String query = KB.PREFIXES + sparql;

		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("aadm", kb.getFactory().createIRI(aadmId)));

		AADM aadm = null;
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			Value createdAt = bindingSet.getBinding("createdAt").getValue();
			IRI user = (IRI) bindingSet.getBinding("user").getValue();
			String templates = bindingSet.getBinding("templates").getValue().stringValue();
			String inputs = bindingSet.getBinding("inputs").getValue().stringValue();

			aadm = new AADM(kb.getFactory().createIRI(aadmId));
			aadm.setUser(user);
			aadm.setCreatedAt(ZonedDateTime.parse(createdAt.stringValue()));

			String[] split = templates.split(" ");
			for (String s : split) {
				String[] split2 = s.split("\\|");
				boolean isInput = split2[1].endsWith("Input");
				NodeFull f = new NodeFull(kb.getFactory().createIRI(split2[0]), true && !isInput);
				f.setType(kb.getFactory().createIRI(split2[1]));
				aadm.addTemplate(f);
			}

			if (!Strings.isNullOrEmpty(inputs)) {
				split = inputs.split(" ");
				for (String s : split) {
					String[] split2 = s.split("\\|");
					NodeFull f = new NodeFull(kb.getFactory().createIRI(split2[0]), false);
					f.isInput = true;
					f.setType(kb.getFactory().createIRI(split2[1]));
					aadm.addTemplate(f);
				}
			}

//			aadm.setTemplates(
//					Arrays.stream(templates.split(" ")).map(x -> kb.getFactory().createIRI(x)).map(x -> new NodeFull(x))
//							.collect(Collectors.toSet()));

		}
		result.close();

		if (aadm != null) {
			aadm.build(this);
		} else {
			System.err.println("AADM is null");
		}

		return aadm;
	}

	public Set<SodaliteAbstractModel> getModels(String type, String namespace) throws IOException {
		System.out.println(String.format("getModels for %s type, %s namespace", type, namespace));
		Set<SodaliteAbstractModel> models = new HashSet<>();
		
		String sparql = "PREFIX soda: <https://www.sodalite.eu/ontologies/sodalite-metamodel/> \r\n" +
						"PREFIX DUL: <http://www.loa-cnr.it/ontologies/DUL.owl#> \r\n";
		if (type.equals("AADM"))
			sparql += MyUtils.fileToString("".equals(namespace) ? "sparql/models/getGlobalAADMModels.sparql" : "sparql/models/getNamedAADMModels.sparql");
		else if (type.equals("RM"))
			sparql += MyUtils.fileToString("".equals(namespace)  ? "sparql/models/getGlobalResourceModels.sparql" : "sparql/models/getNamedResourceModels.sparql");
		
		String query = sparql;
		
		System.out.println(query);

		
		TupleQueryResult result = null;
		if 	("".equals(namespace) )
			result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query);
		else 
			result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("g", kb.getFactory().createIRI(namespace)));
			
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI model = (IRI) bindingSet.getBinding("m").getValue();
			Value createdAt = bindingSet.getBinding("time").getValue();
			IRI user = (IRI) bindingSet.getBinding("user").getValue();
			Value dsl = bindingSet.getBinding("dsl").getValue();
			Value name = bindingSet.getBinding("name").getValue();
			
			SodaliteAbstractModel a = new SodaliteAbstractModel(model);
			a.setUser(user);
			a.setCreatedAt(ZonedDateTime.parse(createdAt.stringValue()));
			a.setDsl(dsl.toString());
			a.setName(name.toString());
			
			models.add(a);
		}
		
		result.close();
		return models;
	}
	
	public SodaliteAbstractModel getModelForResource(String resource, String namespace) throws IOException {
		System.out.println(String.format("getModels for %s resource, %s namespace", resource, namespace));
		SodaliteAbstractModel a = null;
		
		String sparql = "";
		String query;
		TupleQueryResult result = null;
		if ("".equals(namespace)) {
			sparql += MyUtils.fileToString("sparql/models/getModelFromGlobalResource.sparql");
			query = KB.SODA_DUL_PREFIXES + sparql;
			System.out.println(query);
			result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
														new SimpleBinding("name", kb.getFactory().createLiteral(resource)));
		} else {
			sparql += MyUtils.fileToString("sparql/models/getModelFromNamedResource.sparql");
			query = KB.SODA_DUL_PREFIXES + sparql;
			System.out.println(query);
			result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding[] { new SimpleBinding("g", kb.getFactory().createIRI(namespace)),
					new SimpleBinding("name", kb.getFactory().createLiteral(resource))});
			
		}
			
		if (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI model = (IRI) bindingSet.getBinding("m").getValue();
			Value createdAt = bindingSet.getBinding("time").getValue();
			IRI user = (IRI) bindingSet.getBinding("user").getValue();
			Value dsl = bindingSet.getBinding("dsl").getValue();
			Value name = bindingSet.getBinding("name").getValue();
			
			a = new SodaliteAbstractModel(model);
			a.setUser(user);
			a.setCreatedAt(ZonedDateTime.parse(createdAt.stringValue()));
			a.setDsl(dsl.stringValue());
			a.setName(name.stringValue());
		}
		
		return a;
	}
	
	public SodaliteAbstractModel getModelFromURI (String uri) throws IOException {
		System.out.println(String.format("getModelFromURI for %s uri", uri));
		SodaliteAbstractModel a = null;
		
		String sparql = MyUtils.fileToString("sparql/models/getModel.sparql");
		String query = KB.SODA_DUL_PREFIXES + sparql;
		System.out.println(query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, 
										new SimpleBinding("m", kb.getFactory().createIRI(uri)));
		
			
		if (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI model = (IRI) bindingSet.getBinding("m").getValue();
			Value createdAt = bindingSet.getBinding("time").getValue();
			IRI user = (IRI) bindingSet.getBinding("user").getValue();
			Value dsl = bindingSet.getBinding("dsl").getValue();
			Value name = bindingSet.getBinding("name").getValue();
			
			a = new SodaliteAbstractModel(model);
			a.setUser(user);
			a.setCreatedAt(ZonedDateTime.parse(createdAt.stringValue()));
			a.setDsl(dsl.stringValue());
			a.setName(name.stringValue());
		}
		
		return a;
	}
	
	public void deleteModel (String uri) {
		System.out.println(String.format("deleteModel, uri = %s ",  uri));
		
		String query = "PREFIX soda: <https://www.sodalite.eu/ontologies/sodalite-metamodel/> \r\n" +
				"PREFIX DUL: <http://www.loa-cnr.it/ontologies/DUL.owl#> \r\n";
		
		query += "select ?x\r\n" +
						"{\r\n" + 
						"	{\r\n" + 
						"		?m a soda:ResourceModel .\r\n" + 
						"	} UNION {\r\n" + 
						"			?m a soda:AbstractApplicationDeployment .\r\n" + 
						"	}	\r\n" + 
						"	?m soda:includesNodeType|soda:includesRelationshipType|soda:includesDataType|soda:includesCapabilityType|soda:includesTemplate|soda:includesInput ?x .\r\n" + 
						"}";
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("m", kb.getFactory().createIRI(uri)));
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI node = (IRI) bindingSet.getBinding("x").getValue();
			
			new ModifyKB(kb).deleteNode(node);
		}
		
		result.close();		
	}
		
//	public Set<Constraint> getConstraints(IRI concept) throws IOException {
//		System.err.println("getConstraints " + concept);
//
//		Set<Constraint> constraints = new HashSet<>();
//
//		String sparql = MyUtils.fileToString("sparql/getConstraints.sparql");
//		String query = PREFIXES + sparql;
//
//		System.out.println(query);
//		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
//				new SimpleBinding("var1", concept));
//
//		while (result.hasNext()) {
//			BindingSet bindingSet = result.next();
//
//			IRI type = (IRI) bindingSet.getBinding("type").getValue();
//			Value value = bindingSet.getBinding("value").getValue();
//
//			Constraint con = new Constraint();
//			con.setType(type.getLocalName());
//
//			if (Literals.getLabel(value, null) != null) {
//				con.setValue(value.stringValue());
//			} else {
//				// TODO
//			}
//
//			constraints.add(con);
//
//		}
//		result.close();
//		System.out.println("no constraints");
//		return constraints.isEmpty() ? null : constraints;
//
//	}

	public static void main(String[] args) throws IOException {
//		RecommendationService service = new RecommendationService();
//
//		Set<String> requirements = service.getInterfaces("tosca.nodes.Root");
//		System.out.println(requirements);
//
//		service.shutDown();

		KBApi a = new KBApi();

//		Set<Parameter> parameters = a.getParameters(
//				a.kb.factory.createIRI("https://www.sodalite.eu/ontologies/tosca/CPU_FrequencyProperty"));
//		System.out.println(parameters);

		AADM aadm = a.getAADM(
				"https://www.sodalite.eu/ontologies/snow-blueprint-containerized-OS/AbstractApplicationDeployment_1");
		System.err.println(aadm);

	}

}
