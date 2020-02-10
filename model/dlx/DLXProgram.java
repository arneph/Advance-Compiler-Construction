package model.dlx;

import java.util.*;

public class DLXProgram {
	private ArrayList<DLXBlock> blocks;
	
	public DLXProgram() {
		blocks = new ArrayList<>();
	}
	
	public ArrayList<DLXBlock> getBlocks() {
		return blocks;
	}
	
	public String toString() {
		StringBuilder bob = new StringBuilder();
		
		for (DLXBlock block : blocks) {
			bob.append("{" + block.number + "}\n");
			for (DLXInstruction instr : block.getInstructions()) {
				bob.append(instr.toString() + "\n");
			}
		}
		
		return bob.toString();
	}
	
}
