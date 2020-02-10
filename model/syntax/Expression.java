package model.syntax;

import java.util.*;

import vcg.*;

public class Expression implements Factor, VCGRepresentable {
	public enum Operator implements VCGRepresentable {
		Plus,
		Minus;
		
		public VCGNode getVCGNode() {
			VCGNode node = new VCGNode();
			node.setName((this == Operator.Plus) ? "+" : "-");
			
			return node;
		}
	}
	
	public ArrayList<Term> terms;
	public ArrayList<Operator> operators;
	
	public Expression() {
		terms = new ArrayList<>();
		operators = new ArrayList<>();
	}
	
	public VCGNode getVCGNode() {
		VCGNode node = new VCGNode();
		node.setName("Expression");
		for (int i = 0; i < terms.size(); i++) {
			node.getChildren().add(terms.get(i).getVCGNode());
			if (i < operators.size()) {
				node.getChildren().add(operators.get(i).getVCGNode());
			}
		}
		
		return node;
	}
	
}
