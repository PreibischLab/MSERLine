package LineModels;

public class GaussianSplinesecorder implements MTFitFunction {

	@Override
	public double val(double[] x, double[] a, double[] b) {
		final int ndims = x.length;

		return a[2 * ndims + 2] * Etotal(x, a, b) + a[2 * ndims + 3];

	}

	@Override
	public double grad(double[] x, double[] a, double[] b, int k) {
		final int ndims = x.length;

		if (k < ndims) {

			return 2 * b[k] * (x[k] - a[k]) * a[2 * ndims + 2] * Estart(x, a, b);

		}

		else if (k >= ndims && k <= ndims + 1) {
			int dim = k - ndims;
			return 2 * b[dim] * (x[dim] - a[k]) * a[2 * ndims + 2] * Eend(x, a, b);

		}
		else if (k == 2 * ndims) {
			double mplus2bx = 0;
			double[] dxvector = new double[ndims];

			double start = 0, end = 0;
			for (int i = 0; i < ndims; ++i) {

				mplus2bx =  b[ndims] + 2 * a[2 * ndims + 1] * a[0];
				if (i == 0)

					dxvector[i] = 1.0 / Math.sqrt(1 + mplus2bx * mplus2bx);

				else

					dxvector[i] = mplus2bx / Math.sqrt(1 + mplus2bx * mplus2bx);
				
				
				start += 2 * b[i] * (x[i] - (a[i] + a[2 * ndims]* dxvector[i])) * a[2 * ndims + 2] * dxvector[i] * Estartds(x, a, b);

			}

			for (int i = ndims; i < 2 * ndims; ++i) {
				int dim = i - ndims;
				mplus2bx =  b[ndims] + 2 * a[2 * ndims + 1] * a[ndims];
				if (dim == 0)

					dxvector[dim] = 1.0 / Math.sqrt(1 + mplus2bx * mplus2bx);

				else

					dxvector[dim] = mplus2bx / Math.sqrt(1 + mplus2bx * mplus2bx);

				end += 2 * b[dim] * (x[dim] - (a[i] - a[2 * ndims]* dxvector[dim])) * a[2 * ndims + 2] * dxvector[dim] * Eendds(x, a, b);

			}

			return start - end;
		}

		else if (k == 2 * ndims + 1) {
		
			double mplus2bx = 0;
			double dxbydb = 0;
			double[] dxvector = new double[ndims];
			double[] dxvectords = new double[ndims];
			double start = 0, end = 0;
			for (int i = 0; i < ndims; ++i) {

				mplus2bx =  b[ndims] + 2 * a[2 * ndims + 1] * a[0];
				dxbydb = -2 * a[2 * ndims] * mplus2bx * a[0] / (Math.pow(1 + mplus2bx * mplus2bx, 3 / 2));

				if (i == 0){
					dxvector[i] = dxbydb;

					dxvectords[i] = 1.0 / Math.sqrt(1 + mplus2bx * mplus2bx);
				}
				else{
					dxvector[i] = mplus2bx * dxbydb;
					dxvectords[i] = mplus2bx / Math.sqrt(1 + mplus2bx * mplus2bx);
					
				}
				start += 2 * b[i] * (x[i] - (a[i] + a[2 *ndims]* dxvectords[i])) * a[2 * ndims + 2] * dxvector[i] * Estartds(x, a, b);

			}

			for (int i = ndims; i < 2 * ndims; ++i) {
				int dim = i - ndims;

				mplus2bx =  b[ndims] + 2 * a[2 * ndims + 1] * a[ndims];

				dxbydb = -2 * a[2 * ndims] * mplus2bx * a[ndims] / (Math.pow(1 + mplus2bx * mplus2bx, 3 / 2));

				if (dim == 0){

					dxvector[dim] = dxbydb;
					dxvectords[dim] = 1.0 / Math.sqrt(1 + mplus2bx * mplus2bx);
				}
				else{

					dxvector[dim] = mplus2bx * dxbydb;
					dxvectords[dim] = mplus2bx / Math.sqrt(1 + mplus2bx * mplus2bx);
				}
				end += 2 * b[dim] * (x[dim] - (a[i] - a[2 * ndims]* dxvectords[dim])) * a[2 * ndims + 2] * dxvector[dim] * Eendds(x, a, b);

			}

			return start - end;

		}

		
		else if (k == 2 * ndims + 2)

			return Etotal(x, a, b);

		else if (k == 2 * ndims + 3)
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
		double ds = Math.abs(a[2 * ndims]);

		double dx = ds / Math.sqrt(1 + (slope + 2 * curvature * minVal[0]) * (slope + 2 * curvature * minVal[0]));
		double dy = (slope + 2 * curvature * minVal[0]) * dx;
		double[] dxvector = { dx, dy };

		for (int i = 0; i < x.length; i++) {
			di = x[i] - (a[i] + dxvector[i]);
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
		double ds = Math.abs(a[2 * ndims]);

		double dx = ds / Math.sqrt(1 + (slope + 2 * curvature * maxVal[0]) * (slope + 2 * curvature * maxVal[0]));
		double dy = (slope + 2 * curvature * maxVal[0]) * dx;
		double[] dxvector = { dx, dy };

		for (int i = 0; i < x.length; i++) {
			di = x[i] - (a[i + ndims] - dxvector[i]);
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
		double curvature = a[2 * ndims + 1];
		double slope = b[ndims];

		double ds = Math.abs(a[2 * ndims]);

		while (true) {

			sum = 0;

			double dx = ds / Math.sqrt(1 + (slope + 2 * curvature * minVal[0]) * (slope + 2 * curvature * minVal[0]));
			double dy = (slope + 2 * curvature * minVal[0]) * dx;
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

	

	public static double Distance(final double[] cordone, final double[] cordtwo) {

		double distance = 0;
		final double ndims = cordone.length;

		for (int d = 0; d < ndims; ++d) {

			distance += Math.pow((cordone[d] - cordtwo[d]), 2);

		}
		return Math.sqrt(distance);
	}

}
