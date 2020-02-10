package processor;

import java.util.*;

import model.dlx.*;

public class DLXAssembler {
	private DLXProgram program;
	
	public DLXAssembler(DLXProgram pgm) {
		program = pgm;
	}
	
	public void resolveLabels() {
		int instrCount = 0;
		for (DLXBlock block : program.getBlocks()) {
			for (DLXInstruction instr : block.getInstructions()) {
				instr.number = instrCount++;
			}
		}
		
		for (DLXBlock block : program.getBlocks()) {
			for (DLXInstruction instr : block.getInstructions()) {
				for (int i = 0; i < 3; i++) {
					String arg = instr.getArg(i);
					if (arg == null || arg.equals("_")) {
						arg = "0";
					}
					if (arg.matches("\\{[0-9]+\\}")) {
						String numStr = arg.substring(1, arg.length() - 1);
						int num = Integer.parseInt(numStr);
						
						while (program.getBlocks().get(num).getInstructions().size() == 0)
							num++;
						
						num = program.getBlocks().get(num).getInstructions().get(0).number;
						arg = Integer.toString(num - instr.number);
					}else if (arg.equals("gp")) {
						arg = "r30";
					}else if (arg.equals("sp")) {
						arg = "r29";
					}else if (arg.equals("fp")) {
						arg = "r28";
					}
					
					instr.setArg(i, arg);
				}
			}
		}
	}
	
	public ArrayList<Integer> generateCode() {
		ArrayList<Integer> machineCode = new ArrayList<>();
		
		for (DLXBlock block : program.getBlocks()) {
			for (DLXInstruction instr : block.getInstructions()) {
				DLXInstruction.Operator op = instr.op;
				DLXInstruction.Format fmt = op.getFormat();
				
				int code = 0;
				
				code |= op.getOpCode() << 26;
				
				if (fmt == DLXInstruction.Format.F1) {
					short a = generateArgument(instr.getArg(0));
					short b = generateArgument(instr.getArg(1));
					short c = generateArgument(instr.getArg(2));
					
					code |= (a & 0x001F) << 21;
					code |= (b & 0x001F) << 16;
					code |= (c & 0xFFFF) <<  0;
					
				}else if (fmt == DLXInstruction.Format.F2) {
					short a = generateArgument(instr.getArg(0));
					short b = generateArgument(instr.getArg(1));
					short c = generateArgument(instr.getArg(2));
					
					code |= (a & 0x001F) << 21;
					code |= (b & 0x001F) << 16;
					code |= (c & 0x001F) <<  0;
					
				}else if (fmt == DLXInstruction.Format.F3) {
					short c = generateArgument(instr.getArg(2));
					
					code |= (c & 0xFFFF) <<  0;
				}
				
				machineCode.add(code);
			}
		}
		
		return machineCode;
	}
	
	public short generateArgument(String arg) {
		try {
			if (arg.startsWith("#")) {
				return Short.parseShort(arg.substring(1));
			}else if (arg.startsWith("r")) {
				return Short.parseShort(arg.substring(1));
			}else{
				return Short.parseShort(arg);
			}
		} catch (NumberFormatException ex) {
			System.out.println(ex);
			return 0;
		}
	}
	
}
