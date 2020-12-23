package kb.validation.exceptions.models;

import org.json.simple.JSONObject;

public class ConstraintPropertyModel  extends ValidationModel {
	String templateName, templateType, property, description;
	
	public ConstraintPropertyModel(String templateName, String templateType, String property, String description) {
		this.templateName = templateName;
		this.templateType = templateType;
		this.property = property;
		this.description = description;
	}
		

	@Override
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("type", "ConstraintProperty");

		JSONObject info = new JSONObject();
		info.put("description", description);
		info.put("name", property);
		info.put("context", templateName);

		json.put("info", info);

		return json;
	}

}
