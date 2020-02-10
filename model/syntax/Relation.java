package model.syntax;

import vcg.*;

public class Relation implements VCGRepresentable {
	public enum Operator {
		LessThan,
		LessThanOrEqual,
		Equal,
		NotEqual,
		GreaterThanOrEqual,
		GreaterThan;

		public String toString() {
			if (this == LessThan) return "<";
			if (this == LessThanOrEqual) return "<=";
			if (this == Equal) return "==";
			if (this == NotEqual) return "!=";
			if (this == GreaterThanOrEqual) return ">=";
			return ">";
		}
	}
	
	public Operator op;
	public Expression lhs;
	public Expression rhs;
	
	public Relation() {
		op = Operator.Equal;
		lhs = null;
		rhs = null;
	}
	
	public VCGNode getVCGNode() {
		VCGNode node = new VCGNode();
		node.setName("Relation");
		node.getAttributes().add(new VCGAttribute<>("Operator", op.toString()));
		node.getAttributes().add(new VCGAttribute<>("LHS", lhs.getVCGNode()));
		node.getAttributes().add(new VCGAttribute<>("RHS", rhs.getVCGNode()));
		
		return node;
	}
	
}
