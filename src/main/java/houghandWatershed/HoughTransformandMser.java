package houghandWatershed;

import java.util.ArrayList;
import drawandOverlay.HoughPushCurves;
import drawandOverlay.OverlayLines;
import ij.gui.EllipseRoi;
import net.imglib2.FinalInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.FloatType;
import preProcessing.GetLocalmaxmin;
import preProcessing.GlobalThresholding;

/**
 * Hough transform of images that operates on 2D images.
 * <p>
 * For 3D images, the Hough transform is done only in 2D XY slices.
 * 
 * @author Varun Kapoor - 2016
 *
 * @param <T>
 *            the type of the source image.
 */

public class HoughTransformandMser extends BenchmarkAlgorithm implements OutputAlgorithm<double[]> {

	private static final String BASE_ERROR_MSG = "[HoughTransform2D] ";
	private final RandomAccessibleInterval<FloatType> source;
	private final int label;
	private double[] slopeandintercept;

	/**
	 * Instantiate a new Hough Transform object that does Hough Transform on 2D
	 * images or slices of a 3D image.
	 * 
	 * @param source
	 *            the source 2D image on which the Hough transform has to be
	 *            done
	 * @param bitimg
	 *            the bitimg of the source to be used for doing the distance
	 *            transform and watershedding, created using user defined
	 *            threshold
	 * @param minlength
	 *            the minimum length of the lines to be detected this is done to
	 *            avoid dots which appear in microscopy images.
	 * 
	 */

	public HoughTransformandMser(final int label, final RandomAccessibleInterval<FloatType> source) {

		this.source = source;
		this.label = label;
		

	}

	public double[] getslopeandintercept() {

		return slopeandintercept;
	}

	@Override
	public boolean checkInput() {
		if (source.numDimensions() > 2) {
			errorMessage = BASE_ERROR_MSG + " Can only operate on 1D, 2D, make slices of your stack . Got "
					+ source.numDimensions() + "D.";
			return false;
		}

		return true;
	}

	@Override
	public boolean process() {

		final int ndims = source.numDimensions();

		final Float ThresholdValue = GlobalThresholding.AutomaticThresholding(source);
		
		
		final double[] sizes = new double[ndims];
        slopeandintercept = new double[ndims];
	           
			
			System.out.println("Doing Hough Transform to determine line parameters in Label:" + " " + label);
			// Set size of pixels in Hough space
			int mintheta = 0;

			// Usually is 180 but to allow for detection of vertical
			// lines,allowing a few more degrees

			int maxtheta = 240;
			double size = Math
					.sqrt((source.dimension(0) * source.dimension(0) + source.dimension(1) * source.dimension(1)));
			int minRho = (int) -Math.round(size);
			int maxRho = -minRho;
			double thetaPerPixel = 1;
			double rhoPerPixel = 1;
			double[] min = { mintheta, minRho };
			double[] max = { maxtheta, maxRho };
			int pixelsTheta = (int) Math.round((maxtheta - mintheta) / thetaPerPixel);
			int pixelsRho = (int) Math.round((maxRho - minRho) / rhoPerPixel);

			double ratio = (max[0] - min[0]) / (max[1] - min[1]);
			FinalInterval interval = new FinalInterval(new long[] { pixelsTheta, (long) (pixelsRho * ratio) });
			final RandomAccessibleInterval<FloatType> houghimage = new ArrayImgFactory<FloatType>().create(interval,
					new FloatType());

			HoughPushCurves.Houghspace(source, houghimage, min, max, ThresholdValue);

			for (int d = 0; d < houghimage.numDimensions(); ++d)
				sizes[d] = houghimage.dimension(d);

			// Define Arraylist to get the slope and the intercept of the
			// Hough
			// detected lines
			ArrayList<RefinedPeak<Point>> SubpixelMinlist = new ArrayList<RefinedPeak<Point>>(
					source.numDimensions());

			// Get the list of all the detections
			SubpixelMinlist = GetLocalmaxmin.HoughspaceMaxima(houghimage, interval, sizes, thetaPerPixel, rhoPerPixel);

			// Reduce the number of detections by picking One line per
			// Label,
			// using the best detection for each label
			RefinedPeak<Point> ReducedMinlistsingle = OverlayLines.ReducedListsingle(source, SubpixelMinlist, sizes,
					min, max);

			if (ReducedMinlistsingle != null) {
				double[] points = OverlayLines.GetRhoThetasingle(ReducedMinlistsingle, sizes, min, max);

				RefinedPeak<Point> peak = OverlayLines.ReducedListsingle(source, SubpixelMinlist, sizes, min, max);

				points = OverlayLines.GetRhoThetasingle(peak, sizes, min, max);

				double slope = -1.0 / (Math.tan(Math.toRadians(points[0])));
				double intercept = points[1] / Math.sin(Math.toRadians(points[0]));

				slopeandintercept[0] = slope;
				slopeandintercept[1] = intercept;
				
				 

			}
			/**
			 * This object has rho, theta, min dimensions, max dimensions of the
			 * label
			 * 
			 */
		

		return true;
	}

	@Override
	public double[] getResult() {

		return slopeandintercept;
	}

	public static double Distance(final long[] minCorner, final long[] maxCorner) {

		double distance = 0;

		for (int d = 0; d < minCorner.length; ++d) {

			distance += Math.pow((minCorner[d] - maxCorner[d]), 2);

		}
		return Math.sqrt(distance);
	}
}
