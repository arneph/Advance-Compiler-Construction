package vcg;

public class VCGAttribute<T> {
	private String label;
	private T value;
	
	public VCGAttribute(String label, T value) {
		this.label = label;
		this.value = value;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public T getValue() {
		return value;
	}
	
	public void setValue(T value) {
		this.value = value;
	}
	
}
