package model.ssa;

public class WhitespaceValue extends Value {
	public WhitespaceValue() {
	}
	public boolean equals(Value v) {
		return v == this;
	}
	public String toString() {
		return "";
	}
}
