package peakFitter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.tools.javac.util.Pair;

import LineModels.GaussianLineds;
import LineModels.GaussianLinemaxds;
import LineModels.GaussianLineminds;
import graphconstructs.Staticproperties;
import graphconstructs.Trackproperties;
import ij.gui.EllipseRoi;
import labeledObjects.CommonOutput;
import labeledObjects.CommonOutputHF;
import labeledObjects.LabelledImg;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Point;
import net.imglib2.PointSampleList;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import peakFitter.GaussianMaskFitMSER.EndfitMSER;
import preProcessing.GetLocalmaxmin;
import util.Boundingboxes;

public class SubpixelVelocityPCLine extends BenchmarkAlgorithm
		implements OutputAlgorithm<Pair<ArrayList<double[]>, ArrayList<double[]>>> {

	private static final String BASE_ERROR_MSG = "[SubpixelVelocity] ";
	private final RandomAccessibleInterval<FloatType> source;
	private final ArrayList<CommonOutputHF> imgs;
	private final ArrayList<double[]> PrevFrameparamstart;
	private final ArrayList<double[]> PrevFrameparamend;
	private final int ndims;
	private final int framenumber;
	private ArrayList<double[]> final_paramliststart;
	private ArrayList<double[]> final_paramlistend;
	private ArrayList<Trackproperties> startinframe;
	private ArrayList<Trackproperties> endinframe;
	private final double[] psf;

	// LM solver iteration params
	public int maxiter = 500;
	public double lambda = 1e-3;
	public double termepsilon = 1e-1;
	// Mask fits iteration param
	public int iterations = 500;
	public double cutoffdistance = 20;
	public boolean halfgaussian = false;
	public double Intensityratio = 0.5;

	
	public void setCutoffdistance(double cutoffdistance) {
		this.cutoffdistance = cutoffdistance;
	}
	public double getCutoffdistance() {
		return cutoffdistance;
	}
	
	public void setIntensityratio(double intensityratio) {
		Intensityratio = intensityratio;
	}
	
	public double getIntensityratio() {
		return Intensityratio;
	}
	public void setMaxiter(int maxiter) {
		this.maxiter = maxiter;
	}
	
	public int getMaxiter() {
		return maxiter;
	}
	
	public void setLambda(double lambda) {
		this.lambda = lambda;
	}
	
	public double getLambda() {
		return lambda;
	}
	
	public void setTermepsilon(double termepsilon) {
		this.termepsilon = termepsilon;
	}
	
	public double getTermepsilon() {
		return termepsilon;
	}
	
	public void setHalfgaussian(boolean halfgaussian) {
		this.halfgaussian = halfgaussian;
	}
	public SubpixelVelocityPCLine(final RandomAccessibleInterval<FloatType> source,
			final ArrayList<CommonOutputHF> imgs, final ArrayList<double[]> PrevFrameparamstart,
			final ArrayList<double[]> PrevFrameparamend, final double[] psf, final int framenumber) {

		this.source = source;
		this.imgs = imgs;
		this.PrevFrameparamstart = PrevFrameparamstart;
		this.PrevFrameparamend = PrevFrameparamend;
		this.psf = psf;
		this.framenumber = framenumber;
		this.ndims = source.numDimensions();

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
	
	public enum StartorEnd{
		
		Start, End
		
	}

	@Override
	public boolean process() {

		final_paramliststart = new ArrayList<double[]>();
		final_paramlistend = new ArrayList<double[]>();
		startinframe = new ArrayList<Trackproperties>();
		endinframe = new ArrayList<Trackproperties>();

		final int oldframenumber = (int) PrevFrameparamstart.get(PrevFrameparamstart.size() - 1)[ndims + 6];
		final int framediff = framenumber - oldframenumber;

		for (int index = 0; index < PrevFrameparamstart.size(); ++index) {

			final int seedlabel = (int) PrevFrameparamstart.get(index)[ndims + 5];

			final double oldslope = PrevFrameparamstart.get(index)[ndims + 3];

			final double oldintercept = PrevFrameparamstart.get(index)[ndims + 4];

			Point linepoint = new Point(ndims);
			linepoint.setPosition(
					new long[] { (long) PrevFrameparamstart.get(index)[0], (long) PrevFrameparamstart.get(index)[1] });

			int labelstart = Getlabel(linepoint, oldslope, oldintercept);

			double[] paramnextframestart = Getfinaltrackparam(PrevFrameparamstart.get(index), labelstart, psf,
					framenumber, StartorEnd.Start);

			if (paramnextframestart == null)
				paramnextframestart = PrevFrameparamstart.get(index);

			final_paramliststart.add(paramnextframestart);

			final double[] oldstartpoint = { PrevFrameparamstart.get(index)[0], PrevFrameparamstart.get(index)[1] };

			final double[] newstartpoint = { paramnextframestart[0], paramnextframestart[1] };
			
			final double newstartslope = (paramnextframestart[3] - paramnextframestart[1])
					/ (paramnextframestart[2] - paramnextframestart[0]);
			final double newstartintercept = paramnextframestart[1] - newstartslope * paramnextframestart[0];
			final double[] directionstart = { (newstartpoint[0] - oldstartpoint[0]) / framediff,
					(newstartpoint[1] - oldstartpoint[1]) / framediff };

			final Trackproperties startedge = new Trackproperties(labelstart, oldstartpoint, newstartpoint,
					newstartslope, newstartintercept, directionstart, seedlabel, framenumber);

			startinframe.add(startedge);

		}

		for (int index = 0; index < PrevFrameparamend.size(); ++index) {

			Point secondlinepoint = new Point(ndims);
			secondlinepoint.setPosition(
					new long[] { (long) PrevFrameparamend.get(index)[0], (long) PrevFrameparamend.get(index)[1] });

			final int seedlabel = (int) PrevFrameparamend.get(index)[ndims + 5];

			final double oldslope = PrevFrameparamend.get(index)[ndims + 3];

			final double oldintercept = PrevFrameparamend.get(index)[ndims + 4];

			int labelend = Getlabel(secondlinepoint, oldslope, oldintercept);
			double[] paramnextframeend = Getfinaltrackparam(PrevFrameparamend.get(index), labelend, psf, framenumber, StartorEnd.End);
			if (paramnextframeend == null)
				paramnextframeend = PrevFrameparamend.get(index);
			final_paramlistend.add(paramnextframeend);

			final double[] oldendpoint = { PrevFrameparamend.get(index)[0], PrevFrameparamend.get(index)[1] };

			double[] newendpoint = { paramnextframeend[0], paramnextframeend[1] };
			
			final double newendslope = (paramnextframeend[3] - paramnextframeend[1])
					/ (paramnextframeend[2] - paramnextframeend[0]);
			final double newendintercept = paramnextframeend[1] - newendslope * paramnextframeend[0];
			final double[] directionend = { (newendpoint[0] - oldendpoint[0]) / framediff,
					(newendpoint[1] - oldendpoint[1]) / framediff };

			final Trackproperties endedge = new Trackproperties(labelend, oldendpoint, newendpoint, newendslope,
					newendintercept, directionend, seedlabel, framenumber);

			endinframe.add(endedge);

		}

		return true;
	}

	@Override
	public Pair<ArrayList<double[]>, ArrayList<double[]>> getResult() {

		Pair<ArrayList<double[]>, ArrayList<double[]>> listpair = new Pair<ArrayList<double[]>, ArrayList<double[]>>(
				final_paramliststart, final_paramlistend);

		return listpair;
	}

	public ArrayList<Trackproperties> getstartStateVectors() {
		return startinframe;
	}

	public ArrayList<Trackproperties> getendStateVectors() {
		return endinframe;
	}

	private final double[] MakerepeatedLineguess(double[] iniparam, int label) {
		long[] newposition = new long[ndims];
		double[] minVal = { Double.MAX_VALUE, Double.MAX_VALUE };
		double[] maxVal = { -Double.MIN_VALUE, -Double.MIN_VALUE };

		RandomAccessibleInterval<FloatType> currentimg = imgs.get(label).Actualroi;

		FinalInterval interval = imgs.get(label).interval;

		currentimg = Views.interval(currentimg, interval);

		double slope = iniparam[ndims + 3];
		double intercept = iniparam[ndims + 4];
		double newintercept = intercept;

		final Cursor<FloatType> outcursor = Views.iterable(currentimg).localizingCursor();

		final double maxintensityline = GetLocalmaxmin.computeMaxIntensity(currentimg);
		while (outcursor.hasNext()) {

			outcursor.fwd();

			if (outcursor.get().get() / maxintensityline > Intensityratio) {

				outcursor.localize(newposition);

				long pointonline = (long) (outcursor.getLongPosition(1) - slope * outcursor.getLongPosition(0)
						- newintercept);

				// To get the min and max co-rodinates along the line so we
				// have starting points to
				// move on the line smoothly

				if (pointonline == 0) {
					for (int d = 0; d < ndims; ++d) {
						if (outcursor.getDoublePosition(d) <= minVal[d])
							minVal[d] = outcursor.getDoublePosition(d);

						if (outcursor.getDoublePosition(d) >= maxVal[d])
							maxVal[d] = outcursor.getDoublePosition(d);

					}
				}
			}
		}
		final double[] MinandMax = new double[2 * ndims + 3];

		if (slope >= 0) {
			for (int d = 0; d < ndims; ++d) {

				MinandMax[d] = minVal[d];
				MinandMax[d + ndims] = maxVal[d];
			}

		}

		if (slope < 0) {

			MinandMax[0] = minVal[0];
			MinandMax[1] = maxVal[1];
			MinandMax[2] = maxVal[0];
			MinandMax[3] = minVal[1];

		}

		MinandMax[2 * ndims] = iniparam[ndims];
		MinandMax[2 * ndims + 1] = iniparam[ndims + 1];
		MinandMax[2 * ndims + 2] = iniparam[ndims + 2];

		

		for (int d = 0; d < ndims; ++d) {

			if (MinandMax[d] == Double.MAX_VALUE || MinandMax[d + ndims] == -Double.MIN_VALUE)
				return null;
			if (MinandMax[d] >= source.dimension(d) || MinandMax[d + ndims] >= source.dimension(d))
				return null;
			if (MinandMax[d] <= 0 || MinandMax[d + ndims] <= 0)
				return null;

		}

		return MinandMax;

	}

	public double[] Getfinaltrackparam(final double[] iniparam, final int label, final double[] psf, final int rate, final StartorEnd startorend) {

		if (iniparam == null || label == Integer.MIN_VALUE)
			return null;

		else {

			PointSampleList<FloatType> datalist = gatherfullData(label);
			final Cursor<FloatType> listcursor = datalist.localizingCursor();
			double[][] X = new double[(int) datalist.size()][ndims];
			double[] I = new double[(int) datalist.size()];
			int index = 0;
			while (listcursor.hasNext()) {
				listcursor.fwd();

				for (int d = 0; d < ndims; d++) {
					X[index][d] = listcursor.getDoublePosition(d);
				}

				I[index] = listcursor.get().getRealDouble();

				index++;
			}

			final double[] LMparam = MakerepeatedLineguess(iniparam, label);
			if (LMparam == null)
				return null;

			else {
				RandomAccessibleInterval<FloatType> currentimg = imgs.get(label).Actualroi;

				FinalInterval interval = imgs.get(label).interval;

				currentimg = Views.interval(currentimg, interval);

				final double[] fixed_param = new double[ndims];

				for (int d = 0; d < ndims; ++d) {

					fixed_param[d] = 1.0 / Math.pow(psf[d], 2);
					
				}

				
				System.out.println("Label: " + label + " " + "Initial guess: " + " StartX: " + LMparam[0] + " StartY: "
						+ LMparam[1] + " EndX: " + LMparam[2] + " EndY: " + LMparam[3]);
				
				final double[] inistartpos = {LMparam[0], LMparam[1]};
				final double[] iniendpos = {LMparam[2], LMparam[3]};
				final double[] inipos = {iniparam[0], iniparam[1]};
				double inicutoffdistance = Distance(inistartpos, iniendpos);

				
				
				// LM solver part
				if (inicutoffdistance > 2) {
					try {
						LevenbergMarquardtSolverLine.solve(X, LMparam, fixed_param, I, new GaussianLineds(), lambda,
								termepsilon, maxiter);
					} catch (Exception e) {
						e.printStackTrace();
					}
				

					final double[] startpos = new double[ndims];
					final double[] endpos = new double[ndims];

					for (int d = 0; d < ndims; ++d) {
						startpos[d] = LMparam[d];
						endpos[d] = LMparam[d + ndims];

					}


					double[] returnparam = new double[ndims + 7];

					final double maxintensityline = GetLocalmaxmin.computeMaxIntensity(currentimg);

					double newslope = (endpos[1] - startpos[1]) / (endpos[0] - startpos[0]);
					double newintercept = (endpos[1] - newslope * endpos[0]);
					double dx = LMparam[4] / Math.sqrt(1 + newslope * newslope);
					double dy = newslope * dx;
					double[] dxvector = { dx, dy };

					double[] startfit = new double[ndims];
					double[] endfit = new double[ndims];

				

					System.out.println("Frame: " + framenumber);

					
					// If mask fits fail, return LM solver results


						
						try {
							endfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, endpos.clone(), psf, iterations,
									dxvector, newslope, newintercept, maxintensityline, halfgaussian, EndfitMSER.EndfitMSER,
									label);
						} catch (Exception e) {
							e.printStackTrace();
						}
						try {
							startfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, startpos.clone(), psf, iterations,
									dxvector, newslope, newintercept, maxintensityline, halfgaussian, EndfitMSER.StartfitMSER,
									label);
						} catch (Exception e) {
							e.printStackTrace();
						}
						for (int d = 0; d < ndims; ++d) {
							 LMparam[d] = startfit[d];
							LMparam[d + ndims] = endpos[d];

						}
						final double LMdist = sqDistance(startpos, endpos);
						final double Maskdist = sqDistance(startfit, endfit);
						
						if (Math.abs(Math.sqrt(Maskdist)) - Math.sqrt(LMdist) > cutoffdistance) {
							if (Math.abs(startpos[0] - startfit[0]) >= cutoffdistance / 2
									&& Math.abs(startpos[1] - startfit[1]) >= cutoffdistance / 2
									|| Math.abs(endpos[0] - endfit[0]) >= cutoffdistance / 2
											&& Math.abs(endpos[1] - endfit[1]) >= cutoffdistance / 2) {
								System.out.println("Mask fits fail, returning LM solver results!");

								for (int d = 0; d < ndims; ++d) {
									LMparam[d] = startpos[d];
									LMparam[d + ndims] = endpos[d];

								}
							}

							if (Math.abs(startpos[0] - startfit[0]) >= cutoffdistance
									|| Math.abs(startpos[1] - startfit[1]) >= cutoffdistance
									|| Math.abs(endpos[0] - endfit[0]) >= cutoffdistance
									|| Math.abs(endpos[1] - endfit[1]) >= cutoffdistance) {
								System.out.println("Mask fits fail, returning LM solver results!");
								for (int d = 0; d < ndims; ++d) {
									LMparam[d] = startpos[d];
									LMparam[d + ndims] = endpos[d];

								}

							}

						}

						for (int d = 0; d < ndims; ++d) {
							if (Double.isNaN(startfit[d]) || Double.isNaN(endfit[d])) {
								System.out.println("Mask fits fail, returning LM solver results!");
									LMparam[d] = startpos[d];
									LMparam[d + ndims] = endpos[d];


							}

						}
						
						
						
					

					for (int j = 0; j < LMparam.length; j++) {
						if (Double.isNaN(LMparam[j]))
							LMparam[j] = iniparam[j];
					}
					
					
					final double[] finalstartpoint = {LMparam[0], LMparam[1]};
					final double[] finalendpoint = {LMparam[2], LMparam[3]};
					
				if (startorend== StartorEnd.Start){
					
					// Growth criteria for the start 
					if (finalstartpoint[0] <= inipos[0]){
					for (int d = 0; d < ndims; ++d) {
						returnparam[d] = finalstartpoint[d];
					}
					
					
						
					}
				
				else{
					
					for (int d = 0; d < ndims; ++d) {
						returnparam[d] = finalendpoint[d];
					}
					
				
					
					
				}
				
				}
				else{
					
					
					// Growth criteria for the end
					if (finalendpoint[0] >= inipos[0]){
					
					
					for (int d = 0; d < ndims; ++d) {
						returnparam[d] = finalendpoint[d];
					}

				}
					
					else{
						for (int d = 0; d < ndims; ++d) {
							returnparam[d] = finalstartpoint[d];
						}
						
						
						
					}
					
					
				}
				
				
			final double	currentslope =  (returnparam[1] - inipos[1]) / (returnparam[0] - inipos[0]);
			final double	currentintercept = returnparam[1] - currentslope * returnparam[0];
				System.out.println("Label: " + label + " X: " + returnparam[0] + " Y: " + returnparam[1]  );
				
					returnparam[ndims] = LMparam[2*ndims];
					returnparam[ndims + 1] = LMparam[2*ndims + 1];
					returnparam[ndims + 2] = LMparam[2*ndims + 2];

					returnparam[ndims + 3] = currentslope;
					returnparam[ndims + 4] = currentintercept;

					returnparam[ndims + 5] = iniparam[7];
					returnparam[ndims + 6] = framenumber;

					return returnparam;
				} 
				
				else
					return null;
			}

		}

	}

	private PointSampleList<FloatType> gatherfullData(final int label) {
		final PointSampleList<FloatType> datalist = new PointSampleList<FloatType>(ndims);

		RandomAccessibleInterval<FloatType> currentimg = imgs.get(label).Actualroi;

		FinalInterval interval = imgs.get(label).interval;

		currentimg = Views.interval(currentimg, interval);

		Cursor<FloatType> localcursor = Views.iterable(currentimg).localizingCursor();

		while (localcursor.hasNext()) {
			localcursor.fwd();

			Point newpoint = new Point(localcursor);
			datalist.add(newpoint, localcursor.get().copy());

		}

		return datalist;
	}

	public int Getlabel(final Point linepoint, final double oldslope, final double oldintercept) {

		ArrayList<Integer> currentlabel = new ArrayList<Integer>();
		int finallabel = Integer.MIN_VALUE;
		int pointonline = Integer.MAX_VALUE;
		for (int index = 0; index < imgs.size(); ++index) {

			RandomAccessibleInterval<FloatType> currentimg = imgs.get(index).Actualroi;
			FinalInterval interval = imgs.get(index).interval;
			currentimg = Views.interval(currentimg, interval);

			if (linepoint.getIntPosition(0) >= interval.min(0) && linepoint.getIntPosition(0) <= interval.max(0)
					&& linepoint.getIntPosition(1) >= interval.min(1)
					&& linepoint.getIntPosition(1) <= interval.max(1)) {

				currentlabel.add(imgs.get(index).roilabel);
			}

		}
		for (int index = 0; index < currentlabel.size(); ++index) {
			int distfromline = (int) Math
					.abs(linepoint.getIntPosition(1) - oldslope * linepoint.getIntPosition(0) - oldintercept);

			if (distfromline < pointonline) {

				pointonline = distfromline;
				finallabel = currentlabel.get(index);

			}

		}

		return finallabel;
	}

	public static double Distance(final double[] cordone, final double[] cordtwo) {

		double distance = 0;

		int ndims = cordone.length;
		for (int d = 0; d < ndims; ++d) {

			distance += Math.pow((cordone[d] - cordtwo[d]), 2);

		}
		return Math.sqrt(distance);
	}

	public static double sqDistance(final double[] cordone, final double[] cordtwo) {

		double distance = 0;
		int ndims = cordone.length;
		for (int d = 0; d < ndims; ++d) {

			distance += Math.pow((cordone[d] - cordtwo[d]), 2);

		}
		return (distance);
	}
	
	public static double Xcorddist(final double Xcordone, final double Xcordtwo){
		
		double distance = Math.abs(Xcordone - Xcordtwo);
		
		return distance;
	}
	
	
public static double dsdist(final double[] cordone, final double[] cordtwo){
		
		double distance = Math.pow((cordone[0] - cordtwo[0]),2) + Math.pow((cordone[1] - cordtwo[1]),2);
		
		return distance;
	}

public static double[] Midpoint (final double[] cordone, final double[] cordtwo){
	int ndims = cordone.length;
	final double[] midpoint = new double[ndims];
	
	for (int d = 0 ; d < ndims; ++d ){
		
		midpoint[d] = ( cordone[d] +cordtwo[d] ) / 2;
	}
	
	return midpoint;
	
}
	

}
