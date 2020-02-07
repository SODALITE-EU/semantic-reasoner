package kb.dto;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import com.google.common.primitives.Ints;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kb.KBApi;
import kb.repository.KB;
import kb.utils.MyUtils;

public class Parameter extends Resource {

	String value;
	String valueUri;
	Set<Parameter> parameters;

	transient IRI classifiedBy;

	public Parameter(IRI iri) {
		super(iri);
	}

	@Override
	public JsonObject serialise() {
		JsonObject parameter = new JsonObject();
		if (description != null)
			parameter.addProperty("description", description);

		if (value != null) {
			if (this.valueUri != null) {
				JsonObject t = new JsonObject();
				t.addProperty("label", value);
//				t.addProperty("uri", this.valueUri);
				JsonObject t2 = new JsonObject();
				t2.add(this.valueUri, t);

				parameter.add(label, t2);
				relevantUris.add(this.valueUri);
			} else {
				Object i = null;
				if ((i = Ints.tryParse(value)) != null) {
					parameter.addProperty(label, (int) i);
				} else if ((i = BooleanUtils.toBooleanObject(value)) != null) {
					parameter.addProperty(label, (boolean) i);
				} else {
					
					if(value.startsWith("[")) {
						JsonArray fromJson = MyUtils.getGson(false).fromJson(value, JsonArray.class);
						parameter.add(label, fromJson);
					} else {
						parameter.addProperty(label, value);
					}
								
					//parameter.addProperty(label, value);
				}
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
			parameter.add(label, temp);
		}

		return parameter;
	}

	public Set<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(Set<Parameter> parameters) {
		this.parameters = parameters;
	}

	public IRI getClassifiedBy() {
		return classifiedBy;
	}

	public void setClassifiedBy(IRI classifiedBy) {
		this.classifiedBy = classifiedBy;
	}

	public String getValue() {
		return value;
	}

	// string
	// List
	// Min, Max
	public void setValue(Value value, KB kb) {

		if (this.label.equals("primary")) {
			value = kb.factory.createLiteral(KB.ANSIBLE + value.stringValue());
		}

	 /*"create": {
            "implementation": "playbooks/torque-job/create.yml"
          },
		*/
		
		if (this.label.equals("implementation") && value instanceof Literal && value.stringValue().endsWith(".yml")) {
			value = kb.factory.createLiteral(KB.ANSIBLE + value.stringValue());
		}
		
		
		
		if (value instanceof Literal) {
			this.value = value.stringValue();
			System.out.println("VALUE1: " + value);

		} else {
			System.out.println("VALUE2: " + value);
			IRI v = (IRI) value;
			List<String> collect = Iterations.asList(
					kb.connection.getStatements(v, kb.factory.createIRI(KB.TOSCA + "hasValue"), null))
					.stream().map(x -> MyUtils.getStringValue(x.getObject())).collect(Collectors.toList());
			if (collect.isEmpty() && !v.stringValue().contains("List")) { //this could be [], i.e. an instance of List without values...
				System.out.println("collect.isEmpty() " + collect);
				this.valueUri = v.toString();
				this.value = v.getLocalName();
//				this.value = v.toString();
			} else {
				System.out.println("collect.not emtpy() " + collect);
				this.value = MyUtils.getGson(false).toJson(collect);//.replaceAll("\"", "");
				System.out.println(this.value + " - " + collect);
			}
		}
	}

	@Override
	public JsonElement serialiseCompact() {
		// TODO Auto-generated method stub
		return null;
	}

}
