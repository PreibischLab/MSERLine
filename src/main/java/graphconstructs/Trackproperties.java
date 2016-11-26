package graphconstructs;

public class Trackproperties {

	
	public final int Label;
	public final double[] oldpoint;
	public final double[] newpoint;
	public final double newslope;
	public final double newintercept;
	
	
	
	public Trackproperties(final int Label,
			final double[] oldpoint, final double[] newpoint, final double newslope, final double newintercept ) {
		this.Label = Label;
		this.oldpoint = oldpoint;
		this.newpoint = newpoint;
		this.newslope = newslope;
		this.newintercept = newintercept;
		

	}
	
}
