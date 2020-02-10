package model.ssa;

public class BlockAddressValue extends Value {
	private int blockNumber;
	
	BlockAddressValue(int num) {
		blockNumber = num;
	}
	
	public boolean equals(Value v) {
		if (v instanceof BlockAddressValue) {
			BlockAddressValue a = (BlockAddressValue) v;
			
			return blockNumber == a.blockNumber;
 		}
		return false;
	}
	
	public String toString() {
		return "{" + blockNumber + "}";
	}
}
