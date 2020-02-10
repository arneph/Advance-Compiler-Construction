package processor;

import java.util.*;

import model.syntax.*;

public class Parser {
	private Scanner scanner;
	private SymbolTable table;
	
	public Parser(String filename) {
		scanner = new Scanner(filename);
		table = scanner.table;
	}
	
	public SymbolTable getTable() {
		return table;
	}
	
	public Computation parse() {
		Computation comp = new Computation();
		
		// Start of Computation:
		if (scanner.sym != Sym.mainToken) {
			scanner.Error("Expected 'main'");
			return null;
		}
		scanner.Next();
		
		// Variables:
		while (scanner.sym == Sym.varToken || scanner.sym == Sym.arrToken) {
			VariableDeclaration varDecl = parseVariableDeclaration();
			
			comp.variables.add(varDecl);
		}
		
		// Functions:
		while (scanner.sym == Sym.funcToken || scanner.sym == Sym.procToken) {
			FunctionDeclaration funcDecl = parseFunctionDeclaration();
			
			comp.functions.add(funcDecl);
		}
		
		// Statement Sequence:
		if (scanner.sym != Sym.beginToken) {
			scanner.Error("Expected '{'");
			return null;
		}
		scanner.Next();
		
		if (scanner.sym == Sym.letToken ||
			scanner.sym == Sym.callToken || 
			scanner.sym == Sym.ifToken || 
			scanner.sym == Sym.whileToken || 
			scanner.sym == Sym.returnToken) {
			comp.statements = parseStatementSequence();
		}
		
		if (scanner.sym != Sym.endToken) {
			scanner.Error("Expected '}'");
		}
		scanner.Next();
		
		// End of Computation:
		if (scanner.sym != Sym.periodToken) {
			scanner.Error("Expected '.'");
			return null;
		}
		scanner.Next();
		
		// End of File:
		if (scanner.sym != Sym.eofToken) {
			scanner.Error("Expected end of file");
			return null;
		}
		
		return comp;
	}
	
	private VariableDeclaration parseVariableDeclaration() {
		VariableDeclaration var = new VariableDeclaration();
		
		var.type = parseTypeDeclaration();
		
		if (scanner.sym != Sym.ident) {
			scanner.Error("Expected identifier");
			return null;
		}

		if (table.getLocalFrame().hasNounName(scanner.name)) {
			scanner.Error("Redeclaration of identifier");
			return null;
		}
		
		if (var.type.dimensions.size() == 0) {
			int id = table.addVariable(scanner.name);
			
			var.nameIDs.add(id);
		}else{
			int id = table.addArray(scanner.name, var.type.simplifiedDimensions());
			
			var.nameIDs.add(id);
		}
		
		scanner.Next();
		
		while (scanner.sym == Sym.commaToken) {
			scanner.Next();
			
			if (scanner.sym != Sym.ident) {
				scanner.Error("Expected identifier");
				return null;
			}
			
			if (table.getLocalFrame().hasNounName(scanner.name)) {
				scanner.Error("Redeclaration of identifier");
				return null;
			}
			
			if (var.type.dimensions.size() == 0) {
				int id = table.addVariable(scanner.name);
				
				var.nameIDs.add(id);
			}else{
				int id = table.addArray(scanner.name, var.type.simplifiedDimensions());
				
				var.nameIDs.add(id);
			}
			
			scanner.Next();
		}
		
		if (scanner.sym != Sym.semiToken) {
			scanner.Error("Expected ';'");
			return null;
		}
		scanner.Next();
		
		return var;
	}
	
	private TypeDeclaration parseTypeDeclaration() {
		TypeDeclaration type = new TypeDeclaration();
		
		if (scanner.sym == Sym.varToken) {
			scanner.Next();
			return type;
			
		}else if (scanner.sym == Sym.arrToken) {
			scanner.Next();
			
			while (scanner.sym == Sym.openbracketToken) {
				scanner.Next();
				
				if (scanner.sym != Sym.number) {
					scanner.Error("Expected number");
					return null;
				}
				
				type.dimensions.add(scanner.val);
				
				scanner.Next();
				
				if (scanner.sym != Sym.closebracketToken) {
					scanner.Error("Expected ']'");
					return null;
				}
				scanner.Next();
			}
			
			return type;
			
		}else{
			scanner.Error("Expected 'var' or 'array'");
			return null;
		}
	}
	
	private FunctionDeclaration parseFunctionDeclaration() {
		FunctionDeclaration func = new FunctionDeclaration();
		
		// Function/Procedure:
		if (scanner.sym == Sym.funcToken) {
			func.isProcedure = false;
			
		}else if (scanner.sym == Sym.procToken) {
			func.isProcedure = true;
		
		}else{
			scanner.Error("Expected 'function' or 'procedure'");
			return null;
		}
		scanner.Next();
		
		// Name:
		if (scanner.sym != Sym.ident) {
			scanner.Error("Expected identifier");
			return null;
		}
		
		String name = scanner.name;
		if (table.getLocalFrame().hasVerbName(name)) {
			scanner.Error("Redeclaration of identifier");
			return null;
		}
		
		int id;
		if (func.isProcedure) {
			id = table.addProcedure(name, func.parameterIDs.size());			
		}else{
			id = table.addFunction(name, func.parameterIDs.size());
		}
		func.nameID = id;
		
		scanner.Next();
		table.pushFrame();
		
		// Parameters:
		if (scanner.sym == Sym.openparenToken) {
			func.parameterIDs = parseFormalParameters();
		}
		
		// Semicolon:
		if (scanner.sym != Sym.semiToken) {
			scanner.Error("Expected ';'");
			return null;
		}
		scanner.Next();
		
		// Variables:
		while (scanner.sym == Sym.varToken || scanner.sym == Sym.arrToken) {
			VariableDeclaration varDecl = parseVariableDeclaration();
			
			func.variables.add(varDecl);
		}
		
		// Statement Sequence:
		if (scanner.sym != Sym.beginToken) {
			scanner.Error("Expected '{'");
			return null;
		}
		scanner.Next();
		
		if (scanner.sym == Sym.letToken ||
			scanner.sym == Sym.callToken || 
			scanner.sym == Sym.ifToken || 
			scanner.sym == Sym.whileToken || 
			scanner.sym == Sym.returnToken) {
			func.statements = parseStatementSequence();
		}
		
		if (scanner.sym != Sym.endToken) {
			scanner.Error("Expected '}'");
			return null;
		}
		scanner.Next();
		
		if (scanner.sym != Sym.semiToken) {
			scanner.Error("Expected ';'");
			return null;
		}
		scanner.Next();
		
		table.popFrame();
		
		return func;
	}
	
	private ArrayList<Integer> parseFormalParameters() {
		ArrayList<Integer> params = new ArrayList<>();
		
		if (scanner.sym != Sym.openparenToken) {
			scanner.Error("Expected '('");
			return null;
		}
		scanner.Next();
		
		if (scanner.sym == Sym.ident) {
			params.add(table.addVariable(scanner.name));
			
			scanner.Next();
			
			while (scanner.sym == Sym.commaToken) {
				scanner.Next();
				
				if (scanner.sym != Sym.ident) {
					scanner.Error("Expected identifier");
					return null;
				}
				
				if (table.getLocalFrame().hasNounName(scanner.name)) {
					scanner.Error("Redeclaration of identifier");
					return null;
				}
				
				params.add(table.addVariable(scanner.name));
				
				scanner.Next();
			}
		}
		
		if (scanner.sym != Sym.closeparenToken) {
			scanner.Error("Expected ')'");
			return null;
		}
		scanner.Next();
		
		return params;
	}
	
	private ArrayList<Statement> parseStatementSequence() {
		ArrayList<Statement> stmts = new ArrayList<>();
		
		Statement stmt = parseStatement();
		
		stmts.add(stmt);
		
		while (scanner.sym == Sym.semiToken) {
			scanner.Next();
			
			stmt = parseStatement();
			
			stmts.add(stmt);
		}
		
		return stmts;
	}
	
	private Statement parseStatement() {
		switch (scanner.sym) {
		case Sym.letToken:
			return parseAssignment();
		case Sym.callToken:
			return parseFunctionCall();
		case Sym.ifToken:
			return parseIfStatement();
		case Sym.whileToken:
			return parseWhileStatement();
		case Sym.returnToken:
			return parseReturnStatement();
		default:
			scanner.Error("Expected 'let', 'call' 'if' 'while' or 'return'");
			return null;
		}
	}
	
	private Assignment parseAssignment() {
		Assignment assign = new Assignment();
		
		// Let:
		if (scanner.sym != Sym.letToken) {
			scanner.Error("Expected 'let'");
			return null;
		}
		scanner.Next();
		
		// Designator:
		assign.designator = parseDesignator();
		
		// Arrow:
		if (scanner.sym != Sym.becomesToken) {
			scanner.Error("Expected '<-'");
			return null;
		}
		scanner.Next();
		
		// Expression:
		Expression ex = parseExpression();
		
		assign.expression = ex;
		
		return assign;
	}
	
	public FunctionCall parseFunctionCall() {
		FunctionCall func = new FunctionCall();
		
		// Call:
		if (scanner.sym != Sym.callToken) {
			scanner.Error("Expected 'call'");
			return null;
		}
		scanner.Next();
		
		// Name:
		if (scanner.sym != Sym.ident) {
			scanner.Error("Expected identifier");
			return null;
		}
		
		if (!table.hasVerbName(scanner.name)) {
			scanner.Error("Unknown function");
			return null;
		}
		func.functionID = table.verbNameToID(scanner.name);
		if (!(table.isProcedure(func.functionID) || table.isFunction(func.functionID))) {
			scanner.Error("Identifier is neither a procedure nor function");
			return null;
		}
		
		scanner.Next();
		
		// Parameters:
		if (scanner.sym != Sym.openparenToken) {
			return func;
		}
		scanner.Next();
		
		if (scanner.sym == Sym.closeparenToken) {
			scanner.Next();
			return func;
		}
		
		Expression ex = parseExpression();
		
		func.parameterExpressions.add(ex);
		
		while (scanner.sym == Sym.commaToken) {
			scanner.Next();
			
			ex = parseExpression();
			
			func.parameterExpressions.add(ex);
		}
		
		if (scanner.sym != Sym.closeparenToken) {
			scanner.Error("Expected ')'");
			return null;
		}
		scanner.Next();
		
		return func;
	}
	
	public IfStatement parseIfStatement() {
		IfStatement ifs = new IfStatement();
		
		// If:
		if (scanner.sym != Sym.ifToken) {
			scanner.Error("Expected 'if'");
			return null;
		}
		scanner.Next();
		
		// Relation:
		ifs.relation = parseRelation();
		
		// Then:
		if (scanner.sym != Sym.thenToken) {
			scanner.Error("Expected 'then'");
			return null;
		}
		scanner.Next();
		
		ifs.thenStatements = parseStatementSequence();
		
		if (scanner.sym == Sym.fiToken) {
			scanner.Next();
			
			return ifs;
		}
		
		// Else:
		if (scanner.sym != Sym.elseToken) {
			scanner.Error("Expected 'else' or 'fi'");
			return null;
		}
		scanner.Next();
		
		ifs.elseStatements = parseStatementSequence();
		
		if (scanner.sym != Sym.fiToken) {
			scanner.Error("Expected 'fi'");
			return null;
		}
		scanner.Next();
		
		return ifs;
	}
	
	public WhileStatement parseWhileStatement() {
		WhileStatement whiles = new WhileStatement();
		
		// While:
		if (scanner.sym != Sym.whileToken) {
			scanner.Error("Expected 'while'");
			return null;
		}
		scanner.Next();
		
		// Relation:
		whiles.relation = parseRelation();
		
		// Do:
		if (scanner.sym != Sym.doToken) {
			scanner.Error("Expected 'do'");
			return null;
		}
		scanner.Next();
		
		whiles.statements = parseStatementSequence();
		
		if (scanner.sym != Sym.odToken) {
			scanner.Error("Expected 'od'");
			return null;
		}
		scanner.Next();
		
		return whiles;
	}
	
	public ReturnStatement parseReturnStatement() {
		ReturnStatement returns = new ReturnStatement();
		
		// Return:
		if (scanner.sym != Sym.returnToken) {
			scanner.Error("Expected 'return'");
			return null;
		}
		scanner.Next();
		
		// Expression:
		if (scanner.sym == Sym.ident || 
			scanner.sym == Sym.number || 
			scanner.sym == Sym.openparenToken || 
			scanner.sym == Sym.callToken) {
			returns.expression = parseExpression();			
		}
		
		return returns;
	}
	
	public Relation parseRelation() {
		Relation rel = new Relation();
		
		// LHS:
		rel.lhs = parseExpression();
		
		// Operator:
		switch (scanner.sym) {
		case Sym.lssToken:
			rel.op = Relation.Operator.LessThan;
			break;
		case Sym.leqToken:
			rel.op = Relation.Operator.LessThanOrEqual;
			break;
		case Sym.eqlToken:
			rel.op = Relation.Operator.Equal;
			break;
		case Sym.neqToken:
			rel.op = Relation.Operator.NotEqual;
			break;
		case Sym.geqToken:
			rel.op = Relation.Operator.GreaterThanOrEqual;
			break;
		case Sym.gtrToken:
			rel.op = Relation.Operator.GreaterThan;
			break;
		default:
			scanner.Error("Excpected '<', '<=', '==', '!=', '>=' or '>'");
			return null;
		}
		scanner.Next();
		
		rel.rhs = parseExpression();
		
		return rel;
	}
	
	public Expression parseExpression() {
		Expression ex = new Expression();
		
		Term t = parseTerm();
		
		ex.terms.add(t);
		
		while (scanner.sym == Sym.plusToken ||
			   scanner.sym == Sym.minusToken) {
			if (scanner.sym == Sym.plusToken) {
				ex.operators.add(Expression.Operator.Plus);
			}else {
				ex.operators.add(Expression.Operator.Minus);
			}
			scanner.Next();
			
			t = parseTerm();
			
			ex.terms.add(t);
		}
		
		return ex;
	}
	
	public Term parseTerm() {
		Term t = new Term();
		
		Factor f = parseFactor();
		
		t.factors.add(f);
		
		while (scanner.sym == Sym.timesToken || 
			   scanner.sym == Sym.divideToken) {
			if (scanner.sym == Sym.timesToken) {
				t.operators.add(Term.Operator.Times);
			}else{
				t.operators.add(Term.Operator.Divide);
			}
			scanner.Next();
			
			f = parseFactor();
			
			t.factors.add(f);
		}
		
		return t;
	}
	
	public Factor parseFactor() {
		switch (scanner.sym) {
		case Sym.ident:
			return parseDesignator();
		case Sym.number:
			Constant c = new Constant();
			
			c.value = scanner.val;
			
			scanner.Next();
			
			return c;
		case Sym.openparenToken:
			scanner.Next();
			
			Expression ex = parseExpression();
			
			if (scanner.sym != Sym.closeparenToken) {
				scanner.Error("Expected ')'");
				return null;
			}
			scanner.Next();
			
			return ex;
		case Sym.callToken:
			return parseFunctionCall();
		default:
			scanner.Error("Expected designator, number, expression or function call");
			return null;
		}
	}
	
	public Designator parseDesignator() {
		Designator d = new Designator();
		
		if (scanner.sym != Sym.ident) {
			scanner.Error("Expected identifier");
			return null;
		}
		
		if (!table.hasNounName(scanner.name)) {
			scanner.Error("Unknown identifier");
			return null;
		}
		d.id = table.nounNameToID(scanner.name);
		
		scanner.Next();
		
		while (scanner.sym == Sym.openbracketToken) {
			scanner.Next();
			
			Expression ex = parseExpression();
			
			d.indexExpressions.add(ex);
			
			if (scanner.sym != Sym.closebracketToken) {
				scanner.Error("Expected ']'");
				return null;
			}
			scanner.Next();
		}
		
		if (d.indexExpressions.size() == 0) {
			if (!table.isVariable(d.id)) {
				scanner.Error("Expected variable identifier");
				return null;
			}
		}else {
			if (!table.isArray(d.id)) {
				scanner.Error("Expected array identifier");
				return null;
			}
		}
		
		return d;
	}
	
}
