package interpolation;

import java.util.ArrayList;
import java.util.Collection;

import Jama.Matrix;
import Jama.QRDecomposition;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;

public class Polynomial extends AbstractFunction<Line> {
 
	/**
	 * 
	 */
	private static final long serialVersionUID = 5010369758205651325L;
	final int minNumPoints = 2;
	int degree;
    public Matrix Coefficients;
	double n, m;
	 private double SSE;
	 private double SST;
		

		/**
		 * @return - the center of the circle in x
		 */
		public double getN() { return n; }

		/**
		 * @return - the center of the circle in y
		 */
		public double getM() { return m; }
	 
	

	/**
	 * @return - the coefficients of the polynomial in x
	 */
	public double GetCoefficients(int j) {
        return Coefficients.get(j, 0);
    }
	
	@Override
	public int getMinNumPoints() { return minNumPoints; }

	
	/*
	 * 
	 * This is a fit function for the polynomial of user chosen degree
	 * 
	 */
	public void fitFunction( final Collection<Point> points, int degree ) throws NotEnoughDataPointsException
	{
		
		this.degree = degree;
		final int Npoints = points.size();
		if ( Npoints < minNumPoints )
			throw new NotEnoughDataPointsException( "Not enough points, at least " + minNumPoints + " are necessary." );
        double[] y = new double[Npoints];
        double[] x = new double[Npoints];
        
        
        int count = 0;
        for ( final Point p : points ){
        	x[count] = p.getW()[ 0 ];
        	y[count] = p.getL()[ 1 ];
        	count++;
        }
        
        
        
		// Vandermonde matrix 
				double[][] vandermonde = new double[Npoints][degree+1];
				for (int i = 0; i < Npoints; i++) {
		            for (int j = 0; j <= degree; j++) {
		                vandermonde[i][j] = Math.pow(x[i], j);
		            }
		        }
		        Matrix X = new Matrix(vandermonde);
		       
		        
		        // create matrix from vector
		        Matrix Y = new Matrix(y, Npoints);
		        
		     // find least squares solution
		        QRDecomposition qr = new QRDecomposition(X);
		        Coefficients = qr.solve(Y);
		
		        
		        
		        // mean of y[] values
		        double sum = 0.0;
		        for (int i = 0; i < Npoints; i++)
		            sum += y[i];
		        double mean = sum / Npoints;
		        
		        
		        // total variation to be accounted for
		        for (int i = 0; i < Npoints; i++) {
		            double dev = y[i] - mean;
		            SST += dev*dev;
		        }

		        // variation not accounted for
		        Matrix residuals = X.times(Coefficients).minus(Y);
		        SSE = residuals.norm2() * residuals.norm2();
		        
	
	}

	
	
	public void fitFunction( final Collection<Point> points ) throws NotEnoughDataPointsException
	{
		
		
		
		final int numPoints = points.size();
		
		if ( numPoints < minNumPoints )
			throw new NotEnoughDataPointsException( "Not enough points, at least " + minNumPoints + " are necessary." );
		
		// compute matrices
		final double[] delta = new double[ 4 ];
		final double[] tetha = new double[ 2 ];
		
		for ( final Point p : points )
		{
			final double x = p.getW()[ 0 ]; 
			final double y = p.getW()[ 1 ]; 
			
			final double xx = x*x;
			final double xy = x*y;
			
			delta[ 0 ] += xx;
			delta[ 1 ] += x;
			delta[ 2 ] += x;
			delta[ 3 ] += 1;
			
			tetha[ 0 ] += xy;
			tetha[ 1 ] += y;
		}
				
		// invert matrix
		MatrixFunctions.invert2x2( delta );
		
		this.m = delta[ 0 ] * tetha[ 0 ] + delta[ 1 ] * tetha[ 1 ];
		this.n = delta[ 2 ] * tetha[ 0 ] + delta[ 3 ] * tetha[ 1 ];
	}

	@Override
	public double distanceTo( final Point point )
	{
		final double x1 = point.getW()[ 0 ]; 
		final double y1 = point.getW()[ 1 ];
		
		
		return Math.abs( y1 - m*x1 - n ) / ( Math.sqrt( m*m + 1 ) );
	}
	
	public static int i = 0;
	
	@Override
	public void set( final Line m )
	{
		this.n = m.getN();
		this.m = m.getM();
		this.setCost( m.getCost() );
	}

	@Override
	public Line copy()
	{
		Line c = new Line();
		
		c.n = getN();
		c.m = getM();
		c.setCost( getCost() );
		
		return c;
	}
	 public int degree() {
	        return degree;
	    }

	    public double R2() {
	        return 1.0 - SSE/SST;
	    }

	    // Horner's method to get y values correspoing to x
	    public double predict(double x) {
	        // horner's method
	        double y = 0.0;
	        for (int j = degree; j >= 0; j--)
	            y = GetCoefficients(j) + (x * y);
	        return y;
	    }
	
	
	
	public static void main( String[] args ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		final ArrayList< Point > points = new ArrayList<Point>();

		points.add( new Point( new double[]{ 1f, -3.95132f } ) );
		points.add( new Point( new double[]{ 2f, 6.51205f } ) );
		points.add( new Point( new double[]{ 3f, 18.03612f } ) );
		points.add( new Point( new double[]{ 4f, 28.65245f } ) );
		points.add( new Point( new double[]{ 5f, 42.05581f } ) );
		points.add( new Point( new double[]{ 6f, 54.01327f } ) );
		points.add( new Point( new double[]{ 7f, 64.58747f } ) );
		points.add( new Point( new double[]{ 8f, 76.48754f } ) );
		points.add( new Point( new double[]{ 9f, 89.00033f } ) );
		
		final ArrayList< PointFunctionMatch > candidates = new ArrayList<PointFunctionMatch>();
		final ArrayList< PointFunctionMatch > inliersPoly = new ArrayList<PointFunctionMatch>();
		final ArrayList< PointFunctionMatch > inliersLine = new ArrayList<PointFunctionMatch>();
		
		for ( final Point p : points )
			candidates.add( new PointFunctionMatch( p ) );
		
		
		// Using the polynomial model to do the fitting
		final Polynomial regression = new Polynomial();
		regression.ransac( candidates, inliersPoly, 100, 0.1, 0.5 );
		
		
		
		System.out.println( inliersPoly.size() );
		
		regression.fit( inliersPoly, 3 );
        System.out.println(" y = "+ regression.GetCoefficients(3) + " x*x*x + " +  "  "    + regression.GetCoefficients(2) + " x*x " + " " +  regression.GetCoefficients(1) 
        + " x " + " +  " +  + regression.GetCoefficients(0)   );
        //+ regression.GetCoefficients(2) + " x*x " + " " + regression.GetCoefficients(3) + " x*x*x");

		for ( final PointFunctionMatch p : inliersPoly )
			System.out.println( regression.distanceTo( p.getP1() ) );
		
		
		
		// Using the line model to do the fitting
		final Line l = new Line();
		l.ransac( candidates, inliersLine, 100, 0.1, 0.5 );
       System.out.println( inliersLine.size() );
		
		l.fit( inliersLine );
		
		
		System.out.println( "y = " + l.m + " x + " + l.n );
		for ( final PointFunctionMatch p : inliersLine )
			System.out.println( l.distanceTo( p.getP1() ) );
		
		//System.out.println( l.distanceTo( new Point( new float[]{ 1f, 0f } ) ) );
	}
	
	
	
}
