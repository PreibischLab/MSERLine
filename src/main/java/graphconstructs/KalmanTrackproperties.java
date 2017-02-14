package graphconstructs;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.AbstractEuclideanSpace;
import net.imglib2.RealLocalizable;

public class KalmanTrackproperties extends AbstractEuclideanSpace implements RealLocalizable, Comparable<KalmanTrackproperties> {

	
	/*
	 * FIELDS
	 */

	public static AtomicInteger IDcounter = new AtomicInteger( -1 );

	/** Store the individual features, and their values. */
	private final ConcurrentHashMap< String, Double > features = new ConcurrentHashMap< String, Double >();

	/** A user-supplied name for this spot. */
	private String name;

	/** This spot ID. */
	private final int ID;
	
	/**
	 * @param Framenumber
	 *            the current frame
	 * 
	 * @param Label
	 *            the label of the MT
	 * @param currentpoint
	 *            the co-ordinates of the new point of the end of MT.
	 * @param newslope
	 *            the newslope of the MT line.
	 * @param newintercept
	 *            the newintercept of the MT line.
	 * @param originalslope
	 *            the original slope of the MT line.
	 * @param originalintercept
	 *            the original intercept of the MT line.
	 * @param seedlabel
	 *            the seedlabel of the MT line.  
	 * @param originalpoint
	 *            the original point of the MT line.
	 * @param originalds
	 *            the original magnitude of the ds vector determined.
	 */
	
	public final int Framenumber;
	public final int Label;
	public final double size;
	public final double[] currentpoint;
	public final double newslope;
	public final double newintercept;
	public final double originalslope;
	public final double originalintercept;
	public final int seedlabel;
	public final double[] originalds;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public KalmanTrackproperties(final int Framenumber, final int Label,  final double size,
			 final double[] currentpoint, final double newslope, final double newintercept,
			final double originalslope, final double originalintercept, final int seedlabel, final double[] originalds ) {
		super( 3 );
		this.ID = IDcounter.incrementAndGet();
		putFeature( FRAME, Double.valueOf( Framenumber ) );
		putFeature( LABEL, Double.valueOf( Label ) );
		
		putFeature( CurrentXPOSITION, Double.valueOf( currentpoint[0] ) );
		putFeature( CurrentYPOSITION, Double.valueOf( currentpoint[1] ) );
		putFeature( NEWSLOPE, Double.valueOf( newslope ) );
		putFeature( ORIGINALSLOPE, Double.valueOf( originalslope ) );
		
		this.Label = Label;
		this.Framenumber = Framenumber;
		this.size = size;
		this.currentpoint = currentpoint;
		this.newslope = newslope;
		this.newintercept = newintercept;
		this.originalslope = originalslope;
		this.originalintercept = originalintercept;
		this.seedlabel = seedlabel;
		this.originalds = originalds;
		

	}
	
	

	
	@Override
	public int compareTo(KalmanTrackproperties o) {

		return hashCode() - o.hashCode();
	}

	@Override
	public void localize(float[] position) {
		int n = position.length;
		for (int d = 0; d < n; ++d)
			position[d] = getFloatPosition(d);

	}

	@Override
	public void localize(double[] position) {
		int n = position.length;
		for (int d = 0; d < n; ++d)
			position[d] = getDoublePosition(d);
	}

	@Override
	public float getFloatPosition(int d) {
		return (float) getDoublePosition(d);
	}

	@Override
	public double getDoublePosition(int d) {
		return getDoublePosition(d);
	}

	@Override
	public int numDimensions() {

		return currentpoint.length;
	}

	
	
	/*
	 * STATIC KEYS
	 */

	



	/** The name of the blob X position feature. */
	public static final String CurrentXPOSITION = "CurrentXPOSITION";

	/** The name of the blob Y position feature. */
	public static final String CurrentYPOSITION = "CurrentYPOSITION";
	
	/** The name of the blob X position feature. */
	public static final String ORIGINALSLOPE = "OLDSLOPE";

	/** The name of the blob Y position feature. */
	public static final String NEWSLOPE = "NEWSLOPE";
	
	
	/** The label of the blob position feature. */
	public static final String LABEL = "LABEL";

	/** The name of the frame feature. */
	public static final String FRAME = "FRAME";
	public final Double getFeature( final String feature )
	{
		return features.get( feature );
	}

	/**
	 * Stores the specified feature value for this spot.
	 *
	 * @param feature
	 *            the name of the feature to store, as a {@link String}.
	 * @param value
	 *            the value to store, as a {@link Double}. Using
	 *            <code>null</code> will have unpredicted outcomes.
	 */
	public final void putFeature( final String feature, final Double value )
	{
		features.put( feature, value );
	}

	/**
	 * Returns the difference between the location of two blobs, this operation
	 * returns ( <code>A.diffTo(B) = - B.diffTo(A)</code>)
	 *
	 * @param target
	 *            the Blob to compare to.
	 * @param int
	 *            n n = 0 for X- coordinate, n = 1 for Y- coordinate
	 * @return the difference in co-ordinate specified.
	 */
	public double diffTo(final KalmanTrackproperties target, int n) {

		final double thisMTlocation = currentpoint[n];
		final double targetMTlocation = target.currentpoint[n];
		return thisMTlocation - targetMTlocation;
	}
}
