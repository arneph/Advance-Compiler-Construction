package processor;

import java.util.*;

import model.ssa.*;

import vcg.*;

public class RegisterAllocator {
	private Program program;
	private DefUseTable defUseTable;
	
	private HashMap<Value, HashSet<Value>> graph;
	private HashMap<Value, Integer> colors;
	private HashMap<Value, HashSet<Value>> phiPartners;
	
	private HashMap<Value, Value> replacements;

	private StringBuilder liveRangeTrace;
	private StringBuilder regAssignmentsTrace;
	
	public static int moveInstrCount = 0;
	
	public RegisterAllocator(Program pgm, DefUseTable dut, StringBuilder p, StringBuilder r) {
		program = pgm;
		defUseTable = dut;
		
		graph = new HashMap<>();
		colors = new HashMap<>();
		phiPartners = new HashMap<>();
		
		replacements = new HashMap<>();
		
		liveRangeTrace = p;
		regAssignmentsTrace = r;
	}
	
	public void allocate() {
		findInterferences();
		findColoring();
		findReplacements();
		applyReplacements();
	}
	
	private static boolean needsColor(Value v) {
		if (v instanceof ConstantValue) return false;
		if (v instanceof MemoryAddressValue) return false;
		
		return true;
	}
	
	private void addValue(Value v) {
		if (needsColor(v) == false) {
			return;
		}
		
		graph.putIfAbsent(v, new HashSet<>());
	}
	
	private void addInterference(Value a, Value b) {
		if (a.equals(b)) {
			return;
		}
		if (needsColor(a) == false ||
			needsColor(b) == false) {
			return;
		}
		if (graph.containsKey(a) == false || 
			graph.containsKey(b) == false) {
			return;
		}
		
		graph.get(a).add(b);
		graph.get(b).add(a);
	}
	
	private void addInterferences(HashSet<Value> a, Value v) {
		for (Value va : a) {
			addInterference(va, v);
		}
	}
	
	private void addPhiPartners(Value a, Value b) {
		if (needsColor(a) == false) return;
		if (needsColor(b) == false) return;
		
		phiPartners.putIfAbsent(a, new HashSet<>());
		phiPartners.putIfAbsent(b, new HashSet<>());
		phiPartners.get(a).add(b);
		phiPartners.get(b).add(a);
	}
	
	private void findInterferences() {
		for (BasicBlock block : program.getBasicBlocks()) {
			if (program.getChildren(block).size() > 0) continue;
			
			findInterferencesFromBlock(block);
		}
	}
	
	private void findInterferencesFromBlock(BasicBlock start) {
		HashMap<BasicBlock, HashSet<Value>> inLiveValues = new HashMap<>();
		
		inLiveValues.put(start, new HashSet<>());
		
		HashMap<BasicBlock, Integer> whileTopBlocksCounts = new HashMap<>();
		HashSet<BasicBlock> ifTopBlocksSet = new HashSet<>();
		LinkedList<BasicBlock> ifTopBlocksStack = new LinkedList<>();
		LinkedList<BasicBlock> queue = new LinkedList<>();
		
		queue.add(start);
		
		while (queue.size() > 0 || ifTopBlocksStack.size() > 0) {
			BasicBlock block;
			
			if (queue.size() > 0) {
				block = queue.removeLast();
				
			}else{
				liveRangeTrace.append("| ");
				
				block = ifTopBlocksStack.pop();
				
				ifTopBlocksSet.remove(block);
			}
			liveRangeTrace.append(String.format("{%d} ", block.getBlockNumber()));
			
			List<BasicBlock> parents = program.getParents(block);
			
			HashSet<Value> liveValues = inLiveValues.get(block);
			HashSet<Value> outLeftLiveValues = new HashSet<>();
			HashSet<Value> outRightLiveValues = new HashSet<>();
			
			findInterferencesInBlock(block,
									liveValues, 
									outLeftLiveValues, 
									outRightLiveValues);
			
			if (parents.size() == 1) {
				BasicBlock parent = parents.get(0);
				
				inLiveValues.putIfAbsent(parent, new HashSet<>());
				inLiveValues.get(parent).addAll(liveValues);
				
			}else if (parents.size() == 2) {
				BasicBlock leftParent = parents.get(0);
				BasicBlock rightParent = parents.get(1);
				
				inLiveValues.putIfAbsent(leftParent, new HashSet<>());
				inLiveValues.get(leftParent).addAll(outLeftLiveValues);
				
				inLiveValues.putIfAbsent(rightParent, new HashSet<>());
				inLiveValues.get(rightParent).addAll(outRightLiveValues);
			}
			
			if (block.isWhileTopBlock()) {
				if (whileTopBlocksCounts.getOrDefault(block, 0) == 0) {
					whileTopBlocksCounts.put(block, 1);
					
					BasicBlock rightParent = parents.get(1);
					
					queue.add(rightParent);
					
				}else if (whileTopBlocksCounts.get(block) == 1){
					whileTopBlocksCounts.put(block, 2);
					
					BasicBlock leftParent = parents.get(0);
					
					if (ifTopBlocksSet.contains(leftParent)) continue;
					
					queue.add(leftParent);	
				}
				
			}else if (block.isIfFollowBlock()) {
				BasicBlock topBlock = program.getDirectDominator(block);
				
				ifTopBlocksSet.add(topBlock);
				ifTopBlocksStack.push(topBlock);
				queue.addAll(program.getParents(block));
				
			}else if (parents.size() > 0) {
				BasicBlock parent = program.getParents(block).get(0);
				
				if (ifTopBlocksSet.contains(parent)) continue;
				
				queue.add(parent);
			}
		}
		
		liveRangeTrace.append("\n");
	}
	
	private void findInterferencesInBlock(BasicBlock block,
								         HashSet<Value> liveValues,
								         HashSet<Value> leftLiveValues,
								         HashSet<Value> rightLiveValues) {
		for (int i = block.getInstructions().size() - 1; i >= 0; i--) {
			Instruction instr = block.getInstructions().get(i);
			int instrNum = instr.getNumber();
			
			Value definedValue = defUseTable.getDefinedValue(instrNum);
			
			if (definedValue != null) {
				liveValues.remove(definedValue);
			}
			
			if (instr.getOperator() == Instruction.Operator.call) {
				instr.setLiveValues(liveValues);
			}
			
			if (instr.getOperator() != Instruction.Operator.phi) {
				for (Value usedValue : defUseTable.getUsedValues(instrNum)) {
					if (usedValue instanceof ConstantValue) continue;
					
					addValue(usedValue);
					addInterferences(liveValues, usedValue);
					
					liveValues.add(usedValue);
				}
				
			}else{
				Value argX = instr.getArgX();
				Value argY = instr.getArgY();
				Value argZ = instr.getArgZ();
				
				addValue(argX);
				addValue(argY);
				addValue(argZ);
				addInterferences(liveValues, argX);
				addInterferences(liveValues, argY);
				addInterferences(leftLiveValues, argX);
				addInterferences(rightLiveValues, argY);
				addPhiPartners(argX, argZ);
				addPhiPartners(argY, argZ);
				
				leftLiveValues.add(argX);
				rightLiveValues.add(argY);
			}
		}
		
		leftLiveValues.addAll(liveValues);
		rightLiveValues.addAll(liveValues);
	}
	
	private void findColoring() {
		for (Value value : graph.keySet()) {
			colors.put(value, -1);
		}
		
		ArrayList<Value> sortedValues = new ArrayList<>(graph.keySet());
		sortedValues.sort((a, b) -> {
			if (phiPartners.containsKey(a) && 
				phiPartners.containsKey(b) == false) {
				return +1;
			}else if (phiPartners.containsKey(b) && 
					  phiPartners.containsKey(a) == false) {
				return -1;
			}
			return graph.get(b).size() - graph.get(a).size();	
		});

		int potentialPartnerCount = 0;
		colorFinder:
		for (int i = 0; i < sortedValues.size(); i++) {
			Value value = sortedValues.get(i);
			HashSet<Value> neighbors = graph.get(value);
			HashSet<Integer> badColors = new HashSet<>();
			
			for (Value neighbor : neighbors) {
				badColors.add(colors.get(neighbor));
			}
			
			regAssignmentsTrace.append(String.format("%3d assigning %5s... ", i, value));
			if (phiPartners.containsKey(value)) {
				regAssignmentsTrace.append(String.format("\n%5s is looking for a partner:\n", value));
			}
			
			if (potentialPartnerCount > 0) potentialPartnerCount--;
			for (Value partner : phiPartners.getOrDefault(value, new HashSet<>())) {
				int partnerColor = colors.getOrDefault(partner, -1);
				
				if (partnerColor != -1) {
					if (badColors.contains(partnerColor)) {
						regAssignmentsTrace.append(String.format("%5s + %5s = </3\n", value, partner));
						continue;
					}
					if (colors.get(value) != -1) continue;
					colors.put(value, partnerColor);
					regAssignmentsTrace.append(String.format("%5s + %5s = <3\n", value, partner));
				}else{
					int index = i + 1 + potentialPartnerCount++;
					if (index < sortedValues.size()) {
						sortedValues.remove(partner);
						sortedValues.add(index, partner);
					}
					regAssignmentsTrace.append(String.format("%5s + %5s = ???\n", value, partner));
				}
			}
			if (colors.get(value) != -1) {
				regAssignmentsTrace.append("found color: " + colors.get(value) + "\n");
				continue;
			}
			
			for (int color = 0; color <= neighbors.size(); color++) {
				if (badColors.contains(color)) continue;
				colors.put(value, color);
				regAssignmentsTrace.append("found color: " + color + "\n");
				continue colorFinder;
			}
		}
	}
	
	private void findReplacements() {
		for (Value value : graph.keySet()) {
			int color = colors.get(value);
			replacements.put(value, new RegisterValue(color + 1));
		}
	}
	
	private void applyReplacements() {
		for (BasicBlock block : program.getBasicBlocks()) {
			for (int i = 0; i < block.getInstructions().size(); i++) {
				Instruction instr = block.getInstructions().get(i);
				
				if (instr.getOperator() == Instruction.Operator.phi) {
					block.eliminateInstruction(instr);
					i--;
					
					Value argX = instr.getArgX();
					Value argY = instr.getArgY();
					Value argZ = instr.getArgZ();
					Value argXR = replacements.getOrDefault(argX, argX);
					Value argYR = replacements.getOrDefault(argY, argY);
					Value argZR = replacements.get(argZ);
					
					if (argXR.equals(argZR) == false) {
						BasicBlock leftParent = program.getLeftParent(block);
						int insertionIndex = leftParent.getIndexOfLastNonBranchInstruction();
						Instruction moveInstr = leftParent.addInstruction(insertionIndex);
						moveInstr.setOperator(Instruction.Operator.move);
						moveInstr.setArgX(argXR);
						moveInstr.setArgZ(argZR);
						moveInstrCount++;
					}
					if (argYR.equals(argZR) == false) {
						BasicBlock rightParent = program.getRightParent(block);
						int insertionIndex = rightParent.getIndexOfLastNonBranchInstruction();
						Instruction moveInstr = rightParent.addInstruction(insertionIndex);
						moveInstr.setOperator(Instruction.Operator.move);
						moveInstr.setArgX(argYR);
						moveInstr.setArgZ(argZR);
						moveInstrCount++;
					}
					
				}else{
					Value argR = instr.getResult();
					
					if (replacements.containsKey(argR)) {
						instr.setArgZ(argR);
					}else if (instr.getArgZ() == null) {
						instr.setArgZ(new WhitespaceValue());
					}
					
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
					}
				}
				
				if (instr.getOperator() == Instruction.Operator.call) {
					instr.replaceLiveValues(replacements);
				}
			}
		}
	}
	
	public ArrayList<VCGNode> getVCGNodes() {
		ArrayList<VCGNode> nodes = new ArrayList<>(graph.size());
		HashMap<Value, VCGNode> seen = new HashMap<>();
		
		String[] colorNames = new String[]{"white", "red", "yellow", "green", 
										  "turquoise", "darkgreen", 
										  "lightblue", "cyan", "magenta",
										  "blue", "lightred", "orange", 
										  "aquamarine", "yellowgreen", "lilac",
										  "pink", "darkmagenta", "darkred",
										  "darkgrey", "darkblue", "lightgrey"};
		
		for (Map.Entry<Value, HashSet<Value>> entry : graph.entrySet()) {
			Value value = entry.getKey();
			HashSet<Value> neighbors = entry.getValue();
			VCGNode node = new VCGNode();
			int colorIndex = colors.get(value) + 1;
			if (colorIndex >= colorNames.length) colorIndex = 0;
			
			node.setName(value.toString());
			node.setColor(colorNames[colorIndex]);
			node.setDirectedChildren(false);
			
			nodes.add(node);
			seen.put(value, node);
			
			for (Value neighbor : neighbors) {
				if (!seen.containsKey(neighbor)) continue;
				
				node.getChildren().add(seen.get(neighbor));
			}
		}
		
		return nodes;
	}
	
}
