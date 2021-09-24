package kb.validation.exceptions.models;

import org.json.simple.JSONObject;

public class RequiredPropertyAttributeModel extends ValidationModel {

	String path, templateName, templateType, property, attribute;

	public RequiredPropertyAttributeModel(String path, String templateName, String templateType, String property, String attribute) {
		super();
		this.path = path;
		this.templateName = templateName;
		this.templateType = templateType;
		this.property = property;
		this.attribute = attribute;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("type", "RequiredProperty");

		JSONObject info = new JSONObject();
		info.put("description", String.format("Missing required %s.", property == null ? "attribute" : "property"));
		info.put("name", property);
		info.put("context", templateName);
		info.put("path", path);

		json.put("info", info);

		return json;

	}

}
