package processor;

import java.util.*;

import model.ssa.*;

public class SubexpressionTable {
	private Program program;
	private HashMap<Instruction.Operator, LinkedList<Instruction>> subexpressions;
	
	public SubexpressionTable(Program pgm) {
		program = pgm;
		subexpressions = new HashMap<>();
	}
	
	private static boolean equalArgs(Value a, Value b) {
		if ((a == null) != (b == null)) return false;
		if (a == null) return true;
		return a.equals(b);
	}
	
	public Instruction getPreviousInstruction(Instruction currInstr) {
		Instruction.Operator currOp = currInstr.getOperator();
		
		if (subexpressions.containsKey(currOp) == false) {
			return null;
		}
		
		for (Instruction prevInstr : subexpressions.get(currOp)) {
			Instruction.Operator prevOp = prevInstr.getOperator();
			Value prevX = prevInstr.getArgX();
			Value prevY = prevInstr.getArgY();
			Value currX = currInstr.getArgX();
			Value currY = currInstr.getArgY();
			
			switch (currOp) {
			case neg:
				if (equalArgs(prevX, currX) == false) continue;
				
				return prevInstr;
			case add:
			case mul:
			case adda:
				if (equalArgs(prevX, currX) &&
					equalArgs(prevY, currY)) {
					return prevInstr;
				}else if (equalArgs(prevX, currY) &&
						  equalArgs(prevY, currX)) {
					return prevInstr;
				}else{
					continue;
				}
			case sub:
			case div:
			case cmp:
				if (equalArgs(prevX, currX) == false) continue;
				if (equalArgs(prevY, currY) == false) continue;
				
				return prevInstr;
			case load:
				MemoryAddressValue currMemAddrValue = null;
				Value currOffset = null;
				if (currY instanceof MemoryAddressValue) {
					currMemAddrValue = (MemoryAddressValue) currInstr.getArgY();
					currOffset = new ConstantValue(0);
					
				}else if (currY instanceof ComputedValue) {
					ComputedValue currCompY = (ComputedValue) currY;
					Instruction currAddaInstr = program.getInstruction(currCompY.getInstructionNumber());
					
					currMemAddrValue = (MemoryAddressValue) currAddaInstr.getArgX();
					currOffset = currAddaInstr.getArgY();
				}
				
				// TODO: improve!
				if (prevOp == Instruction.Operator.call) {
					return null;
				}
				
				MemoryAddressValue prevMemAddrValue = null;
				Value prevOffset = null;
				if (prevY instanceof MemoryAddressValue) {
					prevMemAddrValue = (MemoryAddressValue) prevInstr.getArgY();
					prevOffset = new ConstantValue(0);
					
				}else if (currY instanceof ComputedValue) {
					ComputedValue prevCompY = (ComputedValue) prevY;
					Instruction prevAddaInstr = program.getInstruction(prevCompY.getInstructionNumber());
					
					prevMemAddrValue = (MemoryAddressValue) prevAddaInstr.getArgX();
					prevOffset = prevAddaInstr.getArgY();
				}
				
				if (prevOp == Instruction.Operator.store) {
					if (equalArgs(prevMemAddrValue, currMemAddrValue) == false) {
						continue;
					}
					if ((currOffset instanceof ConstantValue) == false) {
						return null;
					}
					if ((prevOffset instanceof ConstantValue) == false) {
						return null;
					}
					if (equalArgs(prevOffset, currOffset)) {
						return null;
					}
					continue;
				}
				
				if (equalArgs(prevMemAddrValue, currMemAddrValue) == false) continue;
				if (equalArgs(prevOffset, currOffset) == false) continue;
				
				return prevInstr;
			default:
				throw new IllegalStateException();
			}
					
		}
		
		return null;
	}
	
	public void addInstruction(Instruction instr) {
		Instruction.Operator op = instr.getOperator();
		
		if (op == Instruction.Operator.adda) return;
		if (op.ordinal() >= Instruction.Operator.move.ordinal() && 
			op != Instruction.Operator.call) {
			return;
		}
		
		if (op == Instruction.Operator.store || 
			op == Instruction.Operator.call) {
			op = Instruction.Operator.load;
		}
		
		subexpressions.putIfAbsent(op, new LinkedList<>());
		subexpressions.get(op).addFirst(instr);
	}
	
	public void killLoadInstructions() {
		subexpressions.put(Instruction.Operator.load, new LinkedList<>());
	}
	
	@SuppressWarnings("unchecked")
	public SubexpressionTable clone() {
		SubexpressionTable clone = new SubexpressionTable(program);
		
		for (Map.Entry<Instruction.Operator, LinkedList<Instruction>> entry : subexpressions.entrySet()) {
			clone.subexpressions.put(entry.getKey(), (LinkedList<Instruction>)entry.getValue().clone());
		}
		
		return clone;
	}
	
}
