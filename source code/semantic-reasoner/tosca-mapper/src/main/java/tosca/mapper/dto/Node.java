package tosca.mapper.dto;

import java.util.HashSet;
import java.util.Set;


public class Node extends Resource {

	Set<Property> properties = new HashSet<>();
	Set<Attribute> attributes = new HashSet<>();
	Set<Capability> capabilities = new HashSet<>();
	Set<Requirement> requirements = new HashSet<>();
	/*Set<Interface> interfaces;
	Set<Operation> operations;*/
	
	String ofType;
	//needed for adding the class info in the ontology - node_types, relationship_types
	String classType;
	
		
	public Node(String name) {
		super(name);
	}

	public Node(String name, String classType) {
		super(name);
		this.classType = classType;
	}
	
	public String getOfType() {
		return this.ofType;
	}
	
	public void setOfType(String ofType) {
		this.ofType = ofType;
	}
	
	public String getClassType() {
		return this.classType;
	}
	
	public void setClassType(String classType) {
		this.classType = classType;
	}
	
	public Set<Property> getProperties() {
		return this.properties;
	}
	
	public void setProperties(Set<Property> properties) {
		this.properties = properties;
	}
	
	public Set<Attribute> getAttributes() {
		return this.attributes;
	}
	
	public void setAttributes(Set<Attribute> attributes) {
		this.attributes = attributes;
	}
	
	public Set<Capability> getCapabilities() {
		return this.capabilities;
	}
	
	public void setCapabilities(Set<Capability> capabilities) {
		this.capabilities = capabilities;
	}
	
	public Set<Requirement> getRequirements() {
		return this.requirements;
	}
	
	public void setRequirements(Set<Requirement> requirements) {
		this.requirements = requirements;
	}
}
