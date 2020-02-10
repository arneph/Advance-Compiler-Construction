package model.ssa;

public class ConstantValue extends Value {
	private int value;
	
	public ConstantValue(int v) {
		value = v;
	}
	
	public int getValue() {
		return value;
	}
	
	public boolean equals(Value v) {
		if (v instanceof ConstantValue) {
			ConstantValue c = (ConstantValue) v;
			
			return value == c.value;
		}
		return false;
	}
	
	public String toString() {
		return "#" + value;
	}
}
