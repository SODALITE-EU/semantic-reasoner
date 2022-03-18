package kb.validation.exceptions.models;

import org.json.simple.JSONObject;


public class PropertyInputModel extends ValidationModel {
	String contextPath, description, name;

	public PropertyInputModel(String contextPath, String description, String name) {
		super();
		this.contextPath = contextPath;
		this.description = description;
		this.name = name;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("type", "PropertyInput");
		
		JSONObject template_obj = new JSONObject();
		
		template_obj.put("context", contextPath);
		template_obj.put("description", description);
		template_obj.put("name", name);
		
		json.put("info", template_obj);
		
		return json;

	}
}
