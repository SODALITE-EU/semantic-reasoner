package kb.dto;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kb.KBApi;

public class TemplateOptimization extends Resource{
	
	Set<String> optimizations;
	
	public TemplateOptimization(IRI iri, Set<String> optimizations) {
		super(iri);
		this.optimizations = optimizations;
	}
	
	@Override
	public JsonElement serialise() throws IOException {
		JsonObject template_opt = new JsonObject();
		JsonObject data = new JsonObject();				
		JsonArray array = new JsonArray();
		for (String o : optimizations) {
			array.add(o);
		}

		data.add("optimizations", array);
		template_opt.add(label, data);
		
		return template_opt;		
	}

	@Override
	public JsonElement serialiseCompact() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
