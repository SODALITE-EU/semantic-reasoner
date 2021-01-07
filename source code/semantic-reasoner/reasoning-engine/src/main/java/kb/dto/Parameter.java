package kb.dto;

import java.util.List;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import kb.repository.KB;
import kb.utils.MyUtils;

public class Parameter extends Resource {
	private static final Logger LOG = LoggerFactory.getLogger(Parameter.class.getName());

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
				} else if ((i = BooleanUtils.toBooleanObject(value)) != null && !value.equals("no") && !value.equals("yes")) {
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
			JsonArray fileArray = new JsonArray();
			for (Parameter p : parameters) {
				JsonObject serialise = p.serialise();
				relevantUris.addAll(p.relevantUris);
				Set<Entry<String, JsonElement>> entrySet = serialise.entrySet();
				for (Entry<String, JsonElement> entry : entrySet) {
					//All file parameters(in interfaces) are added to a files array 
					if (entry.getKey().equals("file")) {
						fileArray.add(entry.getValue());
					} else {
						temp.add(entry.getKey(), entry.getValue());
					}
				}
			}
			if (fileArray.size() > 0)
				temp.add("files", fileArray);
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
			LOG.info("VALUE1: {}", value);
		} else {
			LOG.info("VALUE2: {}", value);
			IRI v = (IRI) value;
			List<String> collect = Iterations.asList(
					kb.connection.getStatements(v, kb.factory.createIRI(KB.TOSCA + "hasValue"), null))
					.stream().map(x -> MyUtils.getStringValue(x.getObject())).collect(Collectors.toList());
			if (collect.isEmpty() && !v.stringValue().contains("List")) { //this could be [], i.e. an instance of List without values...
				LOG.info("collect.isEmpty() {}", collect);
				this.valueUri = v.toString();
				this.value = v.getLocalName();
//				this.value = v.toString();
			} else {
				LOG.info("collect.not empty() {}", collect);
				this.value = MyUtils.getGson(false).toJson(collect);//.replaceAll("\"", "");
				LOG.info("{} - {}", this.value, collect);
			}
		}
	}

	@Override
	public JsonElement serialiseCompact() {
		// TODO Auto-generated method stub
		return null;
	}

}
