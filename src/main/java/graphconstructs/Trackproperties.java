package graphconstructs;

public class Trackproperties {

	
	public final int Label;
	public final double[] oldpoint;
	public final double[] newpoint;
	public final double newslope;
	public final double newintercept;
	public final double[] direction;
	public final int seedlabel;
	public final int framenumber;
	
	
	public Trackproperties(final int Label,
			final double[] oldpoint, final double[] newpoint, final double newslope, final double newintercept,
			final double[] direction, final int seedlabel, final int framenumber ) {
		this.Label = Label;
		this.oldpoint = oldpoint;
		this.newpoint = newpoint;
		this.newslope = newslope;
		this.newintercept = newintercept;
		this.direction = direction;
		this.seedlabel = seedlabel;
		this.framenumber = framenumber;

	}
	
}
