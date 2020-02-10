package model.syntax;

import vcg.*;

public class Constant implements Factor, VCGRepresentable {
	public Integer value;
	
	public Constant() {
		value = 0;
	}
	
	public VCGNode getVCGNode() {
		VCGNode node = new VCGNode();
		node.setName("Constant");
		node.getAttributes().add(new VCGAttribute<>("Value", Integer.toString(value)));
		
		return node;
	}
	
}
