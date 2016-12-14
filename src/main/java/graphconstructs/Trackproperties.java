package graphconstructs;

public class Trackproperties {

	
	public final int Label;
	public final int frame;
	public final double[] oldpoint;
	public final double[] newpoint;
	public final double newslope;
	public final double newintercept;
	public final double originalslope;
	public final double originalintercept;
	public final int seedlabel;
	public final double[] originalpoint;
	public final double[] originalds;
	
	
	
	public Trackproperties(final int Label, final int frame,
			final double[] oldpoint, final double[] newpoint, final double newslope, final double newintercept,
			final double originalslope, final double originalintercept, final int seedlabel, final double[] originalpoint, final double[] originalds ) {
		this.Label = Label;
		this.frame = frame;
		this.oldpoint = oldpoint;
		this.newpoint = newpoint;
		this.newslope = newslope;
		this.newintercept = newintercept;
		this.originalslope = originalslope;
		this.originalintercept = originalintercept;
		this.seedlabel = seedlabel;
		this.originalpoint = originalpoint;
		this.originalds = originalds;
		

	}
	
}
