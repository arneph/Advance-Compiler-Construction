package model.syntax;

import vcg.*;

public class ReturnStatement implements Statement, VCGRepresentable {
	public Expression expression;
	
	public ReturnStatement() {
		expression = null;
	}
	
	public VCGNode getVCGNode() {
		VCGNode node = new VCGNode();
		node.setName("Return Statement");
		node.getChildren().add(expression.getVCGNode());
		
		return node;
	}
	
}
