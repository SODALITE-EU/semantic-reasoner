package kb.repository;

import java.util.HashMap;
import java.util.Map;

public final class KBConsts {
	public static final Map<String,String> TYPES = new HashMap<String, String>();
    public static final String HAS_PARAMETER = "hasParameter"; 
	public static final String PARAM_CLASSIFIER = "ParamClassifier_";
	public static final String ATTR_CLASSIFIER = "AttrClassifier_";
	public static final String PROP_CLASSIFIER = "PropClassifier_";
	public static final String DESCRIPTION = "description";
	public static final String CAPABILITY = "Capability";
	public static final String PARAMETER = "Parameter";
	public static final String TRIGGER = "Trigger";
	public static final String PROPERTY = "Property";
	public static final String ATTRIBUTE = "Attribute";
	public static final String INTERFACE = "Interface";
	public static final String OPERATION = "Operation";
	public static final String ARTIFACT = "Artifact";
	public static final String TOSCALIST = KB.TOSCA + "List";
	public static final boolean AADM_JSON = true;
	public static final boolean AADM = true;

	
	public static final boolean IS_INPUT = true;
	public static final boolean IS_OUTPUT = false;
	
	public static final String SLASH = "/";
	
	public static final String PROPERTIES = "properties";
	public static final String REQUIREMENTS = "requirements";
	
	
	static {
       	TYPES.put("data","tosca.datatypes.Root");
       	TYPES.put("node", "tosca.nodes.Root");
       	TYPES.put("capability", "tosca.capabilities.Root");
       	TYPES.put("relationship", "tosca.relationships.Root");
       	TYPES.put("interface", "tosca.interfaces.Root");
       	TYPES.put("policy", "tosca.policies.Root");
     }
	
	public static final Map<String,String> AADM_JSON_CLASSES = new HashMap<String, String>();
	static {
		AADM_JSON_CLASSES.put("NodeType", "node_types");
		AADM_JSON_CLASSES.put("DataType", "data_types");
		AADM_JSON_CLASSES.put("GroupType", "group_types");
		AADM_JSON_CLASSES.put("CapabilityType", "capability_types");
		AADM_JSON_CLASSES.put("RelationshipType", "relationship_types");
		AADM_JSON_CLASSES.put("InterfaceType", "interface_types");
		AADM_JSON_CLASSES.put("PolicyType", "policy_types");
		AADM_JSON_CLASSES.put("ArtifactType", "artifact_types");
		
	}
	
	
	public static final Map<String,String> TEMPLATE_CLASSES = new HashMap<String, String>();
	static {
		TEMPLATE_CLASSES.put("Template", "node_templates");
		TEMPLATE_CLASSES.put("PolicyTemplate", "policy_templates");
		
	}
	

	
	private KBConsts() {
		throw new IllegalStateException("KBConsts class");
	}	
}
