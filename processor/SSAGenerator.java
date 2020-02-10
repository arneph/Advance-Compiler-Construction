package processor;

import java.util.*;

import model.syntax.*;
import model.ssa.*;

public class SSAGenerator {
	private SymbolTable symbolTable;
	private Computation computation;
	private Program program;
	
	private HashMap<Instruction, Integer> functionCallPatches;
	
	private static class Context {
		int depth;
		BasicBlock currentBlock;
		ArrayList<BasicBlock> allBlocks;
		VariableVersionTable table;
		FunctionDeclaration function;
		HashSet<Integer> parameterIDs;
		
		Context(int d, BasicBlock b, VariableVersionTable t, FunctionDeclaration f, HashSet<Integer> pids) {
			depth = d;
			currentBlock = b;
			allBlocks = new ArrayList<>();
			allBlocks.add(b);
			table = t;
			function = f;
			parameterIDs = pids;
		}
	}
	
	public SSAGenerator(SymbolTable table, Computation comp) {
		symbolTable = table;
		computation = comp;
		program = new Program();
		
		functionCallPatches = new HashMap<>();
	}
	
	public Program generate() {
		{
			BasicBlock startBlock = program.addBasicBlock(0);
			Context mainCTX = new Context(0, startBlock, 
					                      new VariableVersionTable(), 
					                      null, new HashSet<>());
	
			processStatements(mainCTX, computation.statements);
			
			BasicBlock endBlock = mainCTX.currentBlock;
			
			Instruction endInstr = endBlock.addInstruction();
			endInstr.setOperator(Instruction.Operator.end);
		}
		
		for (FunctionDeclaration function : computation.functions) {
			BasicBlock startBlock = program.addBasicBlock(1);
			Context funcCTX = new Context(1, startBlock,
										  new VariableVersionTable(), 
										  function, 
										  new HashSet<>(function.parameterIDs));
			
			processStatements(funcCTX, function.statements);
			
			BasicBlock endBlock = funcCTX.currentBlock;
			
			if (endBlock.endsWithEndInstruction() == false) {
				Instruction endInstr = endBlock.addInstruction();
				endInstr.setOperator(Instruction.Operator.end);				
			}
			
			program.markFunctionStartBlock(startBlock, function.nameID);
			
			// TODO: handle functions better
		}
		
		for (Map.Entry<Instruction, Integer> entry : functionCallPatches.entrySet()) {
			Instruction callInstr = entry.getKey();
			int funcID = entry.getValue();
			
			callInstr.setArgY(program.getFunctionStartBlock(funcID).getAddress());
		}
		
		return program;
	}
	
	private void processStatements(Context ctx, ArrayList<Statement> statements) {
		for (Statement stmt : statements) {
			if (stmt instanceof Assignment) {
				Assignment assignment = (Assignment) stmt;
				
				processAssignment(ctx, assignment);
				
			}else if (stmt instanceof FunctionCall) {
				FunctionCall functionCall = (FunctionCall) stmt;
				
				processFunctionCall(ctx, functionCall);
				
			}else if (stmt instanceof IfStatement) {
				IfStatement ifStatement = (IfStatement) stmt;
				
				processIfStatement(ctx, ifStatement);
				
			}else if (stmt instanceof WhileStatement) {
				WhileStatement whileStatement = (WhileStatement) stmt;
				
				processWhileStatement(ctx, whileStatement);
				
			}else if (stmt instanceof ReturnStatement) {
				ReturnStatement returnStatement = (ReturnStatement) stmt;
				
				processReturnStatement(ctx, returnStatement);
			}
		}
	}
	
	private void processAssignment(Context ctx, Assignment assignment) {
		Designator desig = assignment.designator;
		Expression expr = assignment.expression;
		int id = desig.id;
		String name = symbolTable.IDToName(id);
		
		Value exprValue = processExpression(ctx, expr);
		
		if (canTreatAsLocalVariable(ctx, id)) {
			Instruction instr = ctx.currentBlock.addInstruction();
			instr.setOperator(Instruction.Operator.move);
			instr.setArgX(exprValue);
			instr.setArgY(new VariableValue(name, id, instr.getNumber()));
			
			ctx.table.getVersions().put(id, instr.getNumber());
			
		}else if (symbolTable.isVariable(id)) {
			Instruction instr = ctx.currentBlock.addInstruction();
			instr.setOperator(Instruction.Operator.store);
			instr.setArgX(exprValue);
			instr.setArgY(new MemoryAddressValue(name, id));
			
			ctx.table.getVersions().put(id, instr.getNumber());
			
		}else{
			Value offsetValue = computeOffset(ctx, desig);
			
			Instruction addaInstr = ctx.currentBlock.addInstruction();
			addaInstr.setOperator(Instruction.Operator.adda);
			addaInstr.setArgX(new MemoryAddressValue(name, id));
			addaInstr.setArgY(offsetValue);
			
			Instruction storeInstr = ctx.currentBlock.addInstruction();
			storeInstr.setOperator(Instruction.Operator.store);
			storeInstr.setArgX(exprValue);
			storeInstr.setArgY(addaInstr.getResult());
		}
	}
	
	private Value processFunctionCall(Context ctx, FunctionCall functionCall) {
		ArrayList<Value> parameterValues = processExpressions(ctx, functionCall.parameterExpressions);
		
		String name = symbolTable.IDToName(functionCall.functionID);
		
		if ("OutputNewLine".equals(name)) {
			Instruction instr = ctx.currentBlock.addInstruction();
			instr.setOperator(Instruction.Operator.writeNL);
			
			return null;
			
		}else if ("OutputNum".equals(name)) {
			Value x = parameterValues.get(0);
			
			Instruction instr = ctx.currentBlock.addInstruction();
			instr.setOperator(Instruction.Operator.write);
			instr.setArgX(x);
			
			return null;
			
		}else if ("InputNum".equals(name)) {
			Instruction instr = ctx.currentBlock.addInstruction();
			instr.setOperator(Instruction.Operator.read);
			
			return instr.getResult();
			
		}else{
			Instruction instr = ctx.currentBlock.addPlaceholderInstruction(() -> {
				// TODO: Actually call function
			});
			instr.setArgX(new ArgumentsValue(parameterValues));
			if (symbolTable.isFunction(functionCall.functionID)) {
				instr.setArgZ(instr.getResult());
			}
			
			functionCallPatches.put(instr, functionCall.functionID);
			
			return instr.getResult();
		}
	}
	
	private void processIfStatement(Context ctx, IfStatement ifStatement) {
		// Top Block:
		BasicBlock topBlock = ctx.currentBlock;
		topBlock.setIfTopBlock(true);
		
		Relation relation = ifStatement.relation;
		Value relationValue = processRelation(ctx, relation);
		
		Instruction condBranchInstr = topBlock.addInstruction();
		condBranchInstr.setOperator(relationOperatorToInverseSSA(relation.op));
		condBranchInstr.setArgX(relationValue);
		
		// If Block:
		BasicBlock ifStartBlock = program.addBasicBlock(ctx.depth);
		Context ifCTX = new Context(ctx.depth, ifStartBlock,
								   ctx.table.clone(), 
								   ctx.function,
								   ctx.parameterIDs);
		
		processStatements(ifCTX, ifStatement.thenStatements);
		
		BasicBlock ifEndBlock = ifCTX.currentBlock;
		
		Instruction uncondBranchInstr = ifEndBlock.addInstruction();
		uncondBranchInstr.setOperator(Instruction.Operator.bra);
		
		// Else Block:
		BasicBlock elseStartBlock = program.addBasicBlock(ctx.depth);
		Context elseCTX = new Context(ctx.depth, elseStartBlock, 
									 ctx.table.clone(), 
									 ctx.function,
									 ctx.parameterIDs);
		
		processStatements(elseCTX, ifStatement.elseStatements);
		
		BasicBlock elseEndBlock = elseCTX.currentBlock;
		
		// Follow Block:
		BasicBlock followBlock = program.addBasicBlock(ctx.depth);
		followBlock.setIfFollowBlock(true);
		ctx.allBlocks.addAll(ifCTX.allBlocks);
		ctx.allBlocks.addAll(elseCTX.allBlocks);
		ctx.allBlocks.add(followBlock);
		ctx.currentBlock = followBlock;
		
		// Merge:
		processMerge(ctx, ifCTX.table, elseCTX.table);
		
		// Connections:
		condBranchInstr.setArgY(elseStartBlock.getAddress());
		uncondBranchInstr.setArgY(followBlock.getAddress());
		
		program.addEdge(topBlock, ifStartBlock);
		program.addEdge(topBlock, elseStartBlock);
		program.addEdge(ifEndBlock, followBlock);
		program.addEdge(elseEndBlock, followBlock);
		
		program.addDominatior(topBlock, ifStartBlock);
		program.addDominatior(topBlock, elseStartBlock);
		program.addDominatior(topBlock, followBlock);
	}
	
	private void processWhileStatement(Context ctx, WhileStatement whileStatement) {
		// Start Block:
		BasicBlock startBlock = ctx.currentBlock;
		VariableVersionTable startValueTable = ctx.table.clone(); // Backup for patch
		
		// Top Block:
		BasicBlock topBlock = program.addBasicBlock(ctx.depth);
		topBlock.setWhileTopBlock(true);
		ctx.allBlocks.add(topBlock);
		ctx.currentBlock = topBlock;
		
		Relation relation = whileStatement.relation;
		Value relationValue = processRelation(ctx, relation);
		
		Instruction condBranchInstr = topBlock.addInstruction();
		condBranchInstr.setOperator(relationOperatorToInverseSSA(relation.op));
		condBranchInstr.setArgX(relationValue);
		
		// While Block:
		BasicBlock whileStartBlock = program.addBasicBlock(ctx.depth);
		Context whileCTX = new Context(ctx.depth, whileStartBlock,
									  ctx.table.clone(),
									  ctx.function,
									  ctx.parameterIDs);
		
		processStatements(whileCTX, whileStatement.statements);
		
		BasicBlock whileEndBlock = whileCTX.currentBlock;
		
		Instruction uncondBranchInstr = whileEndBlock.addInstruction();
		uncondBranchInstr.setOperator(Instruction.Operator.bra);
		
		// Merge & Patch:
		processMerge(ctx, ctx.table, whileCTX.table);
		processPatch(whileCTX, startValueTable, ctx.table);
		
		// Follow Block:
		BasicBlock followBlock = program.addBasicBlock(ctx.depth);
		followBlock.setWhileFollowBlock(true);
		ctx.allBlocks.addAll(whileCTX.allBlocks);
		ctx.allBlocks.add(followBlock);
		ctx.currentBlock = followBlock;
		
		// Connections:
		condBranchInstr.setArgY(followBlock.getAddress());
		uncondBranchInstr.setArgY(topBlock.getAddress());
		
		program.addEdge(startBlock, topBlock);
		program.addEdge(topBlock, whileStartBlock);
		program.addEdge(topBlock, followBlock);
		program.addEdge(whileEndBlock, topBlock);
		
		program.addDominatior(startBlock, topBlock);
		program.addDominatior(topBlock, whileStartBlock);
		program.addDominatior(topBlock, followBlock);
	}
	
	private void processReturnStatement(Context ctx, ReturnStatement returnStatement) {
		Expression returnExpr = returnStatement.expression;
		Value returnValue = (returnExpr != null) ? processExpression(ctx, returnExpr) : null;
		
		Instruction endInstr = ctx.currentBlock.addInstruction();
		endInstr.setOperator(Instruction.Operator.end);
		endInstr.setArgX(returnValue);
		endInstr.setArgY(new MemoryAddressValue("_r", ctx.function.nameID));
		
		// TODO: Hello!
		
		return;
	}
	
	private void processMerge(Context ctx, VariableVersionTable left, VariableVersionTable right) {
		HashSet<Integer> conflictIDs = VariableVersionTable.getConflictingIDs(left, right);
		
		int phiCount = 0;
		for (int id : conflictIDs) {
			String name = symbolTable.IDToName(id);
			
			int leftVersion = left.getVersions().getOrDefault(id, -1);
			int rightVersion = right.getVersions().getOrDefault(id, -1);
			
			Instruction phiInstr = ctx.currentBlock.addInstruction(phiCount++);
			phiInstr.setOperator(Instruction.Operator.phi);
			phiInstr.setArgX(new VariableValue(name, id, leftVersion));
			phiInstr.setArgY(new VariableValue(name, id, rightVersion));
			phiInstr.setArgZ(new VariableValue(name, id, phiInstr.getNumber()));
			
			ctx.table.getVersions().put(id, phiInstr.getNumber());
		}
		
		for (int i = phiCount; i < ctx.currentBlock.getInstructions().size(); i++) {
			Instruction instr = ctx.currentBlock.getInstructions().get(i);
			
			for (int j = 0; j < 3; j++) {
				Value argV = instr.getArg(j);
				
				if (argV == null) continue;
				if (argV instanceof VariableValue) {
					VariableValue varV = (VariableValue) argV;
					int id = varV.getID();
					
					if (!conflictIDs.contains(id)) continue;
					
					varV.setVersion(ctx.table.getVersions().get(id));
				}
			}
		}
	}
	
	private void processPatch(Context ctx, VariableVersionTable oldTable, VariableVersionTable newTable) {
		for (BasicBlock block : ctx.allBlocks) {
			for (Instruction instr : block.getInstructions()) {
				for (int i = 0; i < 3; i++) {
					Value argV = instr.getArg(i);
					
					if (argV == null) continue;
					if (argV instanceof VariableValue) {
						VariableValue varV = (VariableValue) argV;
						int id = varV.getID();
						
						if (oldTable.getVersions().containsKey(id) == false) continue;
						if (oldTable.getVersions().get(id) != varV.getVersion()) continue;
						
						varV.setVersion(newTable.getVersions().get(id));
					}
				}
			}
		}
	}
	
	private Value processRelation(Context ctx, Relation relation) {
		Value lhsValue = processExpression(ctx, relation.lhs);
		Value rhsValue = processExpression(ctx, relation.rhs);
		
		Value result = compare(ctx, lhsValue, rhsValue);
		
		return result;
	}
	
	private ArrayList<Value> processExpressions(Context ctx, ArrayList<Expression> expressions) {
		ArrayList<Value> expressionValues = new ArrayList<Value>(expressions.size());
		
		for (int i = 0; i < expressions.size(); i++) {
			expressionValues.add(processExpression(ctx, expressions.get(i)));
		}
		
		return expressionValues;
	}
	
	private Value processExpression(Context ctx, Expression expression) {
		Value result = processTerm(ctx, expression.terms.get(0));
		
		for (int i = 1; i < expression.terms.size(); i++) {
			Value termValue = processTerm(ctx, expression.terms.get(i));
			
			Expression.Operator exprOp = expression.operators.get(i - 1);
			Instruction.Operator instrOp = expressionOperatorToSSA(exprOp);
			
			result = compute(ctx, instrOp, result, termValue);	
		}
		
		return result;
	}
	
	private Value processTerm(Context ctx, Term term) {
		Value result = processFactor(ctx, term.factors.get(0));
		
		for (int i = 1; i < term.factors.size(); i++) {
			Value factorValue = processFactor(ctx, term.factors.get(i));
			
			Term.Operator exprOp = term.operators.get(i - 1);
			Instruction.Operator instrOp = termOperatorToSSA(exprOp);
			
			result = compute(ctx, instrOp, result, factorValue);
		}
		
		return result;
	}
	
	private Value processFactor(Context ctx, Factor factor) {
		if (factor instanceof Designator) {
			Designator desig = (Designator)(factor);
			int id = desig.id;
			String name = symbolTable.IDToName(id);
			
			if (canTreatAsLocalVariable(ctx, id)) {
				int version = ctx.table.getVersions().getOrDefault(id, -1);
				
				return new VariableValue(name, id, version);
				
			}else if (symbolTable.isVariable(id)) {
				Instruction instr = ctx.currentBlock.addInstruction();
				instr.setOperator(Instruction.Operator.load);
				instr.setArgY(new MemoryAddressValue(name, id));
				
				return instr.getResult();
				
			}else{
				Value offsetValue = computeOffset(ctx, desig);
				
				Instruction addaInstr = ctx.currentBlock.addInstruction();
				addaInstr.setOperator(Instruction.Operator.adda);
				addaInstr.setArgX(new MemoryAddressValue(name, id));
				addaInstr.setArgY(offsetValue);
				
				Instruction loadInstr = ctx.currentBlock.addInstruction();
				loadInstr.setOperator(Instruction.Operator.load);
				loadInstr.setArgY(addaInstr.getResult());
				
				return loadInstr.getResult();
			}
			
		}else if (factor instanceof Constant) {
			Constant c = (Constant)(factor);
			
			return new ConstantValue(c.value);
			
		}else if (factor instanceof Expression) {
			Expression expr = (Expression)(factor);
			
			return processExpression(ctx, expr);
			
		}else if (factor instanceof FunctionCall) {
			FunctionCall call = (FunctionCall)(factor);
			
			return processFunctionCall(ctx, call);
		}else{
			throw new IllegalStateException("Unknown Factor");
		}
	}
	
	private Value compare(Context ctx, Value x, Value y) {
		boolean constX = x instanceof ConstantValue;
		boolean constY = y instanceof ConstantValue;
		
		if (constX && constY) {
			int xc = ((ConstantValue) x).getValue();
			int yc = ((ConstantValue) y).getValue();
			int rc;
			if (xc > yc) {
				rc = +1;
			}else if (xc < yc) {
				rc = -1;
			}else{
				rc = 0;
			}
			
			return new ConstantValue(rc);
			
		}else{
			Instruction cmpInstr = ctx.currentBlock.addInstruction();
			cmpInstr.setOperator(Instruction.Operator.cmp);
			cmpInstr.setArgX(x);
			cmpInstr.setArgY(y);
			
			return cmpInstr.getResult();
		}
	}
	private Value computeOffset(Context ctx, Designator designator) {
		Value offsetValue;
		
		if (symbolTable.isVariable(designator.id)) {
			offsetValue = new ConstantValue(0);
			
		}else{
			int[] dimensions = symbolTable.getArrayDimensions(designator.id);
			ArrayList<Value> indexValues = processExpressions(ctx, designator.indexExpressions);
			offsetValue = indexValues.get(indexValues.size() - 1);
			
			for (int i = indexValues.size() - 2; i >= 0; i--) {
				ConstantValue indexFactor = new ConstantValue(factorForDimension(dimensions, i));
				Value term = compute(ctx, Instruction.Operator.mul, indexValues.get(i), indexFactor);
				
				offsetValue = compute(ctx, Instruction.Operator.add, offsetValue, term);
			}
		}
		
		return offsetValue;
	}
	
	private Value compute(Context ctx, Instruction.Operator op, Value x, Value y) {
		boolean constX = x instanceof ConstantValue;
		boolean constY = y instanceof ConstantValue;
		
		if (constX && constY) {
			int xc = ((ConstantValue) x).getValue();
			int yc = ((ConstantValue) y).getValue();
			int rc;
			switch(op) {
			case add:
				rc = xc + yc;
				break;
			case sub:
				rc = xc - yc;
				break;
			case mul:
				rc = xc * yc;
				break;
			case div:
				if (yc == 0) {
					rc = 0; // TODO: Output divide by zero warning
				}else{
					rc = xc / yc;
				}
				break;
			default:
				throw new IllegalStateException("Unknown computation operator");
			}
			return new ConstantValue(rc);
			
		}else if (constX || constY) {
			ConstantValue a = (ConstantValue)((constX) ? x : y);
			Value b = ((constX) ? y : x);
			
			switch(op) {
			case add:
				if (a.getValue() == 0) {
					return b;
				}else{
					break;
				}
			case sub:
				if (a.getValue() == 0 && constY) {
					return b;
					
				}else if (a.getValue() == 0 && constX) {
					Instruction negInstr = ctx.currentBlock.addInstruction();
					negInstr.setOperator(Instruction.Operator.neg);
					negInstr.setArgX(b);
					
					return negInstr.getResult();
				}else{
					break;
				}
			case mul:
				if (a.getValue() == 0) {
					return new ConstantValue(0);
				}else if (a.getValue() == 1) {
					return b;
				}else{
					break;
				}
			case div:
				if (a.getValue() == 1 && constY) {
					return b;
				}else{
					break;
				}
			default:
				throw new IllegalStateException("Unknown computation operator");
			}
		}
		
		Instruction instr = ctx.currentBlock.addInstruction();
		instr.setOperator(op);
		instr.setArgX(x);
		instr.setArgY(y);
		
		return instr.getResult();
	}
	
	// designator helper function:
	
	private boolean canTreatAsLocalVariable(Context ctx, int id) {
		if (symbolTable.isArray(id)) return false;
		if (symbolTable.getDepth(id) < ctx.depth) return false;
		if (ctx.parameterIDs.contains(id)) return false;
		if (ctx.depth == 0 && computation.functions.size() > 0) return false;
		
		return true;
	}
	
	// model.syntax to model.ssa conversion helper functions:
	
	private static Instruction.Operator expressionOperatorToSSA(Expression.Operator op) {
		switch (op) {
		case Plus:
			return Instruction.Operator.add;
		case Minus:
			return Instruction.Operator.sub;
		default:
			throw new IllegalStateException("Unknown expression operator");
		}
	}
	
	private static Instruction.Operator termOperatorToSSA(Term.Operator op) {
		switch (op) {
		case Times:
			return Instruction.Operator.mul;
		case Divide:
			return Instruction.Operator.div;
		default:
			throw new IllegalStateException("Unknown expression operator");
		}
	}
	
	private static Instruction.Operator relationOperatorToInverseSSA(Relation.Operator op) {
		switch (op) {
		case LessThan:
			return Instruction.Operator.bge;
		case LessThanOrEqual:
			return Instruction.Operator.bgt;
		case Equal:
			return Instruction.Operator.bne;
		case NotEqual:
			return Instruction.Operator.beq;
		case GreaterThanOrEqual:
			return Instruction.Operator.blt;
		case GreaterThan:
			return Instruction.Operator.ble;
		default:
			throw new IllegalStateException("Unknown relation operator");
		}
	}
	
	// array dimension helper function:
	
	private static int factorForDimension(int[] dimensions, int i) {
		int factor = 1;
		for (int j = dimensions.length - 1; j > i; j--) {
			factor *= dimensions[j];
		}
		return factor;
	}
	
}
