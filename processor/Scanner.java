package processor;

public class Scanner {
	public Reader reader;
	public SymbolTable table;
	
	public int sym;
	public int val;
	public String name;
	
	public Scanner(String fileName) {
		reader = new Reader(fileName);
		table = new SymbolTable();
		
		Next();
	}
	
	public void Next() {
		while (IsWhitespace(reader.sym)) {
			reader.Next();
		}
		
		switch (reader.sym) {
			case 0x00:
			case 0xff:
				sym = reader.sym;
				return;
			case '#':
				while (true) {
					if (reader.sym == 0xff) {
						sym = Sym.eofToken;
						return;
					}else if (reader.sym == '\n') {
						reader.Next();
						Next();
						return;
					}
					reader.Next();
				}
			case '+':
				sym = Sym.plusToken;
				break;
			case '-':
				sym = Sym.minusToken;
				break;
			case '*':
				sym = Sym.timesToken;
				break;
			case '/':
				reader.Next();
				if (reader.sym == '/') {
					while (true) {
						if (reader.sym == 0xff) {
							sym = Sym.eofToken;
							return;
						}else if (reader.sym == '\n') {
							reader.Next();
							Next();
							return;
						}
						reader.Next();
					}
				}else if (reader.sym == '*') {
					while (true) {
						if (reader.sym == 0xff) {
							reader.Error("Multiline comment not closed");
						}else if (reader.sym == '*') {
							reader.Next();
							if (reader.sym == '/') {
								reader.Next();
								Next();
								return;
							}
						}
						reader.Next();
					}
				}else{
					sym = Sym.divideToken;
					return;
				}
			case '=':
				reader.Next();
				if (reader.sym != '=') {
					sym = Sym.errorToken;
					return;
				}
				sym = Sym.eqlToken;
				break;
			case '!':
				reader.Next();
				if (reader.sym != '=') {
					sym = Sym.errorToken;
					return;
				}
				sym = Sym.neqToken;
				break;
			case '<':
				reader.Next();
				if (reader.sym == '=') {
					sym = Sym.leqToken;
				}else if (reader.sym == '-') {
					sym = Sym.becomesToken;
				}else{
					sym = Sym.lssToken;
					return;
				}
				break;
			case '>':
				reader.Next();
				if (reader.sym == '=') {
					sym = Sym.geqToken;
				}else {
					sym = Sym.gtrToken;
					return;
				}
				break;
			case '.':
				sym = Sym.periodToken;
				break;
			case ',':
				sym = Sym.commaToken;
				break;
			case '[':
				sym = Sym.openbracketToken;
				break;
			case ']':
				sym = Sym.closebracketToken;
				break;
			case '(':
				sym = Sym.openparenToken;
				break;
			case ')':
				sym = Sym.closeparenToken;
				break;
			case ';':
				sym = Sym.semiToken;
				break;
			case '{':
				sym = Sym.beginToken;
				break;
			case '}':
				sym = Sym.endToken;
				break;
			default:
				if (IsDigit(reader.sym)) {
					sym = Sym.number;
					val = (reader.sym - '0');
					reader.Next();
					while (IsDigit(reader.sym)) {
						val *= 10;
						val += (reader.sym - '0');
						reader.Next();
					}
					return;
					
				}else if (IsLetter(reader.sym)) {
					StringBuilder builder = new StringBuilder();
					builder.append(reader.sym);
					reader.Next();
					while (IsLetter(reader.sym) || IsDigit(reader.sym)) {
						builder.append(reader.sym);
						reader.Next();
					}
					String s = builder.toString();
					if (table.isKeyword(s)) {
						sym = table.keywordToID(s);
					}else{
						sym = Sym.ident;
						name = s;
					}
				}else{
					reader.Error("Unexpected symbol");
				}
				return;
		}
		reader.Next();
	}
	
	private static boolean IsWhitespace(char c) {
		return c == ' ' || c == '\t' || c == '\r' || c == '\n';
	}
	
	private static boolean IsDigit(char c) {
		return c >= '0' && c <= '9';
	}
	
	private static boolean IsLetter(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
	}
	
	public void Error(String errorMsg) {
		reader.Error(errorMsg);
	}
	
}
