package costMatrix;

import graphconstructs.Trackproperties;

/**
 * Implementation of various cost functions
 * 
 * 
 */

// Cost function base don minimizing the squared distances

public class SquareDistCostFunction implements CostFunction< Trackproperties, Trackproperties >
{

	@Override
	public double linkingCost( final Trackproperties source, final Trackproperties target )
	{
		
		return 0;
		//return source.squareDistanceTo(target );
	}
	
	
	
	

}
