package model.ssa;

public class RegisterValue extends Value {
	private int index;
	
	public RegisterValue(int i) {
		index = i;
	}

	public int hashCode() {
		return index;
	}
	
	public boolean equals(Value v) {
		if (v instanceof RegisterValue) {
			RegisterValue r = (RegisterValue) v;
			
			return index == r.index;
		}
		return false;
	}
	
	public String toString() {
		return String.format("r%d", index);
	}
}
