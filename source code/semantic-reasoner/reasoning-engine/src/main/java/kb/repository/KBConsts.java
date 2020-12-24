package kb.repository;

import java.util.HashMap;
import java.util.Map;

public final class KBConsts {
	private KBConsts() {
		throw new IllegalStateException("KBConsts class");
	}
	
	public static final Map<String,String> TYPES = new HashMap<String, String>();
	static {
       	TYPES.put("data","tosca.datatypes.Root");
       	TYPES.put("node", "tosca.nodes.Root");
       	TYPES.put("capability", "tosca.capabilities.Root");
       	TYPES.put("relationship", "tosca.relationships.Root");
       	TYPES.put("interface", "tosca.interfaces.Root");
     };

     public static final String HAS_PARAMETER = "hasParameter"; 
}
