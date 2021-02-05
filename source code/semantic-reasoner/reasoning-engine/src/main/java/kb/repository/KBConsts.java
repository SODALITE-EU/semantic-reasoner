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
	public static final String PARAMETER = "Parameter";
	public static final String TRIGGER = "Trigger";
	public static final String PROPERTY = "Property";
	public static final String ATTRIBUTE = "Attribute";
	public static final String INTERFACE = "Interface";
	public static final String OPERATION = "Operation";
	public static final String TOSCALIST = KB.TOSCA + "List";
	public static final boolean AADM_JSON = true;
	public static final boolean AADM = true;
	
	private KBConsts() {
		throw new IllegalStateException("KBConsts class");
	}	
	
	static {
       	TYPES.put("data","tosca.datatypes.Root");
       	TYPES.put("node", "tosca.nodes.Root");
       	TYPES.put("capability", "tosca.capabilities.Root");
       	TYPES.put("relationship", "tosca.relationships.Root");
       	TYPES.put("interface", "tosca.interfaces.Root");
       	TYPES.put("policy", "tosca.policies.Root");
     }
}
