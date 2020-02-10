package vcg;

import java.util.*;

public class VCGNode {
	String name;
	int number;
	int x;
	int y;
	ArrayList<VCGAttribute<?>> attributes;
	String color;
	boolean directedChildren;
	ArrayList<VCGNode> children;
	HashMap<VCGNode, String> childConnectionColors;
	
	public VCGNode() {
		this.name = "";
		this.number = 0;
		this.y = 0;
		this.x = 0;
		this.attributes = new ArrayList<>();
		this.color = "white";
		this.directedChildren = true;
		this.children = new ArrayList<>();
		this.childConnectionColors = new HashMap<>();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		if (name == null) name = "";
		this.name = name;
	}
	
	public ArrayList<VCGAttribute<?>> getAttributes() {
		return attributes;
	}
	
	public String getColor() {
		return color;
	}
	
	public void setColor(String color) {
		this.color = color;
	}
	
	public boolean directedChildren() {
		return directedChildren;
	}
	
	public void setDirectedChildren(boolean d) {
		this.directedChildren = d;
	}
	
	public ArrayList<VCGNode> getChildren() {
		return children;
	}
	
	public HashMap<VCGNode, String> getChildConnectionColors() {
		return childConnectionColors;
	}
	
	public ArrayList<VCGNode> getRelatedNodes() {
		ArrayList<VCGNode> relatedNodes = new ArrayList<>();
		
		for (VCGAttribute<?> attribute : attributes) {
			if (attribute.getValue() instanceof VCGNode) {
				relatedNodes.add((VCGNode) attribute.getValue());
			}
		}
		
		relatedNodes.addAll(children);
		
		return relatedNodes;
	}
	
	public ArrayList<VCGNode> getConnectedNodes() {
		ArrayList<VCGNode> connectedNodes = new ArrayList<>();
		
		LinkedList<VCGNode> queue = new LinkedList<>();
		HashSet<VCGNode> seen = new HashSet<>();
		queue.add(this);
		
		while (queue.size() > 0) {
			VCGNode current = queue.removeFirst();
			
			if (seen.contains(current)) continue;
			seen.add(current);
			
			connectedNodes.add(current);
			
			for (VCGNode child : current.getRelatedNodes()) {
				if (seen.contains(child)) continue;
				
				queue.addLast(child);
			}
		}
		
		return connectedNodes;
	}
	
}
