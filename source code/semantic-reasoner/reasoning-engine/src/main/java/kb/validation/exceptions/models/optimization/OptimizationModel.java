package kb.validation.exceptions.models.optimization;

import org.eclipse.rdf4j.model.IRI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;

import java.util.Map;


import kb.validation.exceptions.models.ValidationModel;

public class OptimizationModel extends ValidationModel {
	
	//Set<String> outerOpts;
	//Set<String> appTypeOpts;
	
	String template;
	HashMap<String,String> targetValue;
	
	public OptimizationModel(IRI iri, HashMap<String,String> targetValue) {
		this.template = iri.toString();
		this.targetValue = targetValue;
	}
	
	
	//@Override
	public JSONObject toJson() {
		
		JSONObject template_opt = new JSONObject();
		
		JSONArray a = new JSONArray();
		
		for (Map.Entry e : targetValue.entrySet()) {
			JSONObject o = new JSONObject();
			o.put("path", (String) e.getKey());
			o.put("value", (String) e.getValue());
			
			a.add(o);
		}
		
		String description  = String.format("The optimal optimization values are given");
		
		template_opt.put("type", "Optimization");

		JSONObject info = new JSONObject();
		info.put("description", description);
		info.put("context", template);
		info.put("optimizations", a);
		
		template_opt.put("info", info);
		
		return template_opt;		
	}
	
}
