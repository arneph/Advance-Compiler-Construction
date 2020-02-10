package model.syntax;

import java.util.*;

import vcg.*;

public class FunctionCall implements Statement, Factor, VCGRepresentable {
	public int functionID;
	public ArrayList<Expression> parameterExpressions;
	
	public FunctionCall() {
		functionID = -1;
		parameterExpressions = new ArrayList<>();
	}
	
	public VCGNode getVCGNode() {
		VCGNode node = new VCGNode();
		node.setName("Function Call");
		node.getAttributes().add(new VCGAttribute<>("Function ID", Integer.toString(functionID)));
		for (Expression e : parameterExpressions) {
			node.getChildren().add(e.getVCGNode());
		}
		
		return node;
	}
	
}
