package kb.utils;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.repository.KB;

public class InferencesUtil {
	
	public static IRI getLowestSubclass(KB kb,Set<IRI> classes) {
		
		String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n" + 
						"ASK {?var_class1 rdfs:subclassOf ?var_class2" + "}";
		IRI lowestClass = null;
		
		if (!classes.isEmpty()) {
			lowestClass = classes.stream().iterator().next();
			for (IRI class1 : classes) {
				for  (IRI class2 : classes) {
					boolean	result = QueryUtil.evaluateAskQuery(kb.getConnection(), query, new SimpleBinding[] { new SimpleBinding("var_class1", class1),
										new SimpleBinding("var_class2", class2)});
					if (result) {
						boolean	result2 = QueryUtil.evaluateAskQuery(kb.getConnection(), query, new SimpleBinding[] { new SimpleBinding("var_class1", class1),
								new SimpleBinding("var_class2", lowestClass)});
						if (result2)
							lowestClass = class2;
					}
				}
			}
		}
		return lowestClass;
	}

}
