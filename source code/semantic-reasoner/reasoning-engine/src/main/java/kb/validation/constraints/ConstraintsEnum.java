package kb.validation.constraints;

public enum ConstraintsEnum {
	GREATER_OR_EQUAL {
		@Override
        public boolean compare(String a, String b, String constr_type) {
            if (constr_type.equals("integer"))
            	return Integer.parseInt(a) >= Integer.parseInt(b);
            else if (constr_type.equals("float"))
            	return Float.parseFloat(a) >= Float.parseFloat(b);
            return false;
        }
	},
	LESS_OR_EQUAL {
		@Override
        public boolean compare(String a, String b, String constr_type) {
			if (constr_type.equals("integer"))
				return Integer.parseInt(a) <= Integer.parseInt(b);
			else if (constr_type.equals("float"))
				return Float.parseFloat(a) <= Float.parseFloat(b);
			return false;
        }
	},
	EQUAL{
		@Override
        public boolean compare(String a, String b, String constr_type) {
			return true;
        }
	},
	MIN_LENGTH{
		@Override
        public boolean compare(String a, String b, String constr_type) {
			return true;
        }
	};
	
	public abstract boolean compare(String a, String b, String constr_type);
	
}
