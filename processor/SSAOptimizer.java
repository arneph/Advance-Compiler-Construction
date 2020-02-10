package processor;

import java.util.*;

import model.ssa.*;

public class SSAOptimizer {
	private Program program;
	private DefUseTable defUseTable;
	
	private StringBuilder eliminationProtocol;
	
	public SSAOptimizer(Program pgm, DefUseTable t, StringBuilder protocol) {
		program = pgm;
		defUseTable = t;
		eliminationProtocol = protocol;
	}
	
	// Copy propagation:
	public void removeMoveInstructions() {
		HashMap<Value, Value> replacements = new HashMap<>();
		
		for (BasicBlock block : program.getBasicBlocks()) {
			for (int i = 0; i < block.getInstructions().size(); i++) {
				Instruction instr = block.getInstructions().get(i);
				
				if (instr.getOperator() != Instruction.Operator.move) {
					continue;
				}
				
				block.eliminateInstruction(instr);
				i--;
				
				Value oldValue = instr.getArgY();
				Value newValue = instr.getArgX();
				while (replacements.containsKey(newValue)) {
					newValue = replacements.get(newValue);
				}
				
				replacements.put(oldValue, newValue);
				
				defUseTable.removeDefinition(instr.getNumber());
				defUseTable.removeUses(instr.getNumber());

				eliminationProtocol.append(String.format(" CP: (%03d) is a move instruction, replace: %s with: %s\n", 
													    instr.getNumber(),
														instr.getArgY(),
														instr.getArgX()));	
			}
		}
		
		for (int t = 0; t < 2; t++) {
			for (BasicBlock block : program.getBasicBlocks()) {
				for (int i = 0; i < block.getInstructions().size(); i++) {
					Instruction instr = block.getInstructions().get(i);
					for (int j = 0; j < 3; j++) {
						Value argV = instr.getArg(j);
						
						if (argV == null) continue;
						if (argV instanceof ArgumentsValue) {
							ArgumentsValue argsV = (ArgumentsValue) argV;
							
							for (Value av : argsV.getArguments()) {
								Value ar = replacements.getOrDefault(av, null);
								
								if (ar == null) continue;
								
								defUseTable.removeUse(av, instr.getNumber());
								defUseTable.addUse(ar, instr.getNumber());
							}
							
							argsV.replace(replacements);
						}
						if (replacements.containsKey(argV) == false) continue;
						
						Value repV = replacements.get(argV);
						
						instr.setArg(j, repV);
						
						defUseTable.removeUse(argV, instr.getNumber());
						defUseTable.addUse(repV, instr.getNumber());
					}
					
					ConstantValue repC = instr.getConstantValue();
					
					if (repC == null) continue;
					
					block.eliminateInstruction(instr);
					i--;
					
					replacements.put(instr.getResult(), repC);
					
					defUseTable.removeDefinition(instr.getNumber());
					defUseTable.removeUses(instr.getNumber());
					
					eliminationProtocol.append(String.format(" CP: (%03d) is now constant, replace %s with %s\n", 
														    instr.getNumber(),
															instr.getResult(),
															repC));
				}
			}
		}
	}
	
	public void removeUnusedInstructions() {
		for (int i = program.getBasicBlocks().size() - 1; i >= 0; i--) {
			BasicBlock block = program.getBasicBlocks().get(i);
			
			for (int j = block.getInstructions().size() - 1; j >= 0; j--) {
				Instruction instr = block.getInstructions().get(j);
				Value defValue = defUseTable.getDefinedValue(instr.getNumber());
				
				if (defValue == null) continue;
				if (instr.isEliminateable() == false) continue;
				
				if (defUseTable.hasUses(defValue) == false) {
					block.eliminateInstruction(instr);

					defUseTable.removeDefinition(instr.getNumber());
					defUseTable.removeUses(instr.getNumber());
					
					eliminationProtocol.append(String.format("USE: (%03d) is never used\n", 
														    instr.getNumber(), defValue));
					
					instr = (j > 0) ? block.getInstructions().get(j - 1) : null;
					if (j > 0 && instr.getOperator() == Instruction.Operator.adda) {
						block.eliminateInstruction(instr);
						j--;
						
						defUseTable.removeDefinition(instr.getNumber());
						defUseTable.removeUses(instr.getNumber());
						
						eliminationProtocol.append(String.format("USE: (%03d) is orphaned adda\n", 
															    instr.getNumber()));	
						
					}
				}
			}
		}
	}
	
	public void removeCommonSubexpressions() {
		HashMap<Value, Value> replacements = new HashMap<>();
		HashMap<Integer, SubexpressionTable> tables = new HashMap<>();
		
		for (BasicBlock block : program.getBasicBlocks()) {
			BasicBlock dominator = program.getDirectDominator(block);
			SubexpressionTable table;
			
			if (dominator == null) {
				table = new SubexpressionTable(program);
			}else{
				table = tables.get(dominator.getBlockNumber()).clone();
			}
			
			tables.put(block.getBlockNumber(), table);
			
			if (program.isMergeBlock(block)) {
				table.killLoadInstructions();
			}
			
			for (int i = 0; i < block.getInstructions().size(); i++) {
				Instruction currInstr = block.getInstructions().get(i);
				Instruction prevInstr = table.getPreviousInstruction(currInstr);
				
				if (prevInstr == null) {
					table.addInstruction(currInstr);
				}else{
					Value oldResult = currInstr.getResult();
					Value newResult = prevInstr.getResult();
					while (replacements.containsKey(newResult)) {
						newResult = replacements.get(newResult);
					}
					
					replacements.put(oldResult, newResult);
					
					block.eliminateInstruction(currInstr);
					i--;
					
					defUseTable.removeDefinition(currInstr.getNumber());
					defUseTable.removeUses(currInstr.getNumber());
					
					eliminationProtocol.append(String.format("CSE: (%03d) repeats (%03d), replace: %s with: %s\n", 
														    currInstr.getNumber(),
														    prevInstr.getNumber(),
														    oldResult,
														    newResult));	
					
					currInstr = (i >= 0) ? block.getInstructions().get(i) : null;
					if (i >= 0 && currInstr.getOperator() == Instruction.Operator.adda) {
						block.eliminateInstruction(currInstr);
						i--;
						
						defUseTable.removeDefinition(currInstr.getNumber());
						defUseTable.removeUses(currInstr.getNumber());
						
						eliminationProtocol.append(String.format("CSE: (%03d) is orphaned adda\n", 
															    currInstr.getNumber()));	
						
					}
				}
			}
		}
		

		for (BasicBlock block : program.getBasicBlocks()) {
			for (Instruction instr : block.getInstructions()) {
				for (int j = 0; j < 3; j++) {
					Value argV = instr.getArg(j);
					
					if (argV == null) continue;
					if (argV instanceof ArgumentsValue) {
						ArgumentsValue argsV = (ArgumentsValue) argV;
						argsV.replace(replacements);
					}
					if (replacements.containsKey(argV) == false) continue;
					
					Value repV = replacements.get(argV);
					
					instr.setArg(j, repV);
					
					defUseTable.removeUse(argV, instr.getNumber());
					defUseTable.addUse(repV, instr.getNumber());
				}
			}
		}
	}
	
}
