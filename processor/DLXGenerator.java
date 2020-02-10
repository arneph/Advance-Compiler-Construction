package processor;

import java.util.*;

import model.ssa.*;
import model.dlx.*;

public class DLXGenerator {
	private SymbolTable symbolTable;
	private Program inProgram;
	private DLXProgram outProgram;
	
	private int globalSpace;
	
	public DLXGenerator(SymbolTable table, Program program, int gs) {
		symbolTable = table;
		inProgram = program;
		outProgram = new DLXProgram();
		
		globalSpace = gs;
	}
	
	public DLXProgram generate() {
		for (BasicBlock inBlock : inProgram.getBasicBlocks()) {
			DLXBlock outBlock = new DLXBlock(inBlock.getBlockNumber());
			
			outProgram.getBlocks().add(outBlock);
			
			if (inBlock.getDepth() == 0 && inProgram.getParents(inBlock).size() == 0) {
				DLXInstruction instr1 = new DLXInstruction(-1);
				instr1.op = DLXInstruction.Operator.SUBI;
				instr1.arg0 = "sp";
				instr1.arg1 = "gp";
				instr1.arg2 = "#" + globalSpace;
				
				DLXInstruction instr2 = new DLXInstruction(-1);
				instr2.op = DLXInstruction.Operator.ADD;
				instr2.arg0 = "fp";
				instr2.arg1 = "r0";
				instr2.arg2 = "sp";

				outBlock.getInstructions().add(instr1);
				outBlock.getInstructions().add(instr2);
				
			}else if (inBlock.getDepth() > 0 && inProgram.getParents(inBlock).size() == 0) {
				DLXInstruction instr1 = new DLXInstruction(-1);
				instr1.op = DLXInstruction.Operator.PSH;
				instr1.arg0 = "r31";
				instr1.arg1 = "sp";
				instr1.arg2 = "#-4";
				
				DLXInstruction instr2 = new DLXInstruction(-1);
				instr2.op = DLXInstruction.Operator.PSH;
				instr2.arg0 = "fp";
				instr2.arg1 = "sp";
				instr2.arg2 = "#-4";
				
				DLXInstruction instr3 = new DLXInstruction(-1);
				instr3.op = DLXInstruction.Operator.ADD;
				instr3.arg0 = "fp";
				instr3.arg1 = "r0";
				instr3.arg2 = "sp";
				
				DLXInstruction instr4 = new DLXInstruction(-1);
				instr4.op = DLXInstruction.Operator.SUBI;
				instr4.arg0 = "sp";
				instr4.arg1 = "sp";
				instr4.arg2 = "#" + 0; // TODO: find spaaaace!
				
				outBlock.getInstructions().add(instr1);
				outBlock.getInstructions().add(instr2);
				outBlock.getInstructions().add(instr3);
				outBlock.getInstructions().add(instr4);
				
			}
			
			boolean foundEnd = false;
			Instruction addaInstr = null;
			for (Instruction inInstr : inBlock.getInstructions()) {
				if (inInstr.getOperator() == Instruction.Operator.adda) {
					addaInstr = inInstr;
					continue;
				}
				
				ArrayList<DLXInstruction> outInstrs = generateInstructions(inInstr, addaInstr);
				
				addaInstr = null;
				outBlock.getInstructions().addAll(outInstrs);
				
				if (inInstr.getOperator() == Instruction.Operator.end) {
					foundEnd = true;
					break;
				}
			}
			
			if (inBlock.getDepth() > 0 && foundEnd) {
				DLXInstruction instr1 = new DLXInstruction(-1);
				instr1.op = DLXInstruction.Operator.ADD;
				instr1.arg0 = "sp";
				instr1.arg1 = "r0";
				instr1.arg2 = "fp";
				
				DLXInstruction instr2 = new DLXInstruction(-1);
				instr2.op = DLXInstruction.Operator.POP;
				instr2.arg0 = "fp";
				instr2.arg1 = "sp";
				instr2.arg2 = "#4";
				
				DLXInstruction instr3 = new DLXInstruction(-1);
				instr3.op = DLXInstruction.Operator.POP;
				instr3.arg0 = "r31";
				instr3.arg1 = "sp";
				instr3.arg2 = "#4";
				
				DLXInstruction instr4 = new DLXInstruction(-1);
				instr4.op = DLXInstruction.Operator.RET;
				instr4.arg0 = "0";
				instr4.arg1 = "0";
				instr4.arg2 = "r31";
				
				outBlock.getInstructions().add(instr1);
				outBlock.getInstructions().add(instr2);
				outBlock.getInstructions().add(instr3);
				outBlock.getInstructions().add(instr4);
			}else if (inBlock.getDepth() == 0 && foundEnd) {
				DLXInstruction termInstr = new DLXInstruction(-1);
				termInstr.op = DLXInstruction.Operator.RET;
				termInstr.arg0 = "0";
				termInstr.arg1 = "0";
				termInstr.arg2 = "r0";
				
				outBlock.getInstructions().add(termInstr);
			}
		}
		
		return outProgram;
	}
	
	private ArrayList<DLXInstruction> generateInstructions(Instruction inInstr, Instruction inHelpInstr) {
		Value inArgX = inInstr.getArgX();
		Value inArgY = inInstr.getArgY();
		Value inArgZ = inInstr.getArgZ();

		if (inArgX instanceof WhitespaceValue) inArgX = null;
		if (inArgY instanceof WhitespaceValue) inArgY = null;
		if (inArgZ instanceof WhitespaceValue) inArgZ = null;
		
		String outArgX = generateArg(inArgX);
		String outArgY = generateArg(inArgY);
		String outArgZ = generateArg(inArgZ);

		ArrayList<DLXInstruction> outInstrs = new ArrayList<>();
		
		DLXInstruction outInstr0 = null;
		DLXInstruction outInstr1 = new DLXInstruction(inInstr.getNumber());
		
		switch (inInstr.getOperator()) {
		case neg:
			outInstr1.op = DLXInstruction.Operator.MULI;
			outInstr1.arg0 = outArgZ;
			outInstr1.arg1 = outArgX;
			outInstr1.arg2 = "#-1";
			break;
		case add:
			if (inArgX instanceof ConstantValue) {
				String tmpArg = outArgX;
				outArgX = outArgY;
				outArgY = tmpArg;
				outInstr1.op = DLXInstruction.Operator.ADDI;
			}else if (inArgY instanceof ConstantValue) {
				outInstr1.op = DLXInstruction.Operator.ADDI;
			}else {
				outInstr1.op = DLXInstruction.Operator.ADD;
			}
			outInstr1.arg0 = outArgZ;
			outInstr1.arg1 = outArgX;
			outInstr1.arg2 = outArgY;
			break;
		case sub:
			if (inArgX instanceof ConstantValue) {
				outInstr0 = new DLXInstruction(inInstr.getNumber());
				outInstr0.op = DLXInstruction.Operator.ADDI;
				outInstr0.arg0 = "r27";
				outInstr0.arg1 = "r0";
				outInstr0.arg2 = outArgX;
				
				outArgX = "r27";
				outInstr1.op = DLXInstruction.Operator.SUB;
			}else if (inArgY instanceof ConstantValue) {
				outInstr1.op = DLXInstruction.Operator.SUBI;
			}else {
				outInstr1.op = DLXInstruction.Operator.SUB;
			}
			outInstr1.arg0 = outArgZ;
			outInstr1.arg1 = outArgX;
			outInstr1.arg2 = outArgY;
			break;
		case mul:
			if (inArgX instanceof ConstantValue) {
				String tmpArg = outArgX;
				outArgX = outArgY;
				outArgY = tmpArg;
				outInstr1.op = DLXInstruction.Operator.MULI;
			}else if (inArgY instanceof ConstantValue) {
				outInstr1.op = DLXInstruction.Operator.MULI;
			}else {
				outInstr1.op = DLXInstruction.Operator.MUL;
			}
			outInstr1.arg0 = outArgZ;
			outInstr1.arg1 = outArgX;
			outInstr1.arg2 = outArgY;
			break;
		case div:
			if (inArgX instanceof ConstantValue) {
				outInstr0 = new DLXInstruction(inInstr.getNumber());
				outInstr0.op = DLXInstruction.Operator.ADDI;
				outInstr0.arg0 = "r27";
				outInstr0.arg1 = "r0";
				outInstr0.arg2 = outArgX;
				
				outArgX = "r27";
				outInstr1.op = DLXInstruction.Operator.DIV;
			}else if (inArgY instanceof ConstantValue) {
				outInstr1.op = DLXInstruction.Operator.DIVI;
			}else {
				outInstr1.op = DLXInstruction.Operator.DIV;
			}
			outInstr1.arg0 = outArgZ;
			outInstr1.arg1 = outArgX;
			outInstr1.arg2 = outArgY;
			break;
		case cmp:
			if (inArgX instanceof ConstantValue) {
				outInstr0 = new DLXInstruction(inInstr.getNumber());
				outInstr0.op = DLXInstruction.Operator.ADDI;
				outInstr0.arg0 = "r27";
				outInstr0.arg1 = "r0";
				outInstr0.arg2 = outArgX;
				
				outArgX = "r27";
				outInstr1.op = DLXInstruction.Operator.CMP;
			}else if (inArgY instanceof ConstantValue) {
				outInstr1.op = DLXInstruction.Operator.CMPI;
			}else {
				outInstr1.op = DLXInstruction.Operator.CMP;
			}
			outInstr1.arg0 = outArgZ;
			outInstr1.arg1 = outArgX;
			outInstr1.arg2 = outArgY;
			break;
		case load:
		{   MemoryAddressValue memAddrValue;
			Value offsetValue;
			if (inHelpInstr != null) {
				memAddrValue = (MemoryAddressValue) inHelpInstr.getArgX();
				offsetValue = inHelpInstr.getArgY();
			}else{
				memAddrValue = (MemoryAddressValue) inInstr.getArgY();
				offsetValue = new ConstantValue(0);
			}
			int id = memAddrValue.getID();
			
			boolean rel = symbolTable.hasRelativeOffset(id);
			int offset = symbolTable.getOffset(id);
			
			if (offsetValue instanceof ConstantValue) {
				offset += ((ConstantValue) offsetValue).getValue();
				
				outInstr1.op = DLXInstruction.Operator.LDW;
				outInstr1.arg2 = "#" + offset;
			}else{
				outInstr0 = new DLXInstruction(inInstr.getNumber());
				outInstr0.op = DLXInstruction.Operator.ADDI;
				outInstr0.arg0 = "r27";
				outInstr0.arg1 = offsetValue.toString();
				outInstr0.arg2 = "#" + offset;
				
				outInstr1.op = DLXInstruction.Operator.LDX;
				outInstr1.arg2 = "r27";
			}
			
			if (rel == false) {
				outInstr1.arg1 = "gp";
			}else{
				outInstr1.arg1 = "fp";
			}
			if (inArgZ == null) {
				outInstr1.arg0 = "r0";
			}else{
				outInstr1.arg0 = inArgZ.toString();				
			}
		}   break;
		case end:
			if (inArgX == null) return outInstrs;
		case store:
		{   MemoryAddressValue memAddrValue;
			Value offsetValue;
			if (inHelpInstr != null) {
				memAddrValue = (MemoryAddressValue) inHelpInstr.getArgX();
				offsetValue = inHelpInstr.getArgY();
			}else{
				memAddrValue = (MemoryAddressValue) inInstr.getArgY();
				offsetValue = new ConstantValue(0);
			}
			int id = memAddrValue.getID();
			
			boolean rel = symbolTable.hasRelativeOffset(id);
			int offset = symbolTable.getOffset(id);
			
			if (offsetValue instanceof ConstantValue) {
				offset += ((ConstantValue) offsetValue).getValue();
				
				outInstr1.op = DLXInstruction.Operator.STW;
				outInstr1.arg2 = "#" + offset;
			}else{
				outInstr0 = new DLXInstruction(inInstr.getNumber());
				outInstr0.op = DLXInstruction.Operator.ADDI;
				outInstr0.arg0 = "r27";
				outInstr0.arg1 = offsetValue.toString();
				outInstr0.arg2 = "#" + offset;
				
				outInstr1.op = DLXInstruction.Operator.STX;
				outInstr1.arg2 = "r27";
			}
			
			if (rel == false) {
				outInstr1.arg1 = "gp";
			}else{
				outInstr1.arg1 = "fp";
			}
			
			if (inArgX instanceof ConstantValue) {
				DLXInstruction outLoadInstr = new DLXInstruction(inInstr.getNumber());
				outLoadInstr.op = DLXInstruction.Operator.ADDI;
				outLoadInstr.arg0 = "r27";
				outLoadInstr.arg1 = "r0";
				outLoadInstr.arg2 = inArgX.toString();
				
				outInstrs.add(outLoadInstr);
				
				outInstr1.arg0 = "r27";
			}else {
				outInstr1.arg0 = inArgX.toString();
			}
		}   break;
		case move:
			if (inArgX instanceof ConstantValue) {
				outInstr1.op = DLXInstruction.Operator.ADDI;
			}else{
				outInstr1.op = DLXInstruction.Operator.ADD;
			}
			outInstr1.arg1 = "r0";
			outInstr1.arg2 = outArgX;
			outInstr1.arg0 = outArgZ;
			break;
		case bra:
			outInstr1.op = DLXInstruction.Operator.BEQ;
			outInstr1.arg0 = "r0";
			outInstr1.arg2 = outArgY;
			break;
		case beq:
		case bne:
		case blt:
		case bge:
		case ble:
		case bgt:
			if (inArgX instanceof ConstantValue) {
				outInstr0 = new DLXInstruction(inInstr.getNumber());
				outInstr0.op = DLXInstruction.Operator.ADDI;
				outInstr0.arg0 = "r27";
				outInstr0.arg1 = "r0";
				outInstr0.arg2 = outArgX;
				
				outArgX = "r27";
			}
			
			outInstr1.op = convertCompareOp(inInstr.getOperator());
			outInstr1.arg0 = outArgX;
			outInstr1.arg2 = outArgY;
			break;
		case read:
			outInstr1.op = DLXInstruction.Operator.RDD;
			outInstr1.arg0 = outArgZ;
			break;
		case write:
			if (inArgX instanceof ConstantValue) {
				outInstr0 = new DLXInstruction(inInstr.getNumber());
				outInstr0.op = DLXInstruction.Operator.ADDI;
				outInstr0.arg0 = "r27";
				outInstr0.arg1 = "r0";
				outInstr0.arg2 = outArgX;
				
				outArgX = "r27";
			}
			
			outInstr1.op = DLXInstruction.Operator.WRD;
			outInstr1.arg1 = outArgX;
			break;
		case writeNL:
			outInstr1.op = DLXInstruction.Operator.WRL;
			break;
		case call:
			// Save live registers:
			for (Value liveValue : inInstr.getLiveValues()) {
				if (liveValue instanceof RegisterValue) {
					DLXInstruction pushInstr = new DLXInstruction(inInstr.getNumber());
					pushInstr.op = DLXInstruction.Operator.PSH;
					pushInstr.arg0 = liveValue.toString();
					pushInstr.arg1 = "sp";
					pushInstr.arg2 = "#-4";
					
					outInstrs.add(pushInstr);
					
				}else if (liveValue instanceof MemoryAddressValue) {
					continue;
				}else{
					throw new IllegalStateException();
				}
			}
			
			// Allocate return value space:
			if (inArgZ != null) {
				DLXInstruction pushInstr = new DLXInstruction(inInstr.getNumber());
				pushInstr.op = DLXInstruction.Operator.SUBI;
				pushInstr.arg0 = "sp";
				pushInstr.arg1 = "sp";
				pushInstr.arg2 = "#4";
				
				outInstrs.add(pushInstr);
			}
			
			// Push parameters:
			ArgumentsValue argsValue = (ArgumentsValue) inArgX;
			for (Value argValue : argsValue.getArguments()) {
				DLXInstruction pushInstr = new DLXInstruction(inInstr.getNumber());
				pushInstr.op = DLXInstruction.Operator.PSH;
				pushInstr.arg0 = argValue.toString();
				pushInstr.arg1 = "sp";
				pushInstr.arg2 = "#-4";
				
				outInstrs.add(pushInstr);
			}
			
			DLXInstruction bsrInstr = new DLXInstruction(inInstr.getNumber());
			bsrInstr.op = DLXInstruction.Operator.BSR;
			bsrInstr.arg0 = "_";
			bsrInstr.arg1 = "_";
			bsrInstr.arg2 = outArgY;
			
			outInstrs.add(bsrInstr);
			
			// Pop parameters:
			if (argsValue.getArguments().size() > 0) {
				DLXInstruction addInstr = new DLXInstruction(inInstr.getNumber());
				addInstr.op = DLXInstruction.Operator.ADDI;
				addInstr.arg0 = "sp";
				addInstr.arg1 = "sp";
				addInstr.arg2 = "#" + 4 * argsValue.getArguments().size();
				
				outInstrs.add(addInstr);
			}
			
			// Pop return value:
			if (inArgZ != null && inArgZ instanceof RegisterValue) {
				DLXInstruction pushInstr = new DLXInstruction(inInstr.getNumber());
				pushInstr.op = DLXInstruction.Operator.POP;
				pushInstr.arg0 = outArgZ;
				pushInstr.arg1 = "sp";
				pushInstr.arg2 = "#4";
				
				outInstrs.add(pushInstr);
			}else{
				DLXInstruction addInstr = new DLXInstruction(inInstr.getNumber());
				addInstr.op = DLXInstruction.Operator.ADDI;
				addInstr.arg0 = "sp";
				addInstr.arg1 = "sp";
				addInstr.arg2 = "#4";
				
				outInstrs.add(addInstr);
			}
			
			// Restore live registers:
			for (Value liveValue : inInstr.getLiveValues()) {
				if (liveValue instanceof RegisterValue) {
					DLXInstruction popInstr = new DLXInstruction(inInstr.getNumber());
					popInstr.op = DLXInstruction.Operator.POP;
					popInstr.arg0 = liveValue.toString();
					popInstr.arg1 = "sp";
					popInstr.arg2 = "#4";
					
					outInstrs.add(popInstr);
				}
			}
			return outInstrs;
		default:
			return null;
		}
		
		if (outInstr1.arg0 == null) outInstr1.arg0 = "_";
		if (outInstr1.arg1 == null) outInstr1.arg1 = "_";
		if (outInstr1.arg2 == null) outInstr1.arg2 = "_";
		
		if (outInstr1.op == null) {
			return outInstrs;
		}
		
		if (outInstr0 != null) {
			if (outInstr0.arg0 == null) outInstr0.arg0 = "_";
			if (outInstr0.arg1 == null) outInstr0.arg1 = "_";
			if (outInstr0.arg2 == null) outInstr0.arg2 = "_";
			outInstrs.add(outInstr0);
		}
		
		outInstrs.add(outInstr1);
		
		return outInstrs;
	}
	
	private String generateArg(Value inValue) {
		if (inValue == null) return null;
		if (inValue instanceof ConstantValue || 
			inValue instanceof RegisterValue ||
			inValue instanceof BlockAddressValue) {
			return inValue.toString();
		}else if (inValue instanceof WhitespaceValue || 
				  inValue instanceof ArgumentsValue || 
				  inValue instanceof MemoryAddressValue || 
				  inValue instanceof ComputedValue) {
			return null;
		}else{
			throw new IllegalStateException();
		}
	}
	
	private static DLXInstruction.Operator convertCompareOp(Instruction.Operator op) {
		if (op == Instruction.Operator.bne) return DLXInstruction.Operator.BNE;
		if (op == Instruction.Operator.beq) return DLXInstruction.Operator.BEQ;
		if (op == Instruction.Operator.blt) return DLXInstruction.Operator.BLT;
		if (op == Instruction.Operator.ble) return DLXInstruction.Operator.BLE;
		if (op == Instruction.Operator.bge) return DLXInstruction.Operator.BGE;
		if (op == Instruction.Operator.bgt) return DLXInstruction.Operator.BGT;
		return null;
	}
	
}
