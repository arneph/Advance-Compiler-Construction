package model.ssa;

public class VariableValue extends Value {
	private String name;
	private int id;
	private int version;
	
	public VariableValue(String n, int i) {
		this(n, i, -1);
	}
	
	public VariableValue(String n, int i, int v) {
		name = n;
		id = i;
		version = v;
	}
	
	public int getID() {
		return id;
	}
	
	public int getVersion() {
		return version;
	}
	
	public void setVersion(int v) {
		version = v;
	}

	public int hashCode() {
		return (id << 16) | version;
	}
	
	public boolean equals(Value v) {
		if (v instanceof VariableValue) {
			VariableValue vv = (VariableValue) v;
			
			return id == vv.id && version == vv.version;
 		}
		return false;
	}
	
	public String toString() {
		if (version < 0) return name;
		else return name + "_" + version;
	}
}
