package model.syntax;

import java.util.*;

import vcg.*;

public class FunctionDeclaration implements VCGRepresentable {
	public boolean isProcedure;
	public int nameID;
	public ArrayList<Integer> parameterIDs;
	public ArrayList<VariableDeclaration> variables;
	public ArrayList<Statement> statements;
	
	public FunctionDeclaration() {
		isProcedure = true;
		nameID = -1;
		parameterIDs = new ArrayList<>();
		variables = new ArrayList<>();
		statements = new ArrayList<>();
	}
	
	public int getParameterSpace() {
		return parameterIDs.size() * 4;
	}
	
	public int getReturnSpace() {
		return (isProcedure) ? 0 : 4;
	}
	
	public int getCommunicationSpace() {
		return getReturnSpace() + getParameterSpace();
	}
	
	public int getReservedSpace() {
		int n = 0;
		for (VariableDeclaration var : variables) {
			n += var.nameIDs.size() * var.type.getSize();
		}
		return n * 4;
	}
	
	public VCGNode getVCGNode() {
		VCGNode parametersNode = new VCGNode();
		parametersNode.setName("Parameters");
		for (int id : parameterIDs) {
			VCGNode parameterNode = new VCGNode();
			parameterNode.setName("Parameter");
			parameterNode.getAttributes().add(new VCGAttribute<>("ID", Integer.toString(id)));
			parametersNode.getChildren().add(parameterNode);
		}
		
		VCGNode variablesNode = new VCGNode();
		variablesNode.setName("Variables");
		for (VariableDeclaration v : variables) {
			variablesNode.getChildren().add(v.getVCGNode());
		}
		
		VCGNode statementsNode = new VCGNode();
		statementsNode.setName("Statements");
		for (Statement s : statements) {
			statementsNode.getChildren().add(s.getVCGNode());
		}
		
		VCGNode functionNode = new VCGNode();
		functionNode.setName("Function Declaration");
		functionNode.getAttributes().add(new VCGAttribute<>("Type", (isProcedure) ? "Procedure" : "Function"));
		functionNode.getAttributes().add(new VCGAttribute<>("ID", Integer.toString(nameID)));
		functionNode.getAttributes().add(new VCGAttribute<>("Variables", variablesNode));
		functionNode.getAttributes().add(new VCGAttribute<>("Parameters", parametersNode));
		functionNode.getAttributes().add(new VCGAttribute<>("Statements", statementsNode));
		
		return functionNode;
	}
	
}
