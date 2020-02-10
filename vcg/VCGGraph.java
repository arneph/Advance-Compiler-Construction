package vcg;

import java.util.*;

public class VCGGraph {
	private ArrayList<VCGNode> nodes;
	private ArrayList<VCGEdge> edges;
	private boolean preparedNodes;
	
	public VCGGraph(ArrayList<VCGNode> nodes) {
		if (nodes == null) {
			throw new IllegalArgumentException();
		}
		
		this.nodes = nodes;
		this.edges = new ArrayList<>();
		this.preparedNodes = false;
	}
	
	private void prepareNodes() {
		if (preparedNodes) {
			return;
		}
		
		// Breadth-first graph traversal:
		LinkedList<VCGNode> queue = new LinkedList<>();
		HashSet<VCGNode> seen = new HashSet<>();
		
		// Distinguish nodes with the same name:
		HashMap<String, Integer> counters = new HashMap<>();
		
		// Arrange nodes horizontally:
		HashSet<VCGNode> positioned = new HashSet<>();
		HashMap<Integer, Integer> hCounts = new HashMap<>();
		
		for (VCGNode node : nodes) {
			queue.add(node);
			
			while (queue.size() > 0) {
				VCGNode current = queue.removeFirst();
				
				if (seen.contains(current) == false) {
					seen.add(current);
					positioned.add(current);
				}else{
					continue;
				}
				
				current.number = counters.getOrDefault(current.name, 1);
				counters.put(current.name, current.number + 1);
				
				for (int i = 0; i < current.getAttributes().size(); i++) {
					VCGAttribute<?> attribute = current.getAttributes().get(i);
					
					if (attribute.getValue() instanceof VCGNode) {
						VCGNode child = (VCGNode)attribute.getValue();
						String color = current.getChildConnectionColors().get(child);
						VCGEdge edge = new VCGEdge(current, child, i + 2, true, color);
						
						edges.add(edge);
						
						if (positioned.contains(child)) continue;
						
						child.y = current.y + 1;
						child.x = hCounts.getOrDefault(child.y, 0);
						
						positioned.add(child);
						hCounts.put(child.y, child.x + 1);
						
						queue.addLast(child);
					}
				}
				
				for (VCGNode child : current.getChildren()) {
					String color = current.getChildConnectionColors().get(child);
					VCGEdge edge = new VCGEdge(current, child, current.directedChildren, color);
					
					edges.add(edge);
					
					if (positioned.contains(child)) continue;
					
					child.y = current.y + 1;
					child.x = hCounts.getOrDefault(child.y, 0);
					
					positioned.add(child);
					hCounts.put(child.y, child.x + 1);
					
					queue.addLast(child);
				}
			}
		}
		
		preparedNodes = true;
	}
	
	public String toString() {
		prepareNodes();
		
		StringBuilder bob = new StringBuilder();
		
		bob.append("graph: { title: \"Graph\"\n");
		
		for (VCGNode node : nodes) {
			bob.append("node: {\n");
			bob.append("title: \"" + node.name + " " + node.number + "\"\n");
			bob.append("label: \n\"");
			bob.append(node.name);
			
			for (VCGAttribute<?> attribute : node.getAttributes()) {
				bob.append("\n" + attribute.getLabel());
				if (attribute.getValue() instanceof String) {
					bob.append(": " + (String)attribute.getValue());
				}
			}
			
			bob.append("\"\n");
			bob.append("horizontal_order: " + node.x + "\n");
			bob.append("color: " + node.color + "\n");
			bob.append("}\n");
		}
		
		for (VCGEdge edge : edges) {
			bob.append("edge: { sourcename: \"");
			bob.append(edge.source.name + " " + edge.source.number);
			bob.append("\" targetname: \"");
			bob.append(edge.target.name + " " + edge.target.number);
			bob.append("\"");
			if (edge.color != null) {
				bob.append(" color: " + edge.color);
			}
			if (edge.anchor > -1) {
				bob.append(" anchor: ");
				bob.append(edge.anchor);
			}
			bob.append(" arrowstyle: ");
			if (edge.directed) bob.append("solid");
			else bob.append("none");
			bob.append(" }\n");
		}

		bob.append("}");
		
		return bob.toString();
	}
	
}
