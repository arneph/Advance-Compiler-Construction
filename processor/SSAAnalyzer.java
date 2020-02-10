package processor;

import model.ssa.*;

public class SSAAnalyzer {
	private Program program;
	
	public SSAAnalyzer(Program pgm) {
		program = pgm;
	}
	
	public DefUseTable getDefUseTable() {
		DefUseTable table = new DefUseTable();
		
		for (BasicBlock block : program.getBasicBlocks()) {
			for (int i = 0; i < block.getInstructions().size(); i++) {
				Instruction instr = block.getInstructions().get(i);
				
				switch(instr.getOperator()) {
				case neg:
					table.addUse(instr.getArgX(), instr.getNumber());
					table.addDefinition(instr.getResult(), instr.getNumber());
					break;
				case add:
				case sub:
				case mul:
				case div:
				case cmp:
				case adda:
					table.addUse(instr.getArgX(), instr.getNumber());
					table.addUse(instr.getArgY(), instr.getNumber());
					//table.addDefinition(instr.getResult(), instr.getNumber());
					break;
				case load:
					table.addDefinition(instr.getResult(), instr.getNumber());
					break;
				case store:
					table.addUse(instr.getArgX(), instr.getNumber());
					//table.addUse(instr.getArgY(), instr.getNumber());
					break;
				case move:
					table.addUse(instr.getArgX(), instr.getNumber());
					table.addDefinition(instr.getArgY(), instr.getNumber());
					break;
				case phi:
					table.addUse(instr.getArgX(), instr.getNumber());
					table.addUse(instr.getArgY(), instr.getNumber());
					table.addDefinition(instr.getArgZ(), instr.getNumber());
					break;
				case end:
					if (instr.getArgX() != null) {
						table.addUse(instr.getArgX(), instr.getNumber());
					}
					break;
				case bne:
				case beq:
				case ble:
				case blt:
				case bge:
				case bgt:
					table.addUse(instr.getArgX(), instr.getNumber());
					break;
				case read:
					table.addDefinition(instr.getResult(), instr.getNumber());
					break;
				case write:
					table.addUse(instr.getArgX(), instr.getNumber());
					break;
				case call:
					ArgumentsValue argsV = (ArgumentsValue) instr.getArgX();
					
					for (Value arg : argsV.getArguments()) {
						table.addUse(arg, instr.getNumber());
					}
					if (instr.getArgZ() != null) {
						table.addDefinition(instr.getArgZ(), instr.getNumber());
					}
					break;
				default:
					break;
				}
			}
		}
		
		return table;
	}
}
