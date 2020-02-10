package vcg;

public class VCGEdge {
	VCGNode source;
	VCGNode target;
	int anchor;
	boolean directed;
	String color;
	
	public VCGEdge(VCGNode s, VCGNode t, boolean d, String c) {
		this(s, t, -1, d, c);
	}
	
	public VCGEdge(VCGNode s, VCGNode t, int a, boolean d, String c) {
		source = s;
		target = t;
		anchor = a;
		directed = d;
		color = c;
	}
	
}
