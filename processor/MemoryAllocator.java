package processor;

import model.syntax.*;

public class MemoryAllocator {
	private Computation computation;
	private SymbolTable table;
	private int globalSpace;
	
	public MemoryAllocator(Computation cmp, SymbolTable t) {
		computation = cmp;
		table = t;
		globalSpace = 0;
	}
	
	public int getGlobalSpace() {
		return globalSpace;
	}
	
	public void allocate() {
		for (VariableDeclaration var : computation.variables) {
			int varSize = var.type.getSize();
			
			for (int id : var.nameIDs) {
				table.setHasRelativeOffset(id, false);
				table.setOffset(id, 0 - globalSpace - varSize);
				
				globalSpace += varSize;
			}
		}
		
		globalSpace += 100;
		
		for (FunctionDeclaration func : computation.functions) {
			int parameterSpace = 0;
			for (int id : func.parameterIDs) {
				table.setHasRelativeOffset(id, true);
				table.setOffset(id, 8 + parameterSpace);
				
				parameterSpace += 4;
			}
			
			table.setHasRelativeOffset(func.nameID, true);
			table.setOffset(func.nameID, 8 + parameterSpace);
		}
	}
	
}
