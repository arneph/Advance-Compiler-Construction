package model.dlx;

public class DLXInstruction {
	public enum Format {
		F1,
		F2,
		F3,
		Undefined
	}
	public enum Operator {
		ADD(0x00),
		SUB(0x01),
		MUL(0x02),
		DIV(0x03),
		MOD(0x04),
		CMP(0x05),
		
		ORR(0x08),
		AND(0x09),
		BIC(0x0A),
		XOR(0x0B),
		LSH(0x0C),
		ASH(0x0D),
		CHK(0x0E),
		
		ADDI(0x10),
		SUBI(0x11),
		MULI(0x12),
		DIVI(0x13),
		MODI(0x14),
		CMPI(0x15),
		
		ORRI(0x18),
		ANDI(0x19),
		BICI(0x1A),
		XORI(0x1B),
		LSHI(0x1C),
		ASHI(0x1D),
		CHKI(0x1E),
		
		LDW(0x20),
		LDX(0x21),
		POP(0x22),
		STW(0x24),
		STX(0x25),
		PSH(0x26),
		
		BEQ(0x28),
		BNE(0x29),
		BLT(0x2A),
		BGE(0x2B),
		BLE(0x2C),
		BGT(0x2D),
		
		BSR(0x2E),
		JSR(0x30),
		RET(0x31),
		
		RDD(0x32),
		WRD(0x33),
		WRH(0x34),
		WRL(0x35);
		
		private byte opCode;
		
		private Operator(int opCode) {
			this.opCode = (byte)opCode;
		}
		
		public byte getOpCode() {
			return opCode;
		}
		
		public Operator toStandard() {
			if (!(0x10 <= opCode && opCode <= 0x1F)) {
				throw new IllegalStateException("Can not convert to standard version");
			}
			
			return Operator.values()[opCode-0x10+3];
		}
		
		public Operator toImmediate() {
			if (!(0x00 <= opCode && opCode <= 0x0F)) {
				throw new IllegalStateException("Can not convert to immediate version");
			}
			
			return Operator.values()[opCode+0x10-3];
		}
		
		public Format getFormat() {
			if (0x00 <= opCode && opCode <= 0x0F) {
				return Format.F2;
			}else if (0x10 <= opCode && opCode <= 0x1F) {
				return Format.F1;
			}else if (0x28 <= opCode && opCode <= 0x2D) {
				return Format.F1;
			}
			switch(opCode) {
			case 0x20:
				return Format.F1;
			case 0x21:
				return Format.F2;
			case 0x22:
				return Format.F1;
			case 0x24:
				return Format.F1;
			case 0x25:
				return Format.F2;
			case 0x26:
				return Format.F1;
			case 0x2E:
				return Format.F1;
			case 0x30:
				return Format.F3;
			case 0x31:
				return Format.F2;
			case 0x32:
			case 0x33:
			case 0x34:
				return Format.F2;
			case 0x35:
				return Format.F1;
			default:
				return Format.Undefined;
			}
		}
	}
	
	public int number;
	public Operator op;
	public String arg0;
	public String arg1;
	public String arg2;
	
	public DLXInstruction(int number) {
		this.number = number;
	}
	
	public String getArg(int i) {
		if (i == 0) return arg0;
		if (i == 1) return arg1;
		if (i == 2) return arg2;
		return null;
	}
	
	public void setArg(int i, String v) {
		if (i == 0) arg0 = v;
		if (i == 1) arg1 = v;
		if (i == 2) arg2 = v;
	}
	
	public boolean replaceableBy(DLXInstruction instr) {
		if (op != instr.op) return false;
		
		if (arg0 == null && instr.arg0 != null) return false;
		if (arg0 != null && arg0.equals(instr.arg0) == false) return false;
		if (arg1 == null && instr.arg1 != null) return false;
		if (arg1 != null && arg1.equals(instr.arg1) == false) return false;
		if (arg2 == null && instr.arg2 != null) return false;
		if (arg2 != null && arg2.equals(instr.arg2) == false) return false;
		
		return true;
	}

	public boolean isArithmetic() {
		return op.getOpCode() < 32;
	}
	
	public boolean isMemeory() {
		return op.getOpCode() >= 32 && op.getOpCode() < 40;
	}
	
	public boolean isMemorable() {
		return op.getOpCode() < 40;
	}
	
	public boolean isHeapLoad() {
		return Operator.LDW.equals(op) || Operator.LDX.equals(op);
	}
	
	public boolean isStackLoad() {
		return Operator.POP.equals(op);
	}
	
	public boolean isLoad() {
		return isHeapLoad() || isStackLoad();
	}
	
	public boolean isHeapStore() {
		return Operator.STW.equals(op) || Operator.STX.equals(op);
	}
	
	public boolean isStackStore() {
		return Operator.PSH.equals(op);
	}
	
	public boolean isStore() {
		return isHeapStore() || isStackStore();
	}
	
	public String toString() {
		if (op.getFormat() == Format.F1 || op.getFormat() == Format.F2) {
			return String.format("%04d: %-4s %2s, %2s, %2s", number, op.toString(), arg0, arg1, arg2);
		}else if (op.getFormat() == Format.F3) {
			return String.format("%04d: %-4s %2s", number, op.toString(), arg2); 
		}else {
			return "###";
		}
	}
	
}
