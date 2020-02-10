package processor;

import java.util.*;

public class VariableVersionTable {
	private HashMap<Integer, Integer> versions;
	
	public VariableVersionTable() {
		versions = new HashMap<>();
	}
	
	public HashMap<Integer, Integer> getVersions() {
		return versions;
	}
	
	public VariableVersionTable clone() {
		VariableVersionTable t = new VariableVersionTable();
		t.versions.putAll(versions);
		return t;
	}
	
	public static HashSet<Integer> getConflictingIDs(VariableVersionTable a, VariableVersionTable b) {
		HashSet<Integer> conflictIDs = new HashSet<>();
		
		for (int id : a.versions.keySet()) {
			if (!b.versions.containsKey(id)) {
				conflictIDs.add(id);
			}else if (!a.versions.get(id).equals(b.versions.get(id))) {
				conflictIDs.add(id);
			}
		}
		for (int id : b.versions.keySet()) {
			if (!a.versions.containsKey(id)) {
				conflictIDs.add(id);
			}else if (!a.versions.get(id).equals(b.versions.get(id))) {
				conflictIDs.add(id);
			}
		}
		
		return conflictIDs;
	}
	
}
