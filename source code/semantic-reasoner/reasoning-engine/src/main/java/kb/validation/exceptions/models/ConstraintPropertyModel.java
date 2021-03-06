package kb.validation.exceptions.models;

import org.json.simple.JSONObject;

public class ConstraintPropertyModel  extends ValidationModel {
	public String templateName;
	public String templateType;
	public String property;
	public String path;
	public String description;
	
	public ConstraintPropertyModel(String templateName, String templateType, String property, String path, String description) {
		this.templateName = templateName;
		this.templateType = templateType;
		this.property = property;
		this.path = path;
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
		info.put("path", path);
		
		json.put("info", info);

		return json;
	}

}
