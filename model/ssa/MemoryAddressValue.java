package model.ssa;

public class MemoryAddressValue extends Value {
	private String name;
	private int id;
	
	public MemoryAddressValue(String n, int i) {
		name = n;
		id = i;
	}
	
	public int getID() {
		return id;
	}
	
	public boolean equals(Value v) {
		if (v instanceof MemoryAddressValue) {
			MemoryAddressValue a = (MemoryAddressValue) v;
			
			return id == a.id;
 		}
		return false;
	}
	
	public String toString() {
		return "*" + name;
	}
}
