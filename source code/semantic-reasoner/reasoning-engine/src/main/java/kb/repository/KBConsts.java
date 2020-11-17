package kb.repository;

import java.util.HashMap;
import java.util.Map;

public final class KBConsts {
	
	public static final Map<String,String> TYPES = new HashMap<String, String>(){
        {
            put("data","tosca.datatypes.Root");
            put("node", "tosca.nodes.Root");
            put("capability", "tosca.capabilities.Root");
            put("relationship", "tosca.relationships.Root");
            put("interface", "tosca.interfaces.Root");
        }
    };

}
