package model.syntax;

import java.util.*;

import vcg.*;

public class Computation implements VCGRepresentable {
	public ArrayList<VariableDeclaration> variables;
	public ArrayList<FunctionDeclaration> functions;
	public ArrayList<Statement> statements;
	
	public Computation() {
		variables = new ArrayList<>();
		functions = new ArrayList<>();
		statements = new ArrayList<>();
	}
	
	public VCGNode getVCGNode() {
		VCGNode variablesNode = new VCGNode();
		variablesNode.setName("Variables");
		for (VariableDeclaration v : variables) {
			variablesNode.getChildren().add(v.getVCGNode());
		}
		
		VCGNode functionsNode = new VCGNode();
		functionsNode.setName("Functions");
		for (FunctionDeclaration f : functions) {
			functionsNode.getChildren().add(f.getVCGNode());
		}
		
		VCGNode statementsNode = new VCGNode();
		statementsNode.setName("Statements");
		for (Statement s : statements) {
			statementsNode.getChildren().add(s.getVCGNode());
		}
		
		VCGNode computationNode = new VCGNode();
		computationNode.setName("Computation");
		computationNode.getAttributes().add(new VCGAttribute<>("Variables", variablesNode));
		computationNode.getAttributes().add(new VCGAttribute<>("Functions", functionsNode));
		computationNode.getAttributes().add(new VCGAttribute<>("Statements", statementsNode));
		
		return computationNode;
	}
	
}
