package kb.dsl.verify.singularity;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.clean.ModifyKB;
import kb.repository.KB;
import kb.utils.QueryUtil;

public class VerifySingularity {
	private VerifySingularity() {
	}
	
	private static final Logger LOG = Logger.getLogger(VerifySingularity.class.getName());
	
	public static void removeExistingDefinitions (KB kb, Set<String> nodeTypes, String namespace) throws IOException {
		LOG.log(Level.INFO, "removeExistingTypes = {0}", QueryUtil.convertToSPARQLList(nodeTypes));
		
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
		LOG.log(Level.INFO, "removeExistingTypes query = {0}", query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query);
		
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI model = (IRI) bindingSet.getBinding("m").getValue();
			String nodes = (String) bindingSet.getBinding("nodes").getValue().stringValue();
					
			String[] nsplit = nodes.split(" ");
			
			long startTime = Instant.now().toEpochMilli();
			LOG.log(Level.INFO, "VerifySingularity model = {0}, nodes = {1}\n", new Object[] {model , nodes});
			for (String n: nsplit) {

				new ModifyKB(kb).deleteNode(kb.factory.createIRI(n));
			}
			long endTime = Instant.now().toEpochMilli();
			long timeElapsed = endTime - startTime;
			LOG.log(Level.INFO, "Delete nodes execution time in milliseconds: {0}\n", timeElapsed);
		}
		
		result.close();	
	}

	public static void removeInputs(KB kb, String aadmURI) {
		String sparql = "\r\nselect ?resource\r\n" + 
						"where {\r\n" + 
						"    ?m soda:includesInput ?resource .\r\n" + 
						"}";
		
		String query = KB.SODA_DUL_PREFIXES + sparql;
		LOG.log(Level.INFO, "removeInputs  query = {0}\n", query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, 
										new SimpleBinding("m", kb.getFactory().createIRI(aadmURI)));
		
		if (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI input = (IRI) bindingSet.getBinding("resource").getValue();
			
			new ModifyKB(kb).deleteNode(input);
		}
		
	}

}
