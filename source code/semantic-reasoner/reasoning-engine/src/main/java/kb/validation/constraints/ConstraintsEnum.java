package kb.validation.constraints;

public enum ConstraintsEnum {
	GREATER_OR_EQUAL {
		@Override
        public boolean compare(String a, String b, String constr_type) {
            System.out.println("GREATER_OR_EQUAL a = " + a + " b = " + b);
            if (constr_type.equals("integer"))
            	return Integer.parseInt(a.toString()) >= Integer.parseInt(b.toString());
            else if (constr_type.equals("float"))
            	return Float.parseFloat(a.toString()) >= Float.parseFloat(b.toString());
            return false;
        }
	},
	LESS_OR_EQUAL {
		@Override
        public boolean compare(String a, String b, String constr_type) {
            System.out.println("LESS_OR_EQUAL a = " + a + " b = " + b);
            return true;
        }
	};
	
	public abstract boolean compare(String a, String b, String constr_type);
	
}
