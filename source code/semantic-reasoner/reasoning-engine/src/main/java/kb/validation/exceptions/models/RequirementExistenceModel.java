package kb.validation.exceptions.models;

import java.util.HashMap;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import kb.repository.KBConsts;

public class RequirementExistenceModel extends ValidationModel {

	String template;
	String type;
	String description;
	String name;
	
	
	
	String contextPath;
	
	Set<IRI> matchingTemplates;
	
	
	public RequirementExistenceModel(String contextPath, String description, Set<IRI> matchingTemplates, String name) {
		this.contextPath = contextPath;
		this.description = description;
		this.matchingTemplates = matchingTemplates;
		this.name = name;
	}
	
	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("type", "RequirementExistence");
		
		JSONObject template_obj = new JSONObject();
		
		template_obj.put("context", contextPath);
		template_obj.put("description", description);
		
		JSONArray nodes = new JSONArray();
		for (IRI t : matchingTemplates) {
			nodes.add(t.stringValue());
		}
		template_obj.put("name", name);
		template_obj.put("suggestions", nodes);
		
		json.put("info", template_obj);
		
		return json;
	}

}
