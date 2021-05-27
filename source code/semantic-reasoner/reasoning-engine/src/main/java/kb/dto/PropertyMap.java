package kb.dto;


import java.util.HashMap;



public class PropertyMap {
	/* Example
	 * snow-security-rules:
		type: openstack/sodalite.nodes.OpenStack.SecurityRules
		properties:
			ports:  
				component_ports:
					port_range_min: 8080 
				http_port:  
					protocol: "tcp"  
	 */
	String name; //ports
	
	HashMap<String, HashMap<String, String>> mapProperties; // {component_ports={ port_range_min=8080}, http_port={protocol=tcp}}
	
	public PropertyMap(String name) {
		this.name = name;
		mapProperties = new HashMap<String, HashMap<String, String>>();
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public HashMap<String, HashMap<String, String>> getMapProperties() {
		return this.mapProperties;
	}
	
	public void setMapProperties(HashMap<String, HashMap<String, String>> mapProperties) {
		this.mapProperties = mapProperties;
	}
}
