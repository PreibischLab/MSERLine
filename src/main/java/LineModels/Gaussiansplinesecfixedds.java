package LineModels;

public class Gaussiansplinesecfixedds implements MTFitFunction {

	@Override
	public double val(double[] x, double[] a, double[] b) {
		final int ndims = x.length;

		return a[2 * ndims + 1] * Etotal(x, a, b) + a[2 * ndims + 2];

	}

	@Override
	public double grad(double[] x, double[] a, double[] b, int k) {
		final int ndims = x.length;

		if (k < ndims) {

			return 2 * b[k] * (x[k] - a[k]) * a[2 * ndims + 1] * Estart(x, a, b);

		}

		else if (k >= ndims && k <= ndims + 1) {
			int dim = k - ndims;
			return 2 * b[dim] * (x[dim] - a[k]) * a[2 * ndims + 1] * Eend(x, a, b);

		}

		else if (k == 2 * ndims) {

			return a[2 * ndims + 1] * EdC(x, a, b);

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

	private static final double EdC(final double[] x, final double[] a, final double[] b) {

		double di;
		final int ndims = x.length;
		double[] minVal = new double[ndims];
		double[] maxVal = new double[ndims];

		double curvature = a[2 * ndims];
		for (int i = 0; i < x.length; i++) {
			minVal[i] = a[i];
			maxVal[i] = a[ndims + i];
		}
		double slope = (maxVal[1] - minVal[1]) / (maxVal[0] - minVal[0]) - curvature * (maxVal[0] + minVal[0]);

		double ds = b[ndims + 2];
		double mplus2bxstart = slope;
		double mplus2bxend = slope;

		double[] dxvector = { ds / Math.sqrt(1 + slope * slope), slope * ds / Math.sqrt(1 + slope * slope) };

		double[] dxvectorCstart = { -2 * ds * a[0] / (Math.pow(1 + mplus2bxstart * mplus2bxstart, 3 / 2)),
				-2 * ds * mplus2bxstart * a[0] / (Math.pow(1 + mplus2bxstart * mplus2bxstart, 3 / 2)) };
		double[] dxvectorCend = { -2 * ds * a[ndims] / (Math.pow(1 + mplus2bxend * mplus2bxend, 3 / 2)),
				-2 * ds * mplus2bxend * a[ndims] / (Math.pow(1 + mplus2bxend * mplus2bxend, 3 / 2)) };

		double dsum = 0;
		double sum = 0;
		for (int i = 0; i < x.length; i++) {
			minVal[i] += dxvector[i];
			di = x[i] - minVal[i];
			sum += b[i] * di * di;
			dsum += 2 * b[i] * di * dxvectorCstart[i];
		}
		double sumofgaussians = dsum * Math.exp(-sum);

		double dsumend = 0;
		double sumend = 0;
		for (int i = 0; i < x.length; i++) {
			maxVal[i] -= dxvector[i];
			di = x[i] - maxVal[i];
			sumend += b[i] * di * di;
			dsumend += -2 * b[i] * di * dxvectorCend[i];
		}
		sumofgaussians += dsumend * Math.exp(-sumend);

		return sumofgaussians;

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
		double slope = (maxVal[1] - minVal[1]) / (maxVal[0] - minVal[0]) - curvature * (maxVal[0] + minVal[0]);
		double originalslope = b[ndims];

		double ds = b[ndims + 2];

		while (true) {

			sum = 0;

			double dx = ds / Math.sqrt(1 + (slope) * (slope));
			double dy = (slope) * dx;
			double[] dxvector = { dx, dy };

			for (int i = 0; i < x.length; i++) {

				minVal[i] += dxvector[i];
				di = x[i] - minVal[i];
				sum += b[i] * di * di;
			}
			sumofgaussians += Math.exp(-sum);

			if (minVal[0] >= maxVal[0] || minVal[1] >= maxVal[1] && originalslope > 0)
				break;
			if (minVal[0] >= maxVal[0] || minVal[1] <= maxVal[1] && originalslope < 0)
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