package trackerType;



import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import graphconstructs.KalmanTrackproperties;
import graphconstructs.Logger;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.OutputAlgorithm;
import graphconstructs.Trackproperties;
/**
 * 
 * links objects across multiple frames in time-lapse images, Creates a new graph from a list of blobs, the blob properties of the current frame
 * are enumerated in the static properties
 * @author varunkapoor
 *
 */


public interface MTTracker extends OutputAlgorithm< SimpleWeightedGraph< KalmanTrackproperties, DefaultWeightedEdge > >
	{
		/**
		 * Sets the {@link Logger} instance that will receive messages from this
		 * {@link SpotTracker}.
		 *
		 * @param logger
		 *            the logger to echo messages to.
		 */
		public void setLogger( final Logger logger );
		public void reset();
	}
	

