package kb.validation.exceptions.models;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RequirementExistenceModel extends ValidationModel {

	String template;
	String type;
	String r_a;
	String r_i;
	Boolean required;
	String description;
	
	
	
	String contextPath;
	
	Set<IRI> matchingTemplates;
	
	
	public RequirementExistenceModel(String contextPath, String description, Set<IRI> matchingTemplates) {
		this.contextPath = contextPath;
		this.description = description;
		this.matchingTemplates = matchingTemplates;
	}
	
	@Override
	public JSONObject toJson() {
	
		JSONObject template_obj = new JSONObject();
		
		template_obj.put("context", contextPath);
		template_obj.put("description", description);
		
		JSONArray nodes = new JSONArray();
		for (IRI t : matchingTemplates) {
			nodes.add(t.stringValue());
		}
		
		template_obj.put("suggestions", nodes);
		
		return template_obj;
	}

}
