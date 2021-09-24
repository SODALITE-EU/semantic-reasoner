package kb.validation.exceptions.models;

import org.json.simple.JSONObject;

import kb.repository.KBConsts;

public class RequiredPropertyAttributeModel extends ValidationModel {

	String kindOfTemplate, templateName, templateType, property, attribute;

	public RequiredPropertyAttributeModel(String kindOfTemplate, String templateName, String templateType, String property, String attribute) {
		super();
		this.kindOfTemplate = kindOfTemplate;
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
		String kindOfElement = property == null ? "attributes" : "properties";
		info.put("path", kindOfTemplate + KBConsts.SLASH + kindOfElement + KBConsts.SLASH + kindOfElement);

		json.put("info", info);

		return json;

	}

}
