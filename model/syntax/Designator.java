package model.syntax;

import java.util.*;

import vcg.*;

public class Designator implements Factor, VCGRepresentable {
	public int id;
	public ArrayList<Expression> indexExpressions;
	
	public Designator() {
		id = -1;
		indexExpressions = new ArrayList<>();
	}
	
	public VCGNode getVCGNode() {
		VCGNode node = new VCGNode();
		node.setName("Designator");
		node.getAttributes().add(new VCGAttribute<>("ID", Integer.toString(id)));
		for (Expression e : indexExpressions) {
			node.getChildren().add(e.getVCGNode());
		}
		
		return node;
	}
	
}
