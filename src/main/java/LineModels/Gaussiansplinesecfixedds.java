package LineModels;

public class Gaussiansplinesecfixedds implements MTFitFunction {

	@Override
	public double val(double[] x, double[] a, double[] b) {
		final int ndims = x.length;
		
		
		return  a[2 * ndims + 1] * Etotal(x, a, b) + a[2 * ndims + 2] ;
		
	}

	@Override
	public double grad(double[] x, double[] a, double[] b, int k) {
		final int ndims = x.length;

		if (k < ndims) {

			return 2 * b[k] * (x[k] - a[k])  * a[2 * ndims + 1] * Estart(x, a, b);

		}

		else if (k >= ndims && k <= ndims + 1) {
			int dim = k - ndims;
			return 2 * b[dim] * (x[dim] - a[k])  * a[2 * ndims + 1] * Eend(x, a, b);

		}

		
		
		
		else if (k == 2 * ndims ){


			double mplus2bx = 0;
			double dxbydb = 0;
			double[] dxvector = new double[ndims];
			double[] dxvectords = new double[ndims];
			double start = 0, end = 0;
			for (int i = 0; i < ndims; ++i) {

				mplus2bx =  b[ndims] + 2 * a[2 * ndims] * a[0];
				dxbydb = -2 * b[ndims + 2]* mplus2bx * a[0] / (Math.pow(1 + mplus2bx * mplus2bx, 3 / 2));

				if (i == 0){
					dxvector[i] = dxbydb;
					dxvectords[i] = 1.0 / Math.sqrt(1 + mplus2bx * mplus2bx);
					
				}
				else{
					dxvector[i] = mplus2bx * dxbydb;
					dxvectords[i] = mplus2bx / Math.sqrt(1 + mplus2bx * mplus2bx);	
				}
				start += 2 * b[i] * (x[i] - (a[i] + b[ndims + 2] * dxvectords[i])) * a[2 * ndims + 1] * dxvector[i] * Estartds(x, a, b);

			}

			for (int i = ndims; i < 2 * ndims; ++i) {
				int dim = i - ndims;

				mplus2bx =  b[ndims] + 2 * a[2 * ndims] * a[ndims];

				dxbydb = -2 * b[ndims + 2] * mplus2bx * a[ndims] / (Math.pow(1 + mplus2bx * mplus2bx, 3 / 2));

				if (dim == 0){

					dxvector[dim] = dxbydb;
					dxvectords[dim] = 1.0 / Math.sqrt(1 + mplus2bx * mplus2bx);
				}

				else{

					dxvector[dim] = mplus2bx * dxbydb;
					dxvectords[dim] = mplus2bx / Math.sqrt(1 + mplus2bx * mplus2bx);	
				}

				end += 2 * b[dim] * (x[dim] - (a[i] - b[ndims + 2]* dxvectords[dim])) * a[2 * ndims + 1] * dxvector[dim] * Eendds(x, a, b);

			}

			return start - end;
		
		
		
		}
		
		else if (k == 2 * ndims + 1)
			
			return Etotal(x, a, b);
		
		
		else if (k == 2 * ndims + 2)
			return 1.0;
		
		else
			return 0;

	}

	/*
	 * PRIVATE METHODS
	 */

	/*
	 * @ Define a line analytically as a sum of gaussians, the parameters to be
	 * determined are the start and the end points of the line
	 * 
	 */

	private static final double Estart(final double[] x, final double[] a, final double[] b) {

		double sum = 0;
		double di;
		for (int i = 0; i < x.length; i++) {
			di = x[i] - a[i];
			sum += b[i] * di * di;
		}

		return Math.exp(-sum);

	}

	
	private static final double Estartds(final double[] x, final double[] a, final double[] b) {

		double sum = 0;
		double di;
		final int ndims = x.length;
		double[] minVal = new double[ndims];
		double[] maxVal = new double[ndims];
		double slope = b[x.length];
		for (int i = 0; i < x.length; i++) {
			minVal[i] = a[i];
			maxVal[i] = a[ndims + i];
		}
		double curvature = a[2 * ndims + 1];
		double ds = b[ndims + 2];

		double dx = ds / Math.sqrt(1 + (slope + 2 * curvature * minVal[0]) * (slope + 2 * curvature * minVal[0]));
		double dy = (slope + 2 * curvature * minVal[0]) * dx;
		double[] dxvector = { dx, dy };

		for (int i = 0; i < x.length; i++) {
			di = x[i] - (a[i] + dxvector[i]);
			sum += b[i] * di * di;
		}

		return Math.exp(-sum);

	}
	private static final double Eendds(final double[] x, final double[] a, final double[] b) {

		double sum = 0;
		double di;
		final int ndims = x.length;
		double[] minVal = new double[ndims];
		double[] maxVal = new double[ndims];
		double slope = b[x.length];
		for (int i = 0; i < x.length; i++) {
			minVal[i] = a[i];
			maxVal[i] = a[ndims + i];
		}
		double curvature = a[2 * ndims + 1];
		double ds = b[ndims + 2];

		double dx = ds / Math.sqrt(1 + (slope + 2 * curvature * minVal[0]) * (slope + 2 * curvature * minVal[0]));
		double dy = (slope + 2 * curvature * minVal[0]) * dx;
		double[] dxvector = { dx, dy };

		for (int i = 0; i < x.length; i++) {
			di = x[i] - (a[i + ndims] - dxvector[i]);
			sum += b[i] * di * di;
		}

		return Math.exp(-sum);

	}

	
	private static final double Eend(final double[] x, final double[] a, final double[] b) {

		double sum = 0;
		double di;
		int ndims = x.length;
		for (int i = 0; i < x.length; i++) {
			di = x[i] - a[i + ndims];
			sum += b[i] * di * di;
		}

		return Math.exp(-sum);

	}

	private static final double Etotal(final double[] x, final double[] a, final double[] b) {

		return Estart(x, a, b) + Esum(x, a, b) + Eend(x, a, b);

	}

	
	
	private static final double Esum(final double[] x, final double[] a, final double[] b) {

		final int ndims = x.length;
		double[] minVal = new double[ndims];
		double[] maxVal = new double[ndims];

		for (int i = 0; i < x.length; i++) {
			minVal[i] = a[i];
			maxVal[i] = a[ndims + i];
		}
		double sum = 0;
		double sumofgaussians = 0;
		double di;
		double curvature = a[2 * ndims];
		double slope = b[ndims];
		
		
		double ds = b[ndims + 2];
		
		while (true) {
			
			sum = 0;
			
			double  dx =  ds / Math.sqrt( 1 + (slope + 2 * curvature *minVal[0]  ) *(slope+ 2 * curvature *minVal[0] ));
			 double dy = (slope + 2 * curvature *minVal[0]  ) * dx;
			 double[] dxvector = { dx, dy };
			
			 
			for (int i = 0; i < x.length; i++) {
				
				
				minVal[i] += dxvector[i];
				di = x[i] - minVal[i];
				sum += b[i] * di * di;
			}
			sumofgaussians += Math.exp(-sum);

			
			if (minVal[0] >= maxVal[0] || minVal[1] >= maxVal[1] && slope > 0)
				break;
			if (minVal[0] >= maxVal[0] || minVal[1] <= maxVal[1] && slope < 0)
				break;

		}
		
		
		

		return sumofgaussians;
	}

	private static final double EsumC(final double[] x, final double[] a, final double[] b) {
		final int ndims = x.length;
		double[] minVal = new double[ndims];
		double[] maxVal = new double[ndims];
		double sum = 0, dsum = 0;
		double sumofgaussians = 0;
		double di, de;
	
		for (int i = 0; i < x.length; i++) {
			minVal[i] = a[i];
			maxVal[i] = a[ndims + i];
		}
	
		double slope = b[x.length];
		double curvature = a[2 * ndims];
		double ds = b[ndims + 2];
	double dx =  ds / Math.sqrt( 1 + (slope + 2 * curvature *minVal[0]  ) *(slope+ 2 * curvature *minVal[0]  ));
	double dy = (slope + 2 * curvature *minVal[0] ) * dx;
	double[] dxvector = { dx, dy };
	double dxderiv = - ds * 2* minVal[0]* (slope + 2 * curvature * minVal[0]) / Math.pow(1 + Math.pow((slope + 2 * curvature * minVal[0]),  2), 3/2);
	double[] dxvectorderiv = {dxderiv, dxderiv * (slope + 2 * curvature * minVal[0])};
	double dxderivend =  -ds * 2* maxVal[0]* (slope + 2 * curvature * maxVal[0]) / Math.pow(1 + Math.pow((slope + 2 * curvature * maxVal[0]),  2), 3/2);
	double[] dxvectorderivend = {dxderivend, dxderivend * (slope + 2 * curvature * maxVal[0])};
	for (int i = 0; i < x.length; i++) {
		
		minVal[i] += dxvector[i];
		di = x[i] - minVal[i];
		sum += b[i] * di * di;
		dsum += 2 * b[i] * di * dxvectorderiv[i];
	}
	sumofgaussians = dsum * Math.exp(-sum);
	
	double dsumend = 0;
	double sumend = 0;
	double dxend =  ds / Math.sqrt( 1 + (slope + 2 * curvature *maxVal[0]  ) *(slope+ 2 * curvature *maxVal[0]  ));
	double dyend = (slope + 2 * curvature *maxVal[0] ) * dxend;
	double[] dxvectorend = { dxend, dyend };
	
	for (int i = 0; i < x.length; i++) {
		
		maxVal[i] -= dxvectorend[i];
		di = x[i] - maxVal[i];
		sumend += b[i] * di * di;
		dsumend += -2 * b[i] * di * dxvectorderivend[i];
	}
	sumofgaussians+= dsumend * Math.exp(-sumend);
	
	
	return    sumofgaussians ;
			 
			
			
			 
		

	}
	
	
	
	
	
	public static double Distance(final double[] cordone, final double[] cordtwo) {

		double distance = 0;
		final double ndims = cordone.length;

		for (int d = 0; d < ndims; ++d) {

			distance += Math.pow((cordone[d] - cordtwo[d]), 2);

		}
		return Math.sqrt(distance);
	}

}

