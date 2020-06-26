package kb.validation.exceptions.models;

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
	
	Set<IRI> matchingTemplates;
	
	public RequirementExistenceModel(IRI template, IRI type, IRI r_a, IRI r_i, Set<IRI> matchingTemplates) {
		this.template = template.getLocalName();
		this.type = type.stringValue();
		this.r_a = r_a.stringValue();
		this.r_i = r_i.stringValue();
		this.matchingTemplates = matchingTemplates;
	}
	public RequirementExistenceModel(IRI template, IRI type, IRI r_a, IRI r_i, Set<IRI> matchingTemplates, String description) {
		this.template = template.getLocalName();
		this.type = type.stringValue();
		this.r_a = r_a.stringValue();
		this.r_i = r_i.stringValue();
		this.matchingTemplates = matchingTemplates;
		this.description = description;
	}
	
	public RequirementExistenceModel(IRI template, IRI type, IRI r_a, IRI r_i, Set<IRI> matchingTemplates, boolean required) {
		this.template = template.getLocalName();
		this.type = type.stringValue();
		this.r_a = r_a.stringValue();
		this.r_i = r_i.stringValue();
		this.matchingTemplates = matchingTemplates;
		this.required = Boolean.valueOf(required);
	}
	
	@Override
	public JSONObject toJson() {
		// TODO Auto-generated method stub
		
		JSONObject template_obj = new JSONObject();
		JSONObject requirements_obj = new JSONObject();
		JSONObject r_a_obj = new JSONObject();
		JSONObject r_i_obj = new JSONObject();
		//data.addProperty("", value);
		
		JSONArray nodes = new JSONArray();
		for (IRI t : matchingTemplates) {
			nodes.add(t.stringValue());
		}

		r_i_obj.put(r_i, nodes);
		r_a_obj.put(r_a, r_i_obj);
		
		requirements_obj.put("requirements",r_a_obj);
		//required is only added to modified requirements
		if (required != null)
			template_obj.put("required",required);
		if (description != null)
			template_obj.put("description",description);
		template_obj.put(template, requirements_obj);
		
		return template_obj;
	}

}
