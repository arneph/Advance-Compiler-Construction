package model.dlx;

import java.util.*;

public class DLXBlock {
	private ArrayList<DLXInstruction> instructions;
	
	public int number;
	
	public DLXBlock(int num) {
		instructions = new ArrayList<>();
		number = num;
	}
	
	public ArrayList<DLXInstruction> getInstructions() {
		return instructions;
	}
	
}
