package kb.dsl.verify.singularity;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.clean.ModifyKB;
import kb.repository.KB;
import kb.utils.QueryUtil;

public class VerifySingularity {
	private static final Logger LOG = LoggerFactory.getLogger(VerifySingularity.class.getName());
	private VerifySingularity() {
	}
	
	public static void removeExistingDefinitions (KB kb, Set<String> nodeTypes, String namespace, IRI modelIRI) throws IOException {
		LOG.info("removeExistingTypes = {}", QueryUtil.convertToSPARQLList(nodeTypes));
		
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
		LOG.info("removeExistingTypes query = {}", query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query);
		
		//Refresh model metadata such as time, user
		if(result.hasNext())
			new ModifyKB(kb).deleteModelMetadata(modelIRI);
		
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI model = (IRI) bindingSet.getBinding("m").getValue();
			String nodes = (String) bindingSet.getBinding("nodes").getValue().stringValue();
					
			String[] nsplit = nodes.split(" ");
			
			long startTime = Instant.now().toEpochMilli();
			LOG.info("VerifySingularity model = {}, nodes = {}\n", model , nodes);
			for (String n: nsplit) {

				new ModifyKB(kb).deleteNode(kb.factory.createIRI(n));
			}
			long endTime = Instant.now().toEpochMilli();
			long timeElapsed = endTime - startTime;
			LOG.info("Delete nodes execution time in milliseconds: {}\n", timeElapsed);
		}
		
		result.close();	
	}

	public static void removeInputs(KB kb, String aadmURI) {
		String sparql = "\r\nselect ?resource\r\n" + 
						"where {\r\n" + 
						"    ?m soda:includesInput|soda:includesOutput ?resource .\r\n" + 
						"}";
		
		String query = KB.SODA_DUL_PREFIXES + sparql;
		LOG.info("removeInputsOutputs  query = {}\n", query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, 
										new SimpleBinding("m", kb.getFactory().createIRI(aadmURI)));
		
		if (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI input = (IRI) bindingSet.getBinding("resource").getValue();
			
			new ModifyKB(kb).deleteNode(input);
		}

		result.close();
	}

}
