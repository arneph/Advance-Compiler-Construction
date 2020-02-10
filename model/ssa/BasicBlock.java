package model.ssa;

import java.util.*;

public class BasicBlock {
	private Program program;
	private int blockNumber;
	private int depth;
	private ArrayList<Instruction> instructions;
	private boolean ifTopBlock;
	private boolean ifFollowBlock;
	private boolean whileTopBlock;
	private boolean whileFollowBlock;
	
	BasicBlock(Program pgm, int num, int d) {
		program = pgm;
		blockNumber = num;
		depth = d;
		instructions = new ArrayList<>();
		ifTopBlock = false;
		ifFollowBlock = false;
		whileTopBlock = false;
		whileFollowBlock = false;
	}
	
	public int getBlockNumber() {
		return blockNumber;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public List<Instruction> getInstructions() {
		return Collections.unmodifiableList(instructions);
	}
	
	public int getIndexOfLastNonBranchInstruction() {
		for (int i = instructions.size(); i > 0; i--) {
			Instruction instr = instructions.get(i - 1);
			
			if (instr.isBranch() == false) return i;
		}
		return 0;
	}
	
	public boolean endsWithEndInstruction() {
		if (instructions.size() == 0) return false;
		
		Instruction lastInstr = instructions.get(instructions.size() - 1);
		
		return lastInstr.getOperator() == Instruction.Operator.end;
	}
	
	public Instruction addInstruction() {
		Instruction instr = new Instruction(this, program.instructionCount++);
		
		instructions.add(instr);
		program.instructions.put(instr.getNumber(), instr);
		
		return instr;
	}
	
	public Instruction addPlaceholderInstruction(CodeGenerator gen) {
		Instruction instr = new Instruction(this, program.instructionCount++, gen);
		
		instructions.add(instr);
		program.instructions.put(instr.getNumber(), instr);
		
		return instr;
	}
	
	public Instruction addInstruction(int index) {
		Instruction instr = new Instruction(this, program.instructionCount++);
		
		instructions.add(index, instr);
		program.instructions.put(instr.getNumber(), instr);
		
		return instr;
	}
	
	public void eliminateInstruction(Instruction instr) {
		instructions.remove(instr);
		program.instructions.remove(instr.getNumber());
	}

	public boolean isIfTopBlock() {
		return ifTopBlock;
	}
	
	public void setIfTopBlock(boolean b) {
		ifTopBlock = b;
	}

	public boolean isIfFollowBlock() {
		return ifFollowBlock;
	}
	
	public void setIfFollowBlock(boolean b) {
		ifFollowBlock = b;
	}
	
	public boolean isWhileTopBlock() {
		return whileTopBlock;
	}
	
	public void setWhileTopBlock(boolean b) {
		whileTopBlock = b;
	}
	
	public boolean isWhileFollowBlock() {
		return whileFollowBlock;
	}
	
	public void setWhileFollowBlock(boolean b) {
		whileFollowBlock = b;
	}
	
	public BlockAddressValue getAddress() {
		return new BlockAddressValue(blockNumber);
	}
	
}
