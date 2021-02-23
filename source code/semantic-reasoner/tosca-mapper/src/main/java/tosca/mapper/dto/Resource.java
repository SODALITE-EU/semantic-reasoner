package tosca.mapper.dto;


public abstract class Resource {
	String name;
	String description;

	public Resource(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String setName() {
		return this.name;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
