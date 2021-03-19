package kb.validation.constraints;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kb.repository.KB;

public enum ConstraintsEnum {
	GREATER_OR_EQUAL {
		@Override
		//b parameter is a Set because of the compare method definition, it could be simply a String
        public <T> boolean compare(String a, Set<T> b, String constrType) {
			String bs = b.iterator().next().toString();
			System.err.println("greater or equal  a=" + a+ ", bs = " + bs);
			if (constrType.equals("integer"))
            	return Integer.parseInt(a) >= Integer.parseInt(bs);
            else if (constrType.equals("float"))
            	return Float.parseFloat(a) >= Float.parseFloat(bs);
            return false;
        }
	},
	LESS_OR_EQUAL {
		@Override
        public <T> boolean compare(String a, Set<T> b, String constrType) {
			String bs = b.iterator().next().toString();
			System.err.println("less or equal  a=" + a+ ", bs = " + bs);
			if (constrType.equals("integer"))
				return Integer.parseInt(a) <= Integer.parseInt(bs);
			else if (constrType.equals("float"))
				return Float.parseFloat(a) <= Float.parseFloat(bs);
			return false;
        }
	},
	EQUAL{
		@Override
        public <T> boolean compare(String a, Set<T> b, String constrType) {
			return true;
        }
	},
	MIN_LENGTH{
		@Override
        public <T> boolean compare(String a, Set<T> b, String constrType) {
			return true;
        }
	},
	VALID_VALUES {
		@Override
        public <T> boolean compare(String a, Set<T> b, String constrType) {
			
			if (constrType.equals(KB.TOSCA + "string")) {
				//Set<String> strs = (Set<String>) b.stream().map(s -> s.toString());
				System.err.println("valid_values:{}" + b);
			}
		
			return false;
        }
	};
	
	public abstract  <T> boolean compare(String a, Set<T> b, String constrType);
	//public  boolean compare(String a, Set<String> b, String constrType);
	
}
