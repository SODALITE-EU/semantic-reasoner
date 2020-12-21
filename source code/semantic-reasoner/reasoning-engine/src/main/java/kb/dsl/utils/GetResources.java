package kb.dsl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.repository.KB;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
/* All the utility functions used by DSL Mapping services.*/

public class GetResources {
	
	private static final Logger LOG = Logger.getLogger(GetResources.class.getName());

	public static IRI getKBNodeType(NamedResource n, String type, KB kb) {
		LOG.info("getKBNodeType label = " + n.getResource() + ", type = " + type);
		String namespace = n.getNamespace();
		String resource = n.getResource();
		
		String sparql = "select ?x { \r\n" +
				"  {\r\n " +
				"    ?x rdf:type owl:Class .\r\n" +
				"   FILTER NOT EXISTS\r\n" +
				"   {\r\n" +
				"     GRAPH ?g { ?x ?p ?o } \r\n" +
				"   }\r\n" +
				"  }\r\n";

		if (namespace != null && !namespace.contains("global"))
			sparql += 	" UNION {\r\n" +
						"     GRAPH " + "<"+ namespace + ">\r\n" +
						"     {\r\n" +
						"	     ?x rdf:type owl:Class . \r\n" +
						"     }\r\n" +
						" }\r\n";

		sparql += 	" ?x rdfs:subClassOf " + type + " .\r\n" +
					" FILTER (strends(str(?x), \"" + resource + "\")). \r\n" +
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
	
	public static IRI getKBProperty(String label, List<String> namespaces, KB kb) {
		LOG.info("getKBProperty label = " + label + ", namespaces = " + namespaces);
		
		String sparql = "select distinct ?x \r\n" +
						"FROM <http://www.ontotext.com/explicit>\r\n" +
						"FROM <http://www.ontotext.com/implicit>\r\n";
		
		for (String n: namespaces) {
			String nIRI = MyUtils.getFullNamespaceIRI(kb, n);
			if(nIRI != null)
				sparql += "FROM NAMED <" + nIRI + ">\r\n";
		}
		
		sparql += "{  " +
					"  {\r\n" +
					"     ?x a rdf:Property . " +
					"     FILTER (strends(str(?x), \"/" + label + "\")). \r\n" +
					"  }";
		
		if (namespaces.size() != 0)
			sparql +=   "  UNION {\r\n" +
					    "    	 GRAPH " + "?g\r\n" +
					    "    	 { ?x a rdf:Property . FILTER (strends(str(?x), \"/" + label + "\")). " +
					    "    	 }\r\n" +
					    "  }\r\n";
		sparql += 	"}";
			
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
	

	
	public static NamedResource setNamedResource (String namespace, String value) {
		NamedResource n = new NamedResource();
		
		String namespaceRef = MyUtils.getNamespaceFromReference(value);
		String resource = value;
		if (namespaceRef != null) {
			namespaceRef = namespace + namespaceRef + "/";
			resource = MyUtils.getReferenceFromNamespace(resource);
			n.setResourceURI(namespaceRef + resource);
		}
		
		n.setNamespace(namespaceRef);
		n.setResource(resource);
		return n;
	}
	
	/* If there are more than one level of custom node types, then all the inherited namespaces
	 * should be searched. For example, prometheus-container is of vehicleiot/sodalite.nodes.PrometheusDockerizedComponent type
	 * which is in turn is of docker/sodalite.nodes.DockerizedComponent type. Both vehicleiot and docker namespaces
	 * should be searched in Dsl Mapping for property names.
	 * type: contains the direct type of the template. e.g. vehicleiot/sodalite.nodes.PrometheusDockerizedComponent
	 * the List of all the inherited namespaces is returned e.g. [docker, vehicleiot]
	 */
	public static List<String> getInheritedNamespacesFromType(KB kb, String type) {
		LOG.info("getInheritedNamespacesFromType type =" + type);
		List<String> namespacesOfType = new ArrayList<String>();
		String query = KB.PREFIXES +
						" select ?g { \r\n" + 
						"	?x rdfs:subClassOf tosca:tosca.entity.Root .\r\n" + 
						"	?x rdfs:subClassOf ?superclass .\r\n" + 
						"    \r\n" + 
						"	GRAPH ?g {\r\n" + 
						"		?superclass soda:hasContext ?c .\r\n" + 
						"	}\r\n" + 
						"}";
		LOG.info(query);
		
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
									new SimpleBinding("x", kb.getFactory().createIRI(type)));
		

		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI g = (IRI) bindingSet.getBinding("g").getValue();
			namespacesOfType.add(g.toString());
		}
		
		result.close();
		return namespacesOfType;
	}
	
	
	/* Triggers in policies contain event filters where requirement and capability
	 * point within a requirement or capability name of the requirements/node
	 * target_filter:              
		node: <node_type>/<node_template> 
		requirement: <req_name> 
		capability: <req_name>/<cap_name>
		
		if requirement: host, e.g. https://www.sodalite.eu/ontologies/workspace/1/radon/host is returned
	 */
	public static IRI getReqCapFromEventFilter(KB kb, String event_filter_req_cap) {
		LOG.info("getReqCapFromEventFilter:");
		
		String req_cap = MyUtils.getStringPattern(event_filter_req_cap, ".*\\.([A-Za-z]*)$");
		String resource= MyUtils.getStringPattern(event_filter_req_cap, "(.*)\\.[A-Za-z]*$");
		
		String resource_iri = MyUtils.getFullResourceIRI(resource, kb);
		
		LOG.info("req_cap = " + req_cap + ", resource = " + resource + ", resource_iri = " + resource_iri);
		
		String sparql = "select ?requirement \r\n" + 
				"where {\r\n" + 
				"	?resource soda:hasContext ?context .\r\n" + 
				"	?context tosca:requirements|tosca:capabilities ?classifier.\r\n" + 
				"	?classifier DUL:classifies ?requirement .\r\n" + 
				"    filter(regex(str(?requirement), \"" + req_cap  + "$\", \"i\")) .\r\n" + 
				"}";
		
		String query = KB.PREFIXES + sparql;
		LOG.info(query);
		
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
									new SimpleBinding("resource", kb.getFactory().createIRI(resource_iri)));
		IRI requirement = null;
		if (result.hasNext()) {
			BindingSet bindingSet = result.next();
			requirement = (IRI) bindingSet.getBinding("requirement").getValue();
		}
				
		result.close();
		return requirement;		
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
