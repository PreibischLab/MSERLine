package MTObjects;

import java.util.ArrayList;

import graphconstructs.KalmanTrackproperties;

public class FramedBlob {

	
	public final int frame;
	public  KalmanTrackproperties Blobs;
	
	
	public FramedBlob( final int frame, KalmanTrackproperties Blobs ){
		
		this.frame = frame;
		this.Blobs = Blobs;
		
	}
	
	
}