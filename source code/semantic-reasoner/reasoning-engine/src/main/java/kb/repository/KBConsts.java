package kb.repository;

import java.util.HashMap;
import java.util.Map;

public final class KBConsts {
	public static final Map<String,String> TYPES = new HashMap<String, String>();
    public static final String HAS_PARAMETER = "hasParameter"; 
	
	
	private KBConsts() {
		throw new IllegalStateException("KBConsts class");
	}	
	
	static {
       	TYPES.put("data","tosca.datatypes.Root");
       	TYPES.put("node", "tosca.nodes.Root");
       	TYPES.put("capability", "tosca.capabilities.Root");
       	TYPES.put("relationship", "tosca.relationships.Root");
       	TYPES.put("interface", "tosca.interfaces.Root");
     };
}
