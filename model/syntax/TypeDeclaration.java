package model.syntax;

import java.util.*;

import vcg.*;

public class TypeDeclaration implements VCGRepresentable {
	public ArrayList<Integer> dimensions;
	
	public TypeDeclaration() {
		dimensions = new ArrayList<>();
	}
	
	public boolean isVariable() {
		return dimensions.size() == 0;
	}
	
	public int getSize() {
		int size = 4;
		for (int s : dimensions) {
			size *= s;
		}
		return size;
	}
	
	public int[] simplifiedDimensions() {
		int[] dims = new int[dimensions.size()];
		
		for (int i = 0; i < dimensions.size(); i++) {
			dims[i] = dimensions.get(i);
		}
		
		return dims;
	}
	
	public VCGNode getVCGNode() {
		VCGNode node = new VCGNode();
		node.setName("Type Declaration");
		node.getAttributes().add(new VCGAttribute<>("Dimensions", dimensions.toString()));
		
		return node;
	}
		
}
