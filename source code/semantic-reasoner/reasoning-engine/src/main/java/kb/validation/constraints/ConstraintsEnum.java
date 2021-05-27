package kb.validation.constraints;

import java.util.Set;
import java.util.stream.Collectors;

import kb.repository.KB;

public enum ConstraintsEnum {
	GREATER_OR_EQUAL {
		@Override
		//b parameter is a Set because of the compare method definition, it could be simply a String
        public <T> boolean compare(String a, Set<T> b, String constrType) {
			String bs = b.iterator().next().toString();
		/*	if (constrType.equals("integer"))
            	return Integer.parseInt(a) >= Integer.parseInt(bs);
             if (constrType.equals("float"))*/
            	return Float.parseFloat(a) >= Float.parseFloat(bs);
           // return false;
        }
	},
	LESS_OR_EQUAL {
		@Override
        public <T> boolean compare(String a, Set<T> b, String constrType) {
			String bs = b.iterator().next().toString();
			/*if (constrType.equals("integer"))
				return Integer.parseInt(a) <= Integer.parseInt(bs);
			 if (constrType.equals("float"))*/
				return Float.parseFloat(a) <= Float.parseFloat(bs);
			//return false;
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
			String bs = b.iterator().next().toString();
			return Integer.parseInt(a) >=  Integer.parseInt(bs);
        }
	},
	LENGTH{
		@Override
        public <T> boolean compare(String a, Set<T> b, String constrType) {
			String bs = b.iterator().next().toString();
			return Integer.parseInt(a) ==  Integer.parseInt(bs);
        }
	},
	MAX_LENGTH{
		@Override
        public <T> boolean compare(String a, Set<T> b, String constrType) {
			String bs = b.iterator().next().toString();
			return Integer.parseInt(a) <=  Integer.parseInt(bs);
        }
	},
	VALID_VALUES {
		@Override
        public <T> boolean compare(String a, Set<T> b, String constrType) {
			
			if (constrType.equals(KB.TOSCA + "string")) {
				//Set<String> strs = (Set<String>) b.stream().map(s -> s.toString());
				Set<String> validValuesItems = b.stream().map(String::valueOf).collect(Collectors.toSet());
				
				System.err.println("valid_values:" + validValuesItems);
				return validValuesItems.contains(a);
				
			}
		
			return false;
        }
	};
	
	public abstract  <T> boolean compare(String a, Set<T> b, String constrType);
	//public  boolean compare(String a, Set<String> b, String constrType);
	
}
