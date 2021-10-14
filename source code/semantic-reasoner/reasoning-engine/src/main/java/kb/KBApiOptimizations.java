package kb;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kb.configs.ConfigsLoader;
import kb.dto.AADM;
import kb.dto.NodeFull;
import kb.repository.KB;
import kb.repository.KBConsts;
import kb.utils.InferencesUtil;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
import kb.validation.exceptions.models.optimization.ApplicationTypeModel;
import kb.validation.exceptions.models.optimization.OptimizationMismatchModel;
import kb.validation.exceptions.models.optimization.OptimizationModel;

public class KBApiOptimizations {
	private static final Logger LOG = LoggerFactory.getLogger(KBApiOptimizations.class.getName());

	public KB kb;
	static ConfigsLoader configInstance = ConfigsLoader.getInstance();
	static {
		configInstance.loadProperties();
	}
	
	public KBApiOptimizations() {
		kb = new KB(configInstance.getGraphdb(), KB.REPOSITORY);
	}
	
	public KBApiOptimizations(KB kb) {
		this.kb = kb;
	}

	public void shutDown() {
		kb.shutDown();
	}
	
	public Set<ValidationModel> getOptimizationSuggestions(IRI aadmUri) throws IOException, ValidationException {
		LOG.info("getOptimizations aadmUri = {}", aadmUri.stringValue());
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
		Set<String> hpcTypes = new HashSet<String>();
		Set<String> AITypes = new HashSet<String>();
		hpcTypes.add("my.nodes.hpc.wm.torque");
		hpcTypes.add("my.nodes.hpc.job.torque");
		AITypes.add("sodalite.nodes.OpenStack.VM");
		AITypes.add("sodalite.nodes.DockerizedComponent");
		AITypes.add("sodalite.nodes.DockerHost");
		appTypes.put("hpc", hpcTypes);
		appTypes.put("ai_training", AITypes);
		
		String sparql_r = MyUtils
				.fileToString("sparql/capabilities/getNodeTemplateCapabilities.sparql");
		String query_r = KB.PREFIXES + sparql_r;
		
		//Check which resources have capabilities
		TupleQueryResult result_r = QueryUtil.evaluateSelectQuery(kb.getConnection(), query_r, new SimpleBinding("var_aadm", aadmUri));
		
		while (result_r.hasNext()) {
			BindingSet bindingSet_r = result_r.next();
			IRI r = (IRI) bindingSet_r.getBinding("resource").getValue();
			optimization_json = MyUtils.getStringValue(bindingSet_r.getBinding("optimizations").getValue());
			IRI  capability_iri =  (IRI) bindingSet_r.getBinding("capability").getValue();
			IRI nodeType_iri = (IRI) bindingSet_r.getBinding("templateType").getValue();
			String  nodeType = MyUtils.getStringValue(nodeType_iri);
			
			resourceOptimizationJson.put(r, optimization_json);
			
			LOG.info("Querying for resource = {}, optimizations = {}, capability = {}", r.toString(), optimization_json,  capability_iri.toString());
			JsonObject jsonObject = JsonParser.parseString(optimization_json).getAsJsonObject();
			String app_type = jsonObject.getAsJsonObject("optimization").get("app_type").getAsString();
			String ai_framework = null;
			if	(app_type.equals("ai_training"))
				ai_framework = jsonObject.getAsJsonObject("optimization").getAsJsonObject("app_type-" + app_type).getAsJsonObject("config").get("ai_framework").getAsString();
			LOG.info("app_type = {} , ai_framework = {}", app_type, ai_framework);
			
			//Check app type
			if(!appTypes.containsKey(app_type)) {
				errorModels.add(new ApplicationTypeModel(app_type, r));
			} else {
				//Check if the derived node type is compatible with the app type in optimization json
				if(!appTypes.get(app_type).contains(nodeType)) {
					InferencesUtil.checkLooseSubclassList(kb, nodeType_iri, appTypes.get(app_type));
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
						LOG.info("Querying for capability = {}, capability value = {}", capability, capability_value);
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
		
		LOG.info("\nOptimizations: ");
		resourceOptimizations.forEach((r,o)->{
			LOG.info("Resource : {}, Optimizations : {}", r, o);
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
						LOG.info("Resource = {}, has user optimization  {}:{}", r.toString(), jsonelement, user_opt_value);
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
	
	public String getClassForType(String node) {
		String classType = null;
		String sparql = "select ?class where { \r\n" + 
				"\t?var a soda:SodaliteSituation;\r\n" + 
				"\ta owl:Class;\r\n" + 
				"\t\tsoda:hasClass ?class\r\n" + 
				"}\r\n";
		
		String query = KB.OWL_PREFIX + KB.SODA_PREFIX + sparql;
		
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding("var", kb.factory.createIRI(node)));

		if (result.hasNext()) {
			BindingSet bindingSet = result.next();
			classType = bindingSet.getBinding("class").getValue().stringValue();
		}
		result.close();
		
		return classType;
	}
	
	
}
