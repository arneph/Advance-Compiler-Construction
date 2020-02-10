package model.syntax;

import vcg.*;

public class Assignment implements Statement, VCGRepresentable {
	public Designator designator;
	public Expression expression;
	
	public Assignment() {
		designator = null;
		expression = null;
	}
	
	public VCGNode getVCGNode() {
		VCGNode node = new VCGNode();
		node.setName("Assignment");
		node.getAttributes().add(new VCGAttribute<>("Designator", designator.getVCGNode()));
		node.getAttributes().add(new VCGAttribute<>("Expression", expression.getVCGNode()));
		
		return node;
	}
	
}
