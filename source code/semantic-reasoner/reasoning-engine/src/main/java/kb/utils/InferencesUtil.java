package kb.utils;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.repository.KB;

public class InferencesUtil {
	
	/* Given a set of a classes, it returns the class that is lowest in the class hierarchy.
	* e.g. tosca.nodes.Compute, sodalite.nodes.OpenStack.VM node types. 
	* sodalite.nodes.OpenStack.VM should be returned. 
	*/
	public static IRI getLowestSubclass(KB kb, Set<IRI> classes) {
		
		String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n" + 
						"ASK {?var_class1 rdfs:subClassOf ?var_class2" + "}";
		IRI lowestClass = null;
		if (!classes.isEmpty()) {
			lowestClass = classes.stream().iterator().next();
			if (classes.size() == 1)
				return lowestClass;
			for (IRI class1 : classes) {
				for  (IRI class2 : classes) {
					System.out.println("CLASS2 = " + class2.toString());
					boolean	result = QueryUtil.evaluateAskQuery(kb.getConnection(), query, new SimpleBinding[] { new SimpleBinding("var_class1", class1),
										new SimpleBinding("var_class2", class2)});
					if (result) {
						boolean	result2 = QueryUtil.evaluateAskQuery(kb.getConnection(), query, new SimpleBinding[] { new SimpleBinding("var_class1", class1),
								new SimpleBinding("var_class2", lowestClass)});
						if (result2) {
							lowestClass = class1;
						}
					}
				}
			}
		}
		return lowestClass;
	}
	
	/* Given a class, and a set of classes, it returns if the class is subclass of any of the classes of the list. */ 
	public static boolean checkSubclassList(KB kb, IRI subclass, Set<String> classes) {
		String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n" + 
				"ASK {?var_subclass rdfs:subclassOf ?superclass ." + 
				"FILTER (strends(str(?superclass), ?var_superclass)) ." + 
				"}";
		for  (String superclass : classes) {
			boolean	result = QueryUtil.evaluateAskQuery(kb.getConnection(), query, new SimpleBinding[] { new SimpleBinding("var_subclass", subclass),
								new SimpleBinding("var_superclass", kb.getFactory().createLiteral(superclass))});
			if (result)
				return true;
		}
		return false;
	}

}
