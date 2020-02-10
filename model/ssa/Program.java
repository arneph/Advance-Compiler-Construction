package model.ssa;

import java.util.*;

import vcg.*;

public class Program {
	int instructionCount;
	HashMap<Integer, Instruction> instructions;
	
	private ArrayList<BasicBlock> basicBlocks;
	
	private HashMap<Integer, ArrayList<Integer>> children;
	private HashMap<Integer, ArrayList<Integer>> parents;
	
	private HashMap<Integer, Integer> dominators;
	private HashMap<Integer, ArrayList<Integer>> dominees;
	
	private HashMap<Integer, Integer> functionStartBlocks;
	
	public Program() {
		instructionCount = 0;
		instructions = new HashMap<>();
		
		basicBlocks = new ArrayList<>();
		
		children = new HashMap<>();
		parents = new HashMap<>();
		
		dominators = new HashMap<>();
		dominees = new HashMap<>();
	
		functionStartBlocks = new HashMap<>();
	}

	public List<BasicBlock> getBasicBlocks() {
		return Collections.unmodifiableList(basicBlocks);
	}
	
	public Instruction getInstruction(int instrNum) {
		return instructions.getOrDefault(instrNum, null);
	}
	
	public BasicBlock addBasicBlock(int depth) {
		BasicBlock block = new BasicBlock(this, basicBlocks.size(), depth);
		
		basicBlocks.add(block);
		
		return block;
	}
	
	public boolean isBranchBlock(BasicBlock block) {
		int blockNum = block.getBlockNumber();
		
		if (children.containsKey(blockNum) == false) {
			return false;
		}
		
		return children.get(blockNum).size() == 2;
	}
	
	public boolean isMergeBlock(BasicBlock block) {
		int blockNum = block.getBlockNumber();
		
		if (parents.containsKey(blockNum) == false) {
			return false;
		}
		
		return parents.get(blockNum).size() == 2;
	}
	
	public BasicBlock getLeftParent(BasicBlock block) {
		int blockNum = block.getBlockNumber();
		int parentNum = parents.get(blockNum).get(0);
		
		return basicBlocks.get(parentNum);
	}
	
	public BasicBlock getRightParent(BasicBlock block) {
		int blockNum = block.getBlockNumber();
		int parentNum = parents.get(blockNum).get(1);
		
		return basicBlocks.get(parentNum);
	}
	
	public List<BasicBlock> getParents(BasicBlock child) {
		int childNum = child.getBlockNumber();
		ArrayList<BasicBlock> results = new ArrayList<>(2);
		
		if (!parents.containsKey(childNum)) {
			return results;
		}
		
		for (int parentNum : parents.get(childNum)) {
			BasicBlock parent = basicBlocks.get(parentNum);
			
			results.add(parent);
		}
		
		return Collections.unmodifiableList(results);
	}
	
	public List<BasicBlock> getChildren(BasicBlock parent) {
		int parentNum = parent.getBlockNumber();
		ArrayList<BasicBlock> results = new ArrayList<>(2);
		
		if (!children.containsKey(parentNum)) {
			return results;
		}
		
		for (int childNum : children.get(parentNum)) {
			BasicBlock child = basicBlocks.get(childNum);
			
			results.add(child);
		}
		
		return Collections.unmodifiableList(results);
	}
	
	public void addEdge(BasicBlock parent, BasicBlock child) {
		int parentNum = parent.getBlockNumber();
		int childNum = child.getBlockNumber();
		
		if (parentNum == childNum) {
			throw new IllegalArgumentException();
		}
		
		children.putIfAbsent(parentNum, new ArrayList<>());
		children.get(parentNum).add(childNum);
		
		parents.putIfAbsent(childNum, new ArrayList<>());
		parents.get(childNum).add(parentNum);
	}
	
	public boolean isDominator(BasicBlock dominator, BasicBlock dominee) {
		int dominatorNum = dominator.getBlockNumber();
		int domineeNum = dominee.getBlockNumber();
		
		while (domineeNum != -1) {
			if (dominatorNum == domineeNum) return true;
			
			domineeNum = dominators.getOrDefault(domineeNum, -1);
		}
		
		return false;
	}
	
	public BasicBlock getDirectDominator(BasicBlock block) {
		int domineeNum = block.getBlockNumber();
		int dominatorNum = dominators.getOrDefault(domineeNum, -1);
		
		if (dominatorNum == -1) return null;
		
		return basicBlocks.get(dominatorNum);
	}
	
	public List<BasicBlock> getDominees(BasicBlock dominator) {
		int dominatorNum = dominator.getBlockNumber();
		ArrayList<BasicBlock> results = new ArrayList<>();
		
		if (dominees.containsKey(dominatorNum) == false) {
			return results;
		}
		
		for (int domineeNum : dominees.get(dominatorNum)) {
			BasicBlock dominee = basicBlocks.get(domineeNum);
			
			results.add(dominee);
		}
		
		return results;
	}
	
	public void addDominatior(BasicBlock dominator, BasicBlock dominee) {
		int dominatorNum = dominator.getBlockNumber();
		int domineeNum = dominee.getBlockNumber();
		
		if (dominatorNum == domineeNum) {
			throw new IllegalArgumentException();
		}
		
		dominators.put(domineeNum, dominatorNum);
		dominees.putIfAbsent(dominatorNum, new ArrayList<>());
		dominees.get(dominatorNum).add(domineeNum);
	}
	
	public BasicBlock getFunctionStartBlock(int id) {
		int startBlockNum = functionStartBlocks.getOrDefault(id, -1);
		
		if (startBlockNum == -1) return null;
		
		return basicBlocks.get(startBlockNum);
	}
	
	public void markFunctionStartBlock(BasicBlock startBlock, int id) {
		int startBlockNum = startBlock.getBlockNumber();
		
		functionStartBlocks.put(id, startBlockNum);
	}
	
	public ArrayList<VCGNode> getInstructionVCGNodes() {
		ArrayList<VCGNode> nodes = new ArrayList<>(basicBlocks.size());
		
		for (int i = 0; i < basicBlocks.size(); i++) {
			BasicBlock block = basicBlocks.get(i);
			VCGNode node = new VCGNode();
			
			node.setName("{" + block.getBlockNumber() + "}");
			
			for (Instruction instr : block.getInstructions()) {
				node.getAttributes().add(new VCGAttribute<Object>(instr.toString(), null));
			}
			
			nodes.add(node);
		}
		
		for (int i = 0; i < basicBlocks.size(); i++) {
			if (!children.containsKey(i)) continue;
			
			VCGNode node = nodes.get(i);
			
			for (int childIndex : children.get(i)) {
				node.getChildren().add(nodes.get(childIndex));
			}
		}
		
		return nodes;
	}
	
	public ArrayList<VCGNode> getControlFlowVCGNodes() {
		ArrayList<VCGNode> nodes = new ArrayList<>(basicBlocks.size());

		for (int i = 0; i < basicBlocks.size(); i++) {
			BasicBlock block = basicBlocks.get(i);
			VCGNode node = new VCGNode();
			
			node.setName("{" + block.getBlockNumber() + "}");
			
			nodes.add(node);
		}
		
		for (int i = 0; i < basicBlocks.size(); i++) {
			if (!children.containsKey(i)) continue;
			
			VCGNode node = nodes.get(i);
			
			for (int childIndex : children.get(i)) {
				node.getChildren().add(nodes.get(childIndex));
			}
		}
		
		return nodes;
	}
	
	public ArrayList<VCGNode> getDominationVCGNodes() {
		ArrayList<VCGNode> nodes = new ArrayList<>(basicBlocks.size());

		for (int i = 0; i < basicBlocks.size(); i++) {
			BasicBlock block = basicBlocks.get(i);
			VCGNode node = new VCGNode();
			
			node.setName("{" + block.getBlockNumber() + "}");
			
			nodes.add(node);
		}
		
		for (int i = 0; i < basicBlocks.size(); i++) {
			if (!dominators.containsKey(i)) continue;
			int j = dominators.get(i);
			
			VCGNode child = nodes.get(i);
			VCGNode parent = nodes.get(j);
			
			parent.getChildren().add(child);
		}
		
		return nodes;
	}
	
	public String toString() {
		StringBuilder bob = new StringBuilder();
		
		for (int i = 0; i < basicBlocks.size(); i++) {
			BasicBlock block = basicBlocks.get(i);
			
			bob.append("{" + block.getBlockNumber() + "}\n");
			
			for (Instruction instr : block.getInstructions()) {
				bob.append(instr.toString());
				bob.append("\n");
			}
		}
		
		return bob.toString();
	}
	
}
