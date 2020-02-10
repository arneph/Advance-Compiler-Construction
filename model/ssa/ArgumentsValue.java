package model.ssa;

import java.util.*;

public class ArgumentsValue extends Value {
	private ArrayList<Value> arguments;
	
	public ArgumentsValue(List<Value> args) {
		arguments = new ArrayList<>(args);
	}
	
	public List<Value> getArguments() {
		return Collections.unmodifiableList(arguments);
	}
	
	public void replace(HashMap<Value, Value> replacements) {
		for (int i = 0; i < arguments.size(); i++) {
			Value oldArg = arguments.get(i);
			Value newArg = replacements.getOrDefault(oldArg, oldArg);
			
			arguments.set(i, newArg);
		}
	}
	
	public int hashCode() {
		int code = 0;
		for (Value arg : arguments) {
			code ^= arg.hashCode();
		}
		return code;
	}
	
	public boolean equals(Value v) {
		if (v instanceof ArgumentsValue) {
			ArgumentsValue a = (ArgumentsValue) v;
			
			if (arguments.size() != a.arguments.size()) return false;
			
			for (int i = 0; i < arguments.size(); i++) {
				Value x = arguments.get(i);
				Value y = a.arguments.get(i);
				if (x.equals(y) == false) return false;
			}
			return true;
		}
		return false;
	}
	
	public String toString() {
		String s = "";
		
		for (int i = 0; i < arguments.size(); i++) {
			if (i > 0) s += ",";
			s += arguments.get(i).toString();
		}
		
		return s;
	}
}
