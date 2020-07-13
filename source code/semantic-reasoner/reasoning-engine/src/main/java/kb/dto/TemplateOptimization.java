package kb.dto;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kb.KBApi;

public class TemplateOptimization extends Resource{
	
	//Set<String> outerOpts;
	//Set<String> appTypeOpts;
	
	HashMap<String,String> targetValue;
	
	public TemplateOptimization(IRI iri, HashMap<String,String> targetValue) {
		super(iri);
		this.targetValue = targetValue;
	}
	
	
	//@Override
	public JsonObject serialise() throws IOException {
		
		JsonObject template_opt = new JsonObject();
		
		JsonArray a = new JsonArray();
		
		for (Map.Entry e : targetValue.entrySet()) {
			JsonObject o = new JsonObject();
			o.addProperty("path", (String) e.getKey());
			o.addProperty("value", (String) e.getValue());
			
			a.add(o);
		}
		
		template_opt.add(label, a);
		return template_opt;		
	}

	@Override
	public JsonElement serialiseCompact() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
