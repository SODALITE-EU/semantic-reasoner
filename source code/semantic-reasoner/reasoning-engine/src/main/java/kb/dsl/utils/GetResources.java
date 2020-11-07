package kb.dsl.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.dsl.DSLMappingService;
import kb.repository.KB;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;

public class GetResources {

	public static IRI getKBNodeType(NamedResource n, String type, KB kb) {
		System.out.println("getKBNodeType label = " + n.getResource() + ", type = " + type);
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
	
	public static IRI getKBProperty(String label, List<String> namespaces, KB kb) {
		
		String sparql = "select distinct ?x \r\n" +
						"FROM <http://www.ontotext.com/explicit>\r\n";
		
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
	
	
	public static List<String> getInheritedNamespacesFromType(KB kb, String type) {
		System.out.println("getInheritedNamespacesFromType type =" + type);
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
		
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
									new SimpleBinding("x", kb.getFactory().createIRI(type)));
		
		System.out.println(query);
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI g = (IRI) bindingSet.getBinding("g").getValue();
			namespacesOfType.add(g.toString());
		}
		
		return namespacesOfType;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
