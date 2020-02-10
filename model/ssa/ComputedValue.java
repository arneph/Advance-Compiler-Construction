package model.ssa;

public class ComputedValue extends Value {
	private int instruction;
	
	public ComputedValue(int instr) {
		instruction = instr;
	}

	public int getInstructionNumber() {
		return instruction;
	}
	
	public int hashCode() {
		return instruction;
	}
	
	public boolean equals(Value v) {
		if (v instanceof ComputedValue) {
			ComputedValue c = (ComputedValue) v;
			
			return instruction == c.instruction;
 		}
		return false;
	}
	
	public String toString() {
		return String.format("(%03d)", instruction);
	}
	
}
