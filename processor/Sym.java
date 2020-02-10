package processor;
public class Sym {
	public static final int errorToken = 0;
	
	public static final int timesToken = 1;
	public static final int divideToken = 2;
	
	public static final int plusToken = 11;
	public static final int minusToken = 12;
	
	public static final int eqlToken = 20;
	public static final int neqToken = 21;
	public static final int lssToken = 22;
	public static final int geqToken = 23;
	public static final int leqToken = 24;
	public static final int gtrToken = 25;
	
	public static final int periodToken = 30;
	public static final int commaToken = 31;
	public static final int openbracketToken = 32;
	public static final int closebracketToken = 34;
	public static final int closeparenToken = 35;
	
	public static final int becomesToken = 40;
	public static final int thenToken = 41;
	public static final int doToken = 42;

	public static final int openparenToken = 50;

	public static final int number = 60;
	public static final int ident = 61;

	public static final int semiToken = 70;

	public static final int endToken = 80;
	public static final int odToken = 81;
	public static final int fiToken = 82;

	public static final int elseToken = 90;
	
	public static final int letToken = 100;
	public static final int callToken = 101;
	public static final int ifToken = 102;
	public static final int whileToken = 103;
	public static final int returnToken = 104;

	public static final int varToken = 110;
	public static final int arrToken = 111;
	public static final int funcToken = 112;
	public static final int procToken = 113;
	
	public static final int beginToken = 150;
	public static final int mainToken = 200;
	public static final int eofToken = 255;

	public static final String KeywordPrefix = "§";
	public static final String ProcedurePrefix = "!";
	public static final String FunctionPrefix = "?";
	public static final String VariablePrefix = "@";
	public static final String ArrayPrefix = "$";
	
}
