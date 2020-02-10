package model.syntax;

import java.util.*;

import vcg.*;

public class IfStatement implements Statement, VCGRepresentable {
	public Relation relation;
	public ArrayList<Statement> thenStatements;
	public ArrayList<Statement> elseStatements;
	
	public IfStatement() {
		relation = null;
		thenStatements = new ArrayList<>();
		elseStatements = new ArrayList<>();
	}
	
	public VCGNode getVCGNode() {
		VCGNode thenStatementsNode = new VCGNode();
		thenStatementsNode.setName("Then Statements");
		for (Statement s : thenStatements) {
			thenStatementsNode.getChildren().add(s.getVCGNode());
		}
		
		VCGNode elseStatementsNode = new VCGNode();
		elseStatementsNode.setName("Else Statements");
		for (Statement s : elseStatements) {
			elseStatementsNode.getChildren().add(s.getVCGNode());
		}
		
		VCGNode ifNode = new VCGNode();
		ifNode.setName("If Statement");
		ifNode.getAttributes().add(new VCGAttribute<>("Relation", relation.getVCGNode()));
		ifNode.getAttributes().add(new VCGAttribute<>("Then", thenStatementsNode));
		ifNode.getAttributes().add(new VCGAttribute<>("Else", elseStatementsNode));
		
		return ifNode;
	}
	
}
