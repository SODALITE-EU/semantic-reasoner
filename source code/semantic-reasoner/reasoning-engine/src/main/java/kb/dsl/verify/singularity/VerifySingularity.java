package kb.dsl.verify.singularity;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.clean.ModifyKB;
import kb.repository.KB;
import kb.utils.QueryUtil;

public class VerifySingularity {
	
	public static void removeExistingDefinitions (KB kb, Set<String> nodeTypes, String namespace) throws IOException {
		System.out.println("removeExistingTypes = " + QueryUtil.convertToSPARQLList(nodeTypes));
		
		String sparql = "select ?m (group_concat(?x) AS ?nodes) { \r\n" + 
						"    	?m a ?ModelType .\r\n" + 
						"		FILTER (?ModelType  IN (soda:AbstractApplicationDeployment, soda:ResourceModel)) .\r\n" + 
						"		?m DUL:isSettingFor ?x .\r\n";
		
		if (namespace.contains("global")) {
			sparql +=	"    	{\r\n" + 
						"          ?x soda:hasName ?name .\r\n" + 
						"          FILTER NOT EXISTS\r\n" + 
						"	      { \r\n" + 
						"			 GRAPH ?g { ?x ?p ?o }\r\n" + 
						"		  }\r\n" + 
						"       } \r\n";
		} else {
			if (!namespace.contains("global"))
				sparql +=	"     {\r\n" + 
							"        GRAPH " + "<"+ namespace.toString() + ">\r\n" +
							"         {\r\n" +
							"			?x soda:hasName ?name .\r\n" + 
							"		   }\r\n" + 
							"     }\r\n";
		}
		
		sparql +=	"	VALUES ?name {" + QueryUtil.convertToSPARQLList(nodeTypes) + "}.\r\n" +
					"} group by ?m";
		

		String query = KB.PREFIXES + sparql;
		System.out.println("removeExistingTypes  query = \n" + query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query);
		
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI model = (IRI) bindingSet.getBinding("m").getValue();
			String nodes = (String) bindingSet.getBinding("nodes").getValue().stringValue();
		
			System.out.format("Resource model = %s, nodes = %s\n", model , nodes);
			
			String[] nsplit = nodes.split(" ");
			
			long startTime = Instant.now().toEpochMilli();
			System.out.println("VerifySingularity model = " + model + ", nodes = " + nodes);
			for (String n: nsplit) {

				new ModifyKB(kb).deleteNode(kb.factory.createIRI(n));
			}
			long endTime = Instant.now().toEpochMilli();
			long timeElapsed = endTime - startTime;
			System.out.println("Delete nodes execution time in milliseconds: " + timeElapsed);
		}
		
		result.close();
						
	}

	public static void removeInputs(KB kb, String aadmURI) {
		String sparql = "\r\nselect ?resource\r\n" + 
						"where {\r\n" + 
						"    ?m soda:includesInput ?resource .\r\n" + 
						"}";
		
		String query = KB.SODA_DUL_PREFIXES + sparql;
		System.out.println("removeInputs  query = " + query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, 
										new SimpleBinding("m", kb.getFactory().createIRI(aadmURI)));
		
		if (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI input = (IRI) bindingSet.getBinding("resource").getValue();
			
			new ModifyKB(kb).deleteNode(input);
		}
		
	}
	
	public static void main(String[] args) {

	}

}
