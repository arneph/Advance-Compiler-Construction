package processor;

import java.util.*;

import model.ssa.*;

public class DefUseTable {
	private static class Entry {
		Value value;
		int definition;
		ArrayList<Integer> uses;
		public Entry(Value v) {
			value = v;
			uses = new ArrayList<>();
		}
	}
	private HashMap<Value, Entry> entries;
	private ArrayList<Entry> orderedEntries;
	private HashMap<Integer, Entry> definitionLines;
	private HashMap<Integer, HashSet<Entry>> useLines;
	
	public DefUseTable() {
		entries = new HashMap<>();
		orderedEntries = new ArrayList<>();
		definitionLines = new HashMap<>();
		useLines = new HashMap<>();
	}
	
	public Value getDefinedValue(int instructionNumber) {
		Entry entry = definitionLines.get(instructionNumber);
		
		if (entry == null) {
			return null;
		}else{
			return entry.value;
		}
	}
	
	public Set<Value> getUsedValues(int instructionNumber) {
		HashSet<Entry> entries = useLines.get(instructionNumber);
		HashSet<Value> values = new HashSet<>();
		
		if (entries != null) {
			for (Entry entry : entries) {
				values.add(entry.value);
			}
		}
		
		return Collections.unmodifiableSet(values);
	}
	
	public Integer getDefinition(Value value) {
		if (entries.containsKey(value) == false) {
			return -1;
		}
		
		Entry entry = entries.get(value);
		
		return entry.definition;
	}
	
	public List<Integer> getUses(Value value) {
		if (entries.containsKey(value) == false) {
			return null;
		}
		
		Entry entry = entries.get(value);
		
		return Collections.unmodifiableList(entry.uses);
	}
	
	public void addDefinition(Value value, int instructionNumber) {
		Entry entry = entries.get(value);
		if (entry == null) {
			entry = new Entry(value);
			entries.put(value, entry);
			orderedEntries.add(entry);
		}
		entry.definition = instructionNumber;
		definitionLines.put(instructionNumber, entry);
	}
	
	public void removeDefinition(int instructionNumber) {
		Entry entry = definitionLines.get(instructionNumber);
		
		if (entry == null) return;
		if (entry.uses.size() > 0) {
			entry.definition = -1;			
		}else {
			entries.remove(entry.value);
			orderedEntries.remove(entry);
		}
		
		definitionLines.remove(instructionNumber);
	}
	
	public void addUse(Value value, int instructionNumber) {
		Entry entry = entries.get(value);
		if (entry == null) {
			entry = new Entry(value);
			entries.put(value, entry);
			orderedEntries.add(entry);
		}
		entry.uses.add(instructionNumber);
		useLines.putIfAbsent(instructionNumber, new HashSet<>());
		useLines.get(instructionNumber).add(entry);
	}
	
	public void removeUse(Value value, int instructionNumber) {
		Entry entry = entries.get(value);
		if (entry == null) {
			return;
		}
		entry.uses.remove(new Integer(instructionNumber));
		if (entry.definition == -1 &&
			entry.uses.size() == 0) {
			entries.remove(entry.value);
			orderedEntries.remove(entry);
		}
		useLines.putIfAbsent(instructionNumber, new HashSet<>());
		useLines.get(instructionNumber).remove(entry);
	}
	
	public void removeUses(int instructionNumber) {
		HashSet<Entry> usingEntries = useLines.get(instructionNumber);
		
		if (usingEntries == null) return;
		
		for (Entry entry : usingEntries) {
			entry.uses.remove(new Integer(instructionNumber));
			
			if (entry.definition == -1 &&
				entry.uses.size() == 0) {
				entries.remove(entry.value);
				orderedEntries.remove(entry);
			}
		}
		
		useLines.remove(instructionNumber);
	}
	
	public boolean hasUses(Value value) {
		Entry entry = entries.get(value);
		
		if (entry == null) {
			return false;
		}else{
			return entry.uses.size() > 0;
		}
	}
	
	public String toString() {
		StringBuilder bob = new StringBuilder();
		
		bob.append("Def Value Uses\n");
		
		for (Entry entry : orderedEntries) {
			bob.append(String.format("%3s %5s", 
					                 entry.definition, 
					                 entry.value.toString()));
			for (int use : entry.uses) {
				bob.append(String.format(" %03d", use));
			}
			bob.append("\n");
		}
		
		return bob.toString();
	}
	
}
