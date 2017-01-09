package mpicbg.pointdescriptor;

import net.imglib2.util.Util;
import mpicbg.models.Point;

public class LinkedPoint<P> extends Point
{
	private static final long serialVersionUID = 1L;

	final P link;
	
	public LinkedPoint( final double[] l, final P link )
	{
		super( l.clone() );		
		this.link = link;
	}

	public LinkedPoint( final double[] l, final double[] w, final P link )
	{
		super( l.clone(), w.clone() );
		this.link = link;
	}

	public P getLinkedObject() { return link; }
	
	public String toString() { return "LinkedPoint " + Util.printCoordinates( l ); }
}
