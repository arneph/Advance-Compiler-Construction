package model.syntax;

import java.util.*;

import vcg.*;

public class WhileStatement implements Statement, VCGRepresentable {
	public Relation relation;
	public ArrayList<Statement> statements;
	
	public WhileStatement() {
		relation = null;
		statements = new ArrayList<>();
	}
	
	public VCGNode getVCGNode() {
		VCGNode statementsNode = new VCGNode();
		statementsNode.setName("Statements");
		for (Statement s : statements) {
			statementsNode.getChildren().add(s.getVCGNode());
		}
		
		VCGNode whileNode = new VCGNode();
		whileNode.setName("While Statement");
		whileNode.getAttributes().add(new VCGAttribute<>("Relation", relation.getVCGNode()));
		whileNode.getAttributes().add(new VCGAttribute<>("Statements", statementsNode));
		
		return whileNode;
	}
	
}
