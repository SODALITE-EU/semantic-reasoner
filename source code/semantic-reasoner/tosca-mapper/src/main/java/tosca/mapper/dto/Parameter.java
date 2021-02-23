package tosca.mapper.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;




public class Parameter extends Resource {
	String value;
	Set<Parameter> parameters;
	List<String> values;
	
	public Parameter(String name) {
		super(name);
	}
	
	
	public String getValue() {
		return this.value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public Set<Parameter> getParameters() {
		return this.parameters;
	}
	
	public void setParameters(Set<Parameter> parameters) {
		this.parameters = parameters;
	}
	
	public List<String> getValues() {
		return this.values;
	}
	
	public void setValues(ArrayList<String> values) {
		this.values = values;
	}
}