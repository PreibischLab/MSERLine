package graphconstructs;

public class Trackproperties {

	
	public final int Label;
	public final double[] oldpoint;
	public final double[] newpoint;
	public final double newslope;
	public final double newintercept;
	public final double originalslope;
	public final double originalintercept;
	public final int seedlabel;
	public final double[] originalpoint;
	
	
	
	public Trackproperties(final int Label,
			final double[] oldpoint, final double[] newpoint, final double newslope, final double newintercept, final double originalslope, final double originalintercept, final int seedlabel, final double[] originalpoint ) {
		this.Label = Label;
		this.oldpoint = oldpoint;
		this.newpoint = newpoint;
		this.newslope = newslope;
		this.newintercept = newintercept;
		this.originalslope = originalslope;
		this.originalintercept = originalintercept;
		this.seedlabel = seedlabel;
		this.originalpoint = originalpoint;
		

	}
	
}
