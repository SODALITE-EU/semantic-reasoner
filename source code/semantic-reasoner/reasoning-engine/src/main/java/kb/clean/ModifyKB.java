package kb.clean;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.configs.ConfigsLoader;
import kb.repository.KB;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;

public class ModifyKB {
	private static final Logger LOG = LoggerFactory.getLogger(ModifyKB.class.getName());
		
	public KB kb;
	static ConfigsLoader configInstance = ConfigsLoader.getInstance();
	static {
		configInstance.loadProperties();
	}
		
	Model model;
	
	//modify this query so as specific property to be returned in CONSTRUCT query
	String queryModel = KB.PREFIXES + " CONSTRUCT { ?r soda:includesType|soda:includesTemplate ?s }\r\n" +
						"WHERE\r\n" +
						"{?r soda:includesType|soda:includesTemplate ?s }";
	String query = KB.PREFIXES + " CONSTRUCT { ?s ?p ?o }\r\n" +
					"WHERE\r\n" +
					"{?s ?p ?o }";

	public static final List<String> propertyList = Arrays.asList(new String [] {
		        KB.TOSCA + "properties", KB.TOSCA + "requirements", KB.TOSCA + "interfaces", KB.TOSCA + "attributes", KB.TOSCA + "input",  KB.TOSCA + "capabilities"
		        , KB.TOSCA + "triggers", KB.TOSCA + "targets"});
	
	public ModifyKB(KB kb) {
		model = new LinkedHashModel();
		this.kb = kb;
	}

	public void deleteNode(IRI node) {
		LOG.info("deleteNode {}", node);
		GraphQueryResult gresultM = QueryUtil.evaluateConstructQuery(kb.getConnection(), queryModel,
				new SimpleBinding("s", node));
		
		Model mresult = QueryResults.asModel(gresultM);
		displayModel(mresult);
		
		Set<IRI> models = Models.subjectIRIs(mresult);
		
		addModel(mresult);
						
		gresultM.close();
			
		GraphQueryResult gresult = QueryUtil.evaluateConstructQuery(kb.getConnection(), query,
					new SimpleBinding("s", node));

		if (gresult.hasNext()) {
			Model result = QueryResults.asModel(gresult);
			gresult.close();
			addModel(result);

			Optional<Resource> _context = Models.getPropertyResource(result, node ,kb.factory.createIRI(KB.SODA + "hasContext"));
			IRI context = null;
			if (_context.isPresent()) {
				context = (IRI) _context.get();
			
			GraphQueryResult gresult2 = QueryUtil.evaluateConstructQuery(kb.getConnection(), query,
											new SimpleBinding("s", context));
				
			Model result2 = QueryResults.asModel(gresult2);
			addModel(result2);
			
			gresult2.close();
			
			for (String p : propertyList) {
					Set<Resource> _classifiers = Models.getPropertyResources(result2, context , kb.factory.createIRI(p));

					for (Resource _c : _classifiers) {
						IRI classifier = (IRI) _c;
				
						GraphQueryResult gresult3 = QueryUtil.evaluateConstructQuery(kb.getConnection(), query,
															new SimpleBinding("s", classifier));
						Model result3 = QueryResults.asModel(gresult3);
					
						addModel(result3);				
						Set<String> _datavalue = Models.getPropertyStrings(result3, classifier , kb.factory.createIRI(KB.TOSCA + "hasDataValue"));
						if (!_datavalue.isEmpty()) {
							continue;
						}
					
						Optional<Resource> _objectvalue = Models.getPropertyResource(result3, classifier , kb.factory.createIRI(KB.TOSCA + "hasObjectValue"));
						if (_objectvalue.isPresent()) {
							continue;
						}
					
						Set<Resource> _parameters = Models.getPropertyResources(result3, classifier , kb.factory.createIRI(KB.DUL + "hasParameter"));
						
						for (Resource _p : _parameters) {
							parameterModel(_p);
						}
					
						gresult3.close();			
					}
				}
			
			}
		}
					
		kb.connection.remove(model);
		
		deleteEmptyModels(models);
	}
		
		
	public void addModel(Model result) {
		for (Statement st: result) {
			model.add(st);
		}	
	}
		
	public void parameterModel(Resource p_r) {
		IRI p = (IRI) p_r;
		GraphQueryResult gresult =  QueryUtil.evaluateConstructQuery(kb.getConnection(), query, new SimpleBinding("s", p));
			
		Model result = QueryResults.asModel(gresult);		
		gresult.close();
		addModel(result);
			
		Set<Literal> _datavalue = Models.getPropertyLiterals(result, p , kb.factory.createIRI(KB.TOSCA + "hasDataValue"));
		if (!_datavalue.isEmpty()) {
			return;
		}
			
		Optional<Resource> _objectvalue = Models.getPropertyResource(result, p , kb.factory.createIRI(KB.TOSCA + "hasObjectValue"));
			
		if (_objectvalue.isPresent()) {
			return;
		}
			
			
		Set<Resource> _parameters = Models.getPropertyResources(result, p , kb.factory.createIRI(KB.DUL + "hasParameter"));
		for (Resource _p : _parameters) {
			parameterModel(_p);
		}
	}
	
	public void deleteEmptyModels(Set<IRI> models) {
		for (IRI m:models) {
			GraphQueryResult gresultM = QueryUtil.evaluateConstructQuery(kb.getConnection(), queryModel, new SimpleBinding("r", m));
		
			if (!gresultM.hasNext()) {
				LOG.info("delete empty models = {}", models);
				GraphQueryResult gresult =  QueryUtil.evaluateConstructQuery(kb.getConnection(), query, new SimpleBinding("s", m));
				
				Model result = QueryResults.asModel(gresult);
				displayModel(result);
				kb.connection.remove(result);
				
				gresult.close();
			} 			
			gresultM.close();
		}
	}
	
	public void displayModel(Model model) {
		for (Statement st : model) {
			//%-50s %-20s %-40s
			LOG.info("{} {} {}",MyUtils.getStringValue(st.getSubject()), MyUtils.getStringValue(st.getPredicate()), st.getObject().toString());
		}
	}
	
	public void deleteNodes(Set<IRI> nodes) {
		for (IRI n: nodes) {
			deleteNode(n);
		}
	}
	
	public boolean deleteModel(String modelUri) {
		String queryDM = KB.SODA_DUL_PREFIXES;
		
		queryDM += "select ?x\r\n" +
						"{\r\n" + 
						"\t{\r\n" + 
						"\t\t?m a soda:ResourceModel .\r\n" + 
						"\t} UNION {\r\n" + 
						"\t\t?m a soda:AbstractApplicationDeployment .\r\n" + 
						"\t}\r\n" + 
						"\t?m soda:includesType|soda:includesTemplate|soda:includesInput ?x .\r\n" + 
						"}";
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), queryDM, new SimpleBinding("m", kb.getFactory().createIRI(modelUri)));
		
		LOG.info(queryDM);
		
		Set<IRI> nodes = new HashSet<>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI node = (IRI) bindingSet.getBinding("x").getValue();			
			nodes.add(node);
		}
		
		if (nodes.isEmpty()) {
			result.close();
			return false;
		}
		
		deleteNodes(nodes);
		
		result.close();
		return true;
	}
	
	//Delete some modelMetadata such as createdAt, so as to be refereshed with new data
	public void deleteModelMetadata(IRI modelIRI) throws IOException {
		LOG.info("deleteModelMetadata {}", modelIRI);
		
		String sparql = MyUtils.fileToString("sparql/clean/getAADMMetadataStatements.sparql");
		String queryd = KB.PREFIXES + sparql;
		GraphQueryResult gresultM = QueryUtil.evaluateConstructQuery(kb.getConnection(), queryd,
				new SimpleBinding("s", modelIRI));
		
		Model mresult = QueryResults.asModel(gresultM);
		kb.connection.remove(mresult);
		gresultM.close();
			
	}
		
	public static void main(String[] args) {
		KB kb = new KB(configInstance.getGraphdb(), "TOSCA");
		IRI n = kb.getFactory().createIRI("https://www.sodalite.eu/ontologies/workspace/1/global/sodalite.image_puller.singularity");
			new ModifyKB(kb).deleteNode(n);
	}

}

