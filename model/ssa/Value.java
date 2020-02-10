package model.ssa;

public abstract class Value {
	
	public abstract boolean equals(Value v);
	public abstract String toString();
	
	public int hashCode() {
		return 0;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Value) {
			return equals((Value) obj);
		}else{
			return super.equals(obj);
		}
	}
	
}
