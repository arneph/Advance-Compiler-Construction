package model.ssa;

import java.util.*;

public class Instruction {
	public enum Operator {
		neg,
		add,
		sub,
		mul,
		div,
		cmp,
		
		adda,
		load,
		store,
		move,
		phi,
		
		end,
		bra,
		bne,
		beq,
		ble,
		blt,
		bge,
		bgt,
		
		read,
		write,
		writeNL,
		
		call
	}
	
	private BasicBlock basicBlock;
	private int number;
	private Operator operator;
	private ArrayList<Value> arguments;
	private HashSet<Value> liveValues;
	
	Instruction(BasicBlock block, int num) {
		this(block, num, null);
	}
	
	Instruction(BasicBlock block, int num, CodeGenerator gen) {
		basicBlock = block;
		number = num;
		if (gen != null)
			operator = Operator.call;
		arguments = new ArrayList<>();
		arguments.add(null); //argX
		arguments.add(null); //argY
		arguments.add(null); //argY
		liveValues = null;
	}
	
	public BasicBlock getBasicBlock() {
		return basicBlock;
	}
	
	public int getNumber() {
		return number;
	}
	
	public Operator getOperator() {
		return operator;
	}
	
	public void setOperator(Operator op) {
		operator = op;
	}
	
	public boolean isBranch() {
		if (operator.compareTo(Operator.bra) < 0) return false;
		if (operator.compareTo(Operator.bgt) > 0) return false;
		return true;
	}
	
	public boolean isEliminateable() {
		if (operator == Operator.adda) return false;
		if (operator.compareTo(Operator.end) >= 0) return false;
		if (operator == Operator.store) return false;
		return true;
	}
	
	public List<Value> getArgs() {
		return Collections.unmodifiableList(arguments);
	}
	
	public Value getArg(int i) {
		return arguments.get(i);
	}
	
	public void setArg(int i, Value v) {
		arguments.set(i, v);
	}
	
	public Value getArgX() {
		return arguments.get(0);
	}
	
	public Value setArgX(Value v) {
		return arguments.set(0, v);
	}
	
	public Value getArgY() {
		return arguments.get(1);
	}
	
	public Value setArgY(Value v) {
		return arguments.set(1, v);
	}
	
	public Value getArgZ() {
		return arguments.get(2);
	}
	
	public Value setArgZ(Value v) {
		return arguments.set(2, v);
	}
	
	public ComputedValue getResult() {
		return new ComputedValue(number);
	}
	
	public HashSet<Value> getLiveValues() {
		return liveValues;
	}
	
	public void setLiveValues(HashSet<Value> vals) {
		liveValues = vals;
	}
	
	public ConstantValue getConstantValue() {
		boolean cArgX = getArgX() instanceof ConstantValue;
		boolean cArgY = getArgY() instanceof ConstantValue;
		ConstantValue cX = (cArgX) ? (ConstantValue) getArgX() : null;
		ConstantValue cY = (cArgY) ? (ConstantValue) getArgY() : null;
		if (operator == Operator.neg && cArgX) {
			return new ConstantValue(-cX.getValue());
		}else if (operator == Operator.add && cArgX && cArgY) {
			return new ConstantValue(cX.getValue() + cY.getValue());
		}else if (operator == Operator.sub && cArgX && cArgY) {
			return new ConstantValue(cX.getValue() - cY.getValue());
		}else if (operator == Operator.mul && cArgX && cArgY) {
			return new ConstantValue(cX.getValue() * cY.getValue());
		}else if (operator == Operator.div && cArgX && cArgY) {
			return new ConstantValue(cX.getValue() / cY.getValue());
		}else if (operator == Operator.cmp && cArgX && cArgY) {
			return new ConstantValue(cX.getValue() - cY.getValue());
		}else{
			return null;
		}
	}
	
	public void replaceLiveValues(HashMap<Value, Value> replacements) {
		HashSet<Value> newLiveValues = new HashSet<>();
		
		for (Value oldValue : liveValues) {
			Value newValue = replacements.getOrDefault(oldValue, oldValue);
			
			newLiveValues.add(newValue);
		}
		
		liveValues = newLiveValues;
	}
	
	public String toString() {
		Value argX = arguments.get(0);
		Value argY = arguments.get(1);
		Value argZ = arguments.get(2);
		String argXS = (argX != null) ? argX.toString() : "";
		String argYS = (argY != null) ? argY.toString() : "";
		String argZS = (argZ != null) ? argZ.toString() : "";
		
		if (argZ != null) {
			return String.format("(%03d) %7s %5s %5s %5s", 
								 number,
								 operator.toString(),
								 argZS, argXS, argYS);
		}else if (argX != null || argY != null) {
			return String.format("(%03d) %7s %5s %5s", 
					 number,
					 operator.toString(),
					 argXS, argYS);
		}else{
			return String.format("(%03d) %7s", 
								 number,
								 operator.toString());
		}
	}
	
}
