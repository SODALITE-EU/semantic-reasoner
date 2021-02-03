package kb.dto;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rdf4j.model.IRI;

import com.google.common.primitives.Ints;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kb.KBApi;
import kb.utils.MyUtils;

public abstract class Feature extends Parameter {

	public IRI hostDefinition;
	public Feature(IRI iri) {
		super(iri);
	}

	public void build(KBApi api) throws IOException {
		// description
		description = api.getDescription(classifiedBy);

		// parse parameters
		parameters = api.getParameters(classifiedBy);
	}

	@Override
	public JsonObject serialise() {
		JsonObject data = new JsonObject();
		if (description != null)
			data.addProperty("description", description);
		//Returned only in ide, and not aadm json
		if (hostDefinition != null)
			data.addProperty("definedIn", hostDefinition.toString());
		if (value != null) {
			if (this.valueUri != null) {
				JsonObject t = new JsonObject();
				t.addProperty("label", value);
//				t.addProperty("uri", this.valueUri);
				JsonObject t2 = new JsonObject();
				t2.add(this.valueUri, t);
				t2.addProperty("label", label);
				data.add("value", t2);
				relevantUris.add(this.valueUri);
			} else {
				Object i = null;
				if ((i = Ints.tryParse(value)) != null) {
					data.addProperty("value", (int) i);
				} else if ((i = BooleanUtils.toBooleanObject(value)) != null) {
					data.addProperty("value", (boolean) i);
				} else {
					if(value.startsWith("[")) {
						JsonArray fromJson = MyUtils.getGson(false).fromJson(value, JsonArray.class);
						data.add("value",fromJson );
					} else {
						data.addProperty("value", value);
					}
				}
				data.addProperty("label", label);
			}

		} else {
			JsonObject temp = new JsonObject();
			for (Parameter p : parameters) {
				JsonObject serialise = p.serialise();
				relevantUris.addAll(p.relevantUris);
				Set<Entry<String, JsonElement>> entrySet = serialise.entrySet();
				for (Entry<String, JsonElement> entry : entrySet) {
					temp.add(entry.getKey(), entry.getValue());
				}
			}

			if (!temp.entrySet().isEmpty())
				data.add("specification", temp);
		}

		JsonObject feature = new JsonObject();
		feature.add(uri, data);
		return feature;
	}

	@Override
	public JsonObject serialiseCompact() {
		JsonObject data = new JsonObject();
		if (description != null)
			data.addProperty("description", description);
		
		if (value != null) {
			if (this.valueUri != null) {
				data.addProperty(uri, this.valueUri);
			} else {
				Object i = null;
				if ((i = Ints.tryParse(value)) != null) {
					data.addProperty(uri, (int) i);
				} else if ((i = BooleanUtils.toBooleanObject(value)) != null) {
					data.addProperty(uri, (boolean) i);
				} else
					data.addProperty(uri, value);
//				data.addProperty("label", label);
			}

		} else {
			JsonObject temp = new JsonObject();
			for (Parameter p : parameters) {
				JsonObject serialise = p.serialise();
				Set<Entry<String, JsonElement>> entrySet = serialise.entrySet();
				for (Entry<String, JsonElement> entry : entrySet) {
					temp.add(entry.getKey(), entry.getValue());
				}
			}
			data.add("specification", temp);
		}

//		JsonObject feature = new JsonObject();
//		feature.add(uri, data);
		return data;
	}

	public IRI getClassifiedBy() {
		return classifiedBy;
	}

	public void setClassifiedBy(IRI classifiedBy) {
		this.classifiedBy = classifiedBy;
	}

	public IRI getHostDefinition() {
		return hostDefinition;
	}

	public void setHostDefinition(IRI hostDefinition) {
		this.hostDefinition = hostDefinition;
	}
}
