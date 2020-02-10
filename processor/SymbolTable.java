package processor;
import java.util.*;

public class SymbolTable {
	public class Scope {
		private HashMap<String, Integer> nounNames; // variables, arrays
		private HashMap<String, Integer> verbNames; // procedures, functions
		private int depth;
		
		private Scope(int d) {
			nounNames = new HashMap<>();
			verbNames = new HashMap<>();
			depth = d;
		}
		
		public int getDepth() {
			return depth;
		}
		
		public boolean hasNounName(String name) {
			return nounNames.containsKey(name);
		}
		
		public int nounNameToID(String s) {
			return nounNames.get(s);
		}
		
		public boolean hasVerbName(String name) {
			return verbNames.containsKey(name);
		}
		
		public int verbNameToID(String s) {
			return verbNames.get(s);
		}
		
		public int addProcedure(String name, int parameterCount) {
			int id = otherIDs++;
			
			addEntry(id, name, Sym.ProcedurePrefix, parameterCount, null);
			
			return id;
		}
		
		public int addFunction(String name, int parameterCount) {
			int id = otherIDs++;
			
			addEntry(id, name, Sym.FunctionPrefix, parameterCount, null);
			
			return id;
		}
		
		public int addVariable(String name) {
			int id = otherIDs++;
			
			addEntry(id, name, Sym.VariablePrefix, -1, null);
			
			return id;
		}
		
		public int addArray(String name, int[] dimensions) {
			int id = otherIDs++;
			
			addEntry(id, name, Sym.ArrayPrefix, -1, dimensions);
			
			return id;
		}
		
		private void addEntry(int id, String name, String type, int params, int[] dim) {
			if (hasID(id)) {
				throw new IllegalArgumentException();
			}
			
			if (type == Sym.VariablePrefix || type == Sym.ArrayPrefix) {
				if (hasNounName(name)) {
					throw new IllegalArgumentException();
				}
				
				nounNames.put(name, id);				
			}else if (type == Sym.ProcedurePrefix || type == Sym.FunctionPrefix) {
				if (hasVerbName(name)) {
					throw new IllegalArgumentException();
				}
				
				verbNames.put(name, id);
			}else if (type == Sym.KeywordPrefix) {
				if (hasNounName(name) || hasVerbName(name)) {
					throw new IllegalArgumentException();
				}
				
				nounNames.put(name, id);
				verbNames.put(name, id);
			}else{
				throw new IllegalArgumentException();
			}
			depths.put(id, depth);
			types.put(id, type);
			parameters.put(id, params);
			dimensions.put(id, dim);
			ids.put(id, name);
		}
	}
	
	private LinkedList<Scope> frames;
	private HashMap<Integer, Integer> depths;
	private HashMap<Integer, String> types;
	private HashMap<Integer, Integer> parameters;
	private HashMap<Integer, int[]> dimensions;
	private HashMap<Integer, Boolean> relativeOffsets;
	private HashMap<Integer, Integer> offsets;
	private HashMap<Integer, String> ids;
	private int otherIDs;
	
	public SymbolTable() {
		Scope globalFrame = new Scope(0);
		
		frames = new LinkedList<>();
		frames.push(globalFrame);
		depths = new HashMap<>();
		types = new HashMap<>();
		parameters = new HashMap<>();
		dimensions = new HashMap<>();
		relativeOffsets = new HashMap<>();
		offsets = new HashMap<>();
		ids = new HashMap<>();
		otherIDs = 300;
		
		globalFrame.addEntry(41, "then", Sym.KeywordPrefix, -1, null);
		globalFrame.addEntry(42, "do", Sym.KeywordPrefix, -1, null);
		
		globalFrame.addEntry(81, "od", Sym.KeywordPrefix, -1, null);
		globalFrame.addEntry(82, "fi", Sym.KeywordPrefix, -1, null);
		
		globalFrame.addEntry(90, "else", Sym.KeywordPrefix, -1, null);
		
		globalFrame.addEntry(100, "let", Sym.KeywordPrefix, -1, null);
		globalFrame.addEntry(101, "call", Sym.KeywordPrefix, -1, null);
		globalFrame.addEntry(102, "if", Sym.KeywordPrefix, -1, null);
		globalFrame.addEntry(103, "while", Sym.KeywordPrefix, -1, null);
		globalFrame.addEntry(104, "return", Sym.KeywordPrefix, -1, null);

		globalFrame.addEntry(110, "var", Sym.KeywordPrefix, -1, null);
		globalFrame.addEntry(111, "array", Sym.KeywordPrefix, -1, null);
		globalFrame.addEntry(112, "function", Sym.KeywordPrefix, -1, null);
		globalFrame.addEntry(113, "procedure", Sym.KeywordPrefix, -1, null);

		globalFrame.addEntry(200, "main", Sym.KeywordPrefix, -1, null);
		
		globalFrame.addEntry(256, "InputNum", Sym.FunctionPrefix, 0, null);
		globalFrame.addEntry(257, "OutputNum", Sym.ProcedurePrefix, 1, null);
		globalFrame.addEntry(258, "OutputNewLine", Sym.ProcedurePrefix, 0, null);
	}
	
	public void pushFrame() {
		frames.push(new Scope(frames.size()));
	}
	
	public void popFrame() {
		if (frames.size() <= 1) {
			throw new IllegalStateException("Can not pop global frame");
		}
		
		frames.pop();
	}
	
	public Scope getLocalFrame() {
		return frames.peek();
	}
	
	public Scope getGlobalFrame() {
		return frames.getLast();
	}
	
	public int getStackDepth() {
		return frames.size() - 1;
	}
	
	public boolean hasNounName(String name) {
		for (Scope frame : frames) {
			if (frame.hasNounName(name)) {
				return true;
			}
		}
		return false;
	}
	
	public int nounNameToID(String name) {
		for (Scope frame : frames) {
			if (frame.hasNounName(name)) {
				return frame.nounNameToID(name);
			}
		}
		return -1;
	}

	public boolean hasVerbName(String name) {
		for (Scope frame : frames) {
			if (frame.hasVerbName(name)) {
				return true;
			}
		}
		return false;
	}
	
	public int verbNameToID(String name) {
		for (Scope frame : frames) {
			if (frame.hasVerbName(name)) {
				return frame.verbNameToID(name);
			}
		}
		return -1;
	}

	public int keywordToID(String name) {
		return nounNameToID(name);
	}
	
	public boolean hasID(int id) {
		return ids.containsKey(id);
	}
	
	public String IDToName(int id) {
		return ids.get(id);
	}
	
	public int getDepth(int id) {
		return depths.getOrDefault(id, -1);
	}
	
	private boolean isKeyword(int id) {
		return Sym.KeywordPrefix.equals(types.get(id));
	}

	public boolean isKeyword(String s) {
		if (!getGlobalFrame().hasNounName(s)) {
			return false;
		}
		
		int id = nounNameToID(s);
		return isKeyword(id);
	}
	
	public boolean isProcedure(int id) {
		return Sym.ProcedurePrefix.equals(types.get(id));
	}
	
	public boolean isFunction(int id) {
		return Sym.FunctionPrefix.equals(types.get(id));
	}
	
	public int getNumberOfParameters(int id) {
		return parameters.get(id);
	}
	
	public boolean isVariable(int id) {
		return Sym.VariablePrefix.equals(types.get(id));
	}
	
	public boolean isArray(int id) {
		return Sym.ArrayPrefix.equals(types.get(id));
	}
	
	public int[] getArrayDimensions(int id) {
		return dimensions.get(id);
	}
	
	public int addProcedure(String name, int parameterCount) {
		return getLocalFrame().addProcedure(name, parameterCount);
	}
	
	public int addFunction(String name, int parameterCount) {
		return getLocalFrame().addFunction(name, parameterCount);
	}
	
	public int addVariable(String name) {
		return getLocalFrame().addVariable(name);
	}
	
	public int addArray(String name, int[] dimensions) {
		return getLocalFrame().addArray(name, dimensions);
	}
	
	public boolean hasRelativeOffset(int id) {
		return relativeOffsets.getOrDefault(id, false);
	}
	
	public void setHasRelativeOffset(int id, boolean r) {
		relativeOffsets.put(id, r);
	}
	
	public int getOffset(int id) {
		return offsets.getOrDefault(id, 0);
	}
	
	public void setOffset(int id, int o) {
		offsets.put(id, o);
	}
	
}
