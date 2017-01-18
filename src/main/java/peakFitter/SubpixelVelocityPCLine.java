package peakFitter;

import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import com.sun.tools.javac.util.Pair;

import LineModels.GaussianLineds;
import LineModels.GaussianLinedsHF;
import LineModels.GaussianLinefixedds;
import LineModels.GaussianSplinesecorder;
import LineModels.Gaussiansplinesecfixedds;
import LineModels.MTFitFunction;
import LineModels.UseLineModel.UserChoiceModel;
import graphconstructs.Trackproperties;
import labeledObjects.CommonOutputHF;
import labeledObjects.Indexedlength;
import lineFinder.LinefinderHF;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Point;
import net.imglib2.PointSampleList;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import peakFitter.GaussianMaskFitMSER.EndfitMSER;
import preProcessing.GetLocalmaxmin;

public class SubpixelVelocityPCLine extends BenchmarkAlgorithm
		implements OutputAlgorithm<Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>>> {

	private static final String BASE_ERROR_MSG = "[SubpixelVelocity] ";
	private final RandomAccessibleInterval<FloatType> source;
	private final ArrayList<CommonOutputHF> imgs;
	private final ArrayList<Indexedlength> PrevFrameparamstart;
	private final ArrayList<Indexedlength> PrevFrameparamend;
	private final int ndims;
	private final int framenumber;
	private ArrayList<Indexedlength> final_paramliststart;
	private ArrayList<Indexedlength> final_paramlistend;
	private ArrayList<Trackproperties> startinframe;
	private ArrayList<Trackproperties> endinframe;
	private final double[] psf;

	// LM solver iteration params
	public int maxiter = 200;
	public double lambda = 1e-4;
	public double termepsilon = 1e-2;
	// Mask fits iteration param
	public int iterations = 300;
	public double cutoffdistance = 15;
	public boolean halfgaussian = false;
	public double Intensityratio = 0.5;
	private final UserChoiceModel model;

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

	public SubpixelVelocityPCLine(final RandomAccessibleInterval<FloatType> source, final LinefinderHF linefinder,
			final ArrayList<Indexedlength> PrevFrameparamstart, final ArrayList<Indexedlength> PrevFrameparamend,
			final double[] psf, final int framenumber, final UserChoiceModel model) {

		linefinder.checkInput();
		linefinder.process();
		imgs = linefinder.getResult();
		this.source = source;
		this.PrevFrameparamstart = PrevFrameparamstart;
		this.PrevFrameparamend = PrevFrameparamend;
		this.psf = psf;
		this.framenumber = framenumber;
		this.ndims = source.numDimensions();
		this.model = model;
		assert (PrevFrameparamend.size() == PrevFrameparamstart.size());
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

	public enum StartorEnd {

		Start, End

	}

	

	@Override
	public boolean process() {

		final_paramliststart = new ArrayList<Indexedlength>();
		final_paramlistend = new ArrayList<Indexedlength>();

		startinframe = new ArrayList<Trackproperties>();
		endinframe = new ArrayList<Trackproperties>();

		final int oldframenumber = PrevFrameparamstart.get(PrevFrameparamstart.size() - 1).framenumber;
		final int framediff = framenumber - oldframenumber;

		for (int index = 0; index < PrevFrameparamstart.size(); ++index) {

			final double originalslope = PrevFrameparamstart.get(index).originalslope;

			final double originalintercept = PrevFrameparamstart.get(index).originalintercept;

			final Point linepoint = new Point(ndims);
			linepoint.setPosition(new long[] { (long) PrevFrameparamstart.get(index).currentpos[0],
					(long) PrevFrameparamstart.get(index).currentpos[1] });
			final Point fixedstartpoint = new Point(ndims);
			fixedstartpoint.setPosition(new long[] { (long) PrevFrameparamstart.get(index).fixedpos[0],
					(long) PrevFrameparamstart.get(index).fixedpos[1] });

			int labelstart = Getlabel(fixedstartpoint, originalslope, originalintercept);
			Indexedlength paramnextframestart;

			if (labelstart != Integer.MIN_VALUE)

				paramnextframestart = Getfinaltrackparam(PrevFrameparamstart.get(index), labelstart, psf, framenumber,
						StartorEnd.Start);
			else
				paramnextframestart = PrevFrameparamstart.get(index);
			if (paramnextframestart == null)
				paramnextframestart = PrevFrameparamstart.get(index);

			final_paramliststart.add(paramnextframestart);

			final double[] oldstartpoint = PrevFrameparamstart.get(index).currentpos;

			final double[] newstartpoint = paramnextframestart.currentpos;

			final double newstartslope = paramnextframestart.slope;
			final double newstartintercept = paramnextframestart.intercept;

			final double[] directionstart = { (newstartpoint[0] - oldstartpoint[0]) / framediff,
					(newstartpoint[1] - oldstartpoint[1]) / framediff };

			final Trackproperties startedge = new Trackproperties(framenumber,labelstart, oldstartpoint, newstartpoint,
					newstartslope, newstartintercept, originalslope, originalintercept,
					PrevFrameparamstart.get(index).seedLabel, PrevFrameparamstart.get(index).fixedpos, PrevFrameparamstart.get(index).originalds);

			startinframe.add(startedge);

		}
		for (int index = 0; index < PrevFrameparamend.size(); ++index) {

			Point secondlinepoint = new Point(ndims);
			secondlinepoint.setPosition(new long[] { (long) PrevFrameparamend.get(index).currentpos[0],
					(long) PrevFrameparamend.get(index).currentpos[1] });
			Point fixedendpoint = new Point(ndims);
			fixedendpoint.setPosition(new long[] { (long) PrevFrameparamend.get(index).fixedpos[0],
					(long) PrevFrameparamend.get(index).fixedpos[1] });

			final double originalslopeend = PrevFrameparamend.get(index).originalslope;

			final double originalinterceptend = PrevFrameparamend.get(index).originalintercept;
			int labelend = Getlabel(fixedendpoint, originalslopeend, originalinterceptend);
			Indexedlength paramnextframeend;

			if (labelend != Integer.MIN_VALUE)
				paramnextframeend = Getfinaltrackparam(PrevFrameparamend.get(index), labelend, psf, framenumber,
						StartorEnd.End);
			else
				paramnextframeend = PrevFrameparamend.get(index);
			if (paramnextframeend == null)
				paramnextframeend = PrevFrameparamend.get(index);

			final_paramlistend.add(paramnextframeend);

			final double[] oldendpoint = PrevFrameparamend.get(index).currentpos;

			double[] newendpoint = paramnextframeend.currentpos;

			final double newendslope = paramnextframeend.slope;
			final double newendintercept = paramnextframeend.intercept;

			final double[] directionend = { (newendpoint[0] - oldendpoint[0]) / framediff,
					(newendpoint[1] - oldendpoint[1]) / framediff };

			final Trackproperties endedge = new Trackproperties(framenumber, labelend, oldendpoint, newendpoint, newendslope,
					newendintercept, originalslopeend, originalinterceptend, PrevFrameparamend.get(index).seedLabel,
					PrevFrameparamend.get(index).fixedpos, PrevFrameparamend.get(index).originalds);

			endinframe.add(endedge);

		}

		return true;
	}

	@Override
	public Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> getResult() {

		Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> listpair = new Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>>(
				final_paramliststart, final_paramlistend);

		return listpair;
	}

	public ArrayList<Trackproperties> getstartStateVectors() {
		return startinframe;
	}

	public ArrayList<Trackproperties> getendStateVectors() {
		return endinframe;
	}

	private final double[] MakerepeatedLineguess(Indexedlength iniparam, int label) {
		long[] newposition = new long[ndims];
		double[] minVal = { Double.MAX_VALUE, Double.MAX_VALUE };
		double[] maxVal = { -Double.MIN_VALUE, -Double.MIN_VALUE };

		RandomAccessibleInterval<FloatType> currentimg = imgs.get(label).Roi;

		FinalInterval interval = imgs.get(label).interval;

		currentimg = Views.interval(currentimg, interval);

		final Cursor<FloatType> outcursor = Views.iterable(currentimg).localizingCursor();

		final double maxintensityline = GetLocalmaxmin.computeMaxIntensity(currentimg);

		if (model == UserChoiceModel.Line) {

			double slope = iniparam.originalslope;
			double intercept = iniparam.originalintercept;
			while (outcursor.hasNext()) {

				outcursor.fwd();

				outcursor.localize(newposition);

				// To get the min and max co-rodinates along the line so we
				// have starting points to
				// move on the line smoothly
				int pointonline = (int) (outcursor.getIntPosition(1) - slope * outcursor.getIntPosition(0) 
       					- intercept) ;
       	      
       	      
				if (outcursor.getDoublePosition(0) <= minVal[0]
						&& outcursor.get().get() / maxintensityline > Intensityratio) {
					minVal[0] = outcursor.getDoublePosition(0);
					minVal[1] = outcursor.getDoublePosition(1);
				}

				if (outcursor.getDoublePosition(0) >= maxVal[0]
						&& outcursor.get().get() / maxintensityline > Intensityratio) {
					maxVal[0] = outcursor.getDoublePosition(0);
					maxVal[1] = outcursor.getDoublePosition(1);
				}
       	       }
			
			final double[] MinandMax = new double[2 * ndims + 3];

			for (int d = 0; d < ndims; ++d) {

				MinandMax[d] = minVal[d];
				MinandMax[d + ndims] = maxVal[d];
			}

			MinandMax[2 * ndims] = 0.5 * Math.min(psf[0], psf[1]);
			MinandMax[2 * ndims + 1] = maxintensityline;
			MinandMax[2 * ndims + 2] = 0;
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
		

		if (model == UserChoiceModel.Linefixedds) {

			while (outcursor.hasNext()) {

				outcursor.fwd();

				outcursor.localize(newposition);

				// To get the min and max co-rodinates along the line so we
				// have starting points to
				// move on the line smoothly

				if (outcursor.getDoublePosition(0) <= minVal[0]
						&& outcursor.get().get() / maxintensityline > Intensityratio) {
					minVal[0] = outcursor.getDoublePosition(0);
					minVal[1] = outcursor.getDoublePosition(1);
				}

				if (outcursor.getDoublePosition(0) >= maxVal[0]
						&& outcursor.get().get() / maxintensityline > Intensityratio) {
					maxVal[0] = outcursor.getDoublePosition(0);
					maxVal[1] = outcursor.getDoublePosition(1);
				}

			}
			final double[] MinandMax = new double[2 * ndims + 2];

			for (int d = 0; d < ndims; ++d) {

				MinandMax[d] = minVal[d];
				MinandMax[d + ndims] = maxVal[d];
			}

			MinandMax[2 * ndims] =maxintensityline;
			MinandMax[2 * ndims + 1] = 0;
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
		if (model == UserChoiceModel.LineHF) {

			
			while (outcursor.hasNext()) {

				outcursor.fwd();

				outcursor.localize(newposition);
				

				// To get the min and max co-rodinates along the line so we
				// have starting points to
				// move on the line smoothly
				if (outcursor.getDoublePosition(0) <= minVal[0]
						&& outcursor.get().get() / maxintensityline > Intensityratio) {
					minVal[0] = outcursor.getDoublePosition(0);
					minVal[1] = outcursor.getDoublePosition(1);
				}

				if (outcursor.getDoublePosition(0) >= maxVal[0]
						&& outcursor.get().get() / maxintensityline > Intensityratio) {
					maxVal[0] = outcursor.getDoublePosition(0);
					maxVal[1] = outcursor.getDoublePosition(1);
				}
			}
			final double[] MinandMax = new double[2 * ndims + 3];

			for (int d = 0; d < ndims; ++d) {

				MinandMax[d] = minVal[d];
				MinandMax[d + ndims] = maxVal[d];
			}

			MinandMax[2 * ndims] = 0.5 * Math.min(psf[0], psf[1]);
			MinandMax[2 * ndims + 1] = maxintensityline;
			MinandMax[2 * ndims + 2] = 0;
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

		if (model == UserChoiceModel.Splineordersecfixedds) {
		


			while (outcursor.hasNext()) {

				outcursor.fwd();

				// To get the min and max co-rodinates along the line so we
				// have starting points to
				// move on the line smoothly

				outcursor.localize(newposition);

				if (outcursor.getDoublePosition(0) <= minVal[0]
						&& outcursor.get().get() / maxintensityline > Intensityratio) {
					minVal[0] = outcursor.getDoublePosition(0);
					minVal[1] = outcursor.getDoublePosition(1);
				}

				if (outcursor.getDoublePosition(0) >= maxVal[0]
						&& outcursor.get().get() / maxintensityline > Intensityratio) {
					maxVal[0] = outcursor.getDoublePosition(0);
					maxVal[1] = outcursor.getDoublePosition(1);
				}
			}

			final double[] MinandMax = new double[2 * ndims + 3];

			for (int d = 0; d < ndims; ++d) {

				MinandMax[d] = minVal[d];
				MinandMax[d + ndims] = maxVal[d];
			}

			MinandMax[2 * ndims + 1] = maxintensityline;
			MinandMax[2 * ndims + 2] = 0;
			MinandMax[2 * ndims] = 0;
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

		if (model == UserChoiceModel.Splineordersec) {
			

			while (outcursor.hasNext()) {

				outcursor.fwd();

				// To get the min and max co-rodinates along the line so we
				// have starting points to
				// move on the line smoothly

				outcursor.localize(newposition);

				if (outcursor.getDoublePosition(0) <= minVal[0]
						&& outcursor.get().get() / maxintensityline > Intensityratio) {
					minVal[0] = outcursor.getDoublePosition(0);
					minVal[1] = outcursor.getDoublePosition(1);
				}

				if (outcursor.getDoublePosition(0) >= maxVal[0]
						&& outcursor.get().get() / maxintensityline > Intensityratio) {
					maxVal[0] = outcursor.getDoublePosition(0);
					maxVal[1] = outcursor.getDoublePosition(1);
				}
			}

			final double[] MinandMax = new double[2 * ndims + 4];

			for (int d = 0; d < ndims; ++d) {

				MinandMax[d] = minVal[d];
				MinandMax[d + ndims] = maxVal[d];
			}

			MinandMax[2 * ndims + 2] = maxintensityline;
			MinandMax[2 * ndims + 3] = 0;
			MinandMax[2 * ndims + 1] = 0;
			MinandMax[2 * ndims] =  0.5 *  Math.min(psf[0], psf[1]);
			
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

		else
			return null;

	}

	public Indexedlength Getfinaltrackparam(final Indexedlength iniparam, final int label, final double[] psf,
			final int rate, final StartorEnd startorend)  {

		final double[] LMparam = MakerepeatedLineguess(iniparam, label);
		if (LMparam == null)
			return iniparam;

		else {

		
			final double[] inipos = iniparam.currentpos;
		
			RandomAccessibleInterval<FloatType> currentimg = imgs.get(label).Actualroi;

			FinalInterval interval = imgs.get(label).interval;

			currentimg = Views.interval(currentimg, interval);

			final double[] fixed_param = new double[ndims + 3];

			for (int d = 0; d < ndims; ++d) {

				fixed_param[d] = 1.0 / Math.pow(psf[d], 2);

			}
			fixed_param[ndims] = iniparam.originalslope;
			fixed_param[ndims + 1] = iniparam.originalintercept;
			fixed_param[ndims + 2] =  0.5 * Math.min(psf[0], psf[1]);

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

			System.out.println("Label: " + label + " " + "Initial guess: " + " StartX: " + LMparam[0] + " StartY: "
					+ LMparam[1] + " EndX: " + LMparam[2] + " EndY: " + LMparam[3]);

			final double[] safeparam = LMparam.clone();
			MTFitFunction UserChoiceFunction = null;
			if (model == UserChoiceModel.Line) {
				fixed_param[ndims] = iniparam.slope;
				fixed_param[ndims + 1] = iniparam.intercept;
				UserChoiceFunction = new GaussianLineds();

			}
			
			if (model == UserChoiceModel.Linefixedds) {
				
				UserChoiceFunction = new GaussianLinefixedds();

			}

			if (model == UserChoiceModel.LineHF) {

				UserChoiceFunction = new GaussianLinedsHF();
			}
			if (model == UserChoiceModel.Splineordersecfixedds) {
				fixed_param[ndims] = iniparam.slope;
				fixed_param[ndims + 1] = iniparam.intercept;
				UserChoiceFunction = new Gaussiansplinesecfixedds();

			}

			if (model == UserChoiceModel.Splineordersec) {
				fixed_param[ndims] = iniparam.slope;
				fixed_param[ndims + 1] = iniparam.intercept;
				
				UserChoiceFunction = new GaussianSplinesecorder();

			}
			// LM solver part

			try {
				LevenbergMarquardtSolverLine.solve(X, LMparam, fixed_param, I, UserChoiceFunction, lambda, termepsilon,
						maxiter);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			for (int j = 0; j < LMparam.length; j++) {
				if (Double.isNaN(LMparam[j]))
					LMparam[j] = safeparam[j];
			}

			double[] startpos = new double[ndims];
			double[] endpos = new double[ndims];

			for (int d = 0; d < ndims; ++d) {
				startpos[d] = LMparam[d];
				endpos[d] = LMparam[d + ndims];

			}

			final double maxintensityline = GetLocalmaxmin.computeMaxIntensity(currentimg);

			System.out.println("Frame: " + framenumber);

			final int seedLabel = iniparam.seedLabel;

			if (model == UserChoiceModel.Line) {
				if (startorend == StartorEnd.Start) {
					double ds = LMparam[2 * ndims];
					
					final double newslope = (startpos[1] - endpos[1]) / (startpos[0] - endpos[0]);
					final double newintercept = startpos[1] - newslope * startpos[0];
					double dx = ds / Math.sqrt(1 + (newslope ) * (newslope));
					double dy = (newslope) * dx;
					double[] dxvector = { dx, dy };
					double sigmas = 0;
					 
					for (int d  = 0; d < ndims; ++d){
						
						sigmas+=psf[d] * psf[d];
					}
					sigmas = Math.min(psf[0], psf[1]);
					final int numgaussians = (int) Math.min(Math.round(sigmas /  ds), 6);
					double[] startfit = new double[ndims];
					try {
						startfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, startpos.clone(), psf, numgaussians,
								iterations, dxvector, newslope, newintercept, maxintensityline, halfgaussian,
								EndfitMSER.StartfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(startfit, startpos);

					
						
					
				
					if (Math.abs(startpos[0] - startfit[0]) >= cutoffdistance || Math.abs(startpos[1] - startfit[1]) >= cutoffdistance){
						System.out.println("Mask fits fail, returning LM solver results!");
						for (int d = 0; d < ndims; ++d) {
						startfit[d] = startpos[d];
						}
					}
					for (int d = 0; d < ndims; ++d) {
						if (Double.isNaN(startfit[d])) {
							System.out.println("Mask fits fail, returning LM solver results!");
							startfit[d] = startpos[d];

						}
					}
					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, LMparam[2 * ndims],
							LMparam[2 * ndims + 1], LMparam[2 * ndims + 2], startfit, iniparam.fixedpos, newslope,
							newintercept, iniparam.originalslope, iniparam.originalintercept, iniparam.originalds);
					System.out.println("New X: " + startfit[0] + " New Y: " + startfit[1]);
					System.out.println("Number of Gaussians used: " + numgaussians+ " ds: " + ds);
					
						
					return PointofInterest;
				} else {
					double ds = LMparam[2 * ndims];
					final double newslope = (endpos[1] - inipos[1]) / (endpos[0] - inipos[0]);
					final double newintercept = endpos[1] - newslope * endpos[0];
					double dx = ds / Math.sqrt(1 + (newslope) * (newslope));
					double dy = (newslope) * dx;
					double[] dxvector = { dx, dy };
					double sigmas = 0;
					 
					for (int d  = 0; d < ndims; ++d){
						
						sigmas+=psf[d] * psf[d];
					}
					sigmas = Math.min(psf[0], psf[1]);
					final int numgaussians = (int) Math.min(Math.round(sigmas /  ds), 6);
					double[] endfit = new double[ndims];
					try {
						endfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, endpos.clone(), psf, numgaussians, iterations,
								dxvector, newslope, newintercept, maxintensityline, halfgaussian, EndfitMSER.EndfitMSER,
								label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(endfit, endpos);
				
						
					

					for (int d = 0; d < ndims; ++d) {
						if (Double.isNaN(endfit[d])) {
							System.out.println("Mask fits fail, returning LM solver results!");
							endfit[d] = endpos[d];

						}
					
					}
				
					if (Math.abs(endpos[0] - endfit[0]) >= cutoffdistance || Math.abs(endpos[1] - endfit[1]) >= cutoffdistance){
						System.out.println("Mask fits fail, returning LM solver results!");
						for (int d = 0; d < ndims; ++d) {
						endfit[d] = endpos[d];
						}
					}
					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, LMparam[2 * ndims],
							LMparam[2 * ndims + 1], LMparam[2 * ndims + 2], endfit, iniparam.fixedpos, newslope,
							newintercept, iniparam.originalslope, iniparam.originalintercept, iniparam.originalds);
					System.out.println("New X: " + endfit[0] + " New Y: " + endfit[1]);
					System.out.println("Number of Gaussians used: " + numgaussians + "ds: " + ds);
					
					return PointofInterest;

				}
			}

			if (model == UserChoiceModel.Linefixedds) {
				if (startorend == StartorEnd.Start) {
					double ds = fixed_param[ndims + 2];
					
					final double newslope = (startpos[1] - inipos[1]) / (startpos[0] - inipos[0]);
					final double newintercept = startpos[1] - newslope * startpos[0];
					double dx = ds / Math.sqrt(1 + (newslope ) * (newslope));
					double dy = (newslope) * dx;
					double[] dxvector = { dx, dy };
					double sigmas = 0;
					 
					for (int d  = 0; d < ndims; ++d){
						
						sigmas+=psf[d] * psf[d];
					}
					sigmas = Math.min(psf[0], psf[1]);
					final int numgaussians = (int) Math.min(Math.round(sigmas /  ds), 6);
					double[] startfit = new double[ndims];
					try {
						startfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, startpos.clone(), psf, numgaussians,
								iterations, dxvector, newslope, newintercept, maxintensityline, halfgaussian,
								EndfitMSER.StartfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(startfit, startpos);

					
				
					if (Math.abs(startpos[0] - startfit[0]) >= cutoffdistance || Math.abs(startpos[1] - startfit[1]) >= cutoffdistance){
						System.out.println("Mask fits fail, returning LM solver results!");
						for (int d = 0; d < ndims; ++d) {
						startfit[d] = startpos[d];
						}
					}
					for (int d = 0; d < ndims; ++d) {
						if (Double.isNaN(startfit[d])) {
							System.out.println("Mask fits fail, returning LM solver results!");
							startfit[d] = startpos[d];

						}
					}
					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, ds,
							LMparam[2 * ndims], LMparam[2 * ndims + 1], startfit, iniparam.fixedpos, newslope,
							newintercept, iniparam.originalslope, iniparam.originalintercept, iniparam.originalds);
					System.out.println("New X: " + startfit[0] + " New Y: " + startfit[1]);
					System.out.println("Number of Gaussians used: " + numgaussians);
					
						
					return PointofInterest;
				} else {
					double ds =fixed_param[ndims + 2];
					final double newslope = (endpos[1] - inipos[1]) / (endpos[0] - inipos[0]);
					final double newintercept = endpos[1] - newslope * endpos[0];
					double dx = ds / Math.sqrt(1 + (newslope) * (newslope));
					double dy = (newslope) * dx;
					double[] dxvector = { dx, dy };
					double sigmas = 0;
					 
					for (int d  = 0; d < ndims; ++d){
						
						sigmas+=psf[d] * psf[d];
					}
					sigmas = Math.min(psf[0], psf[1]);
					final int numgaussians = (int) Math.min(Math.round(sigmas /  ds), 6);
					double[] endfit = new double[ndims];
					try {
						endfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, endpos.clone(), psf, numgaussians, iterations,
								dxvector, newslope, newintercept, maxintensityline, halfgaussian, EndfitMSER.EndfitMSER,
								label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(endfit, endpos);
					
					
						
					

					if (Math.abs(endpos[0] - endfit[0]) >= cutoffdistance || Math.abs(endpos[1] - endfit[1]) >= cutoffdistance){
						System.out.println("Mask fits fail, returning LM solver results!");
						for (int d = 0; d < ndims; ++d) {
						endfit[d] = endpos[d];
						}
					}
					for (int d = 0; d < ndims; ++d) {
						if (Double.isNaN(endfit[d])) {
							System.out.println("Mask fits fail, returning LM solver results!");
							endfit[d] = endpos[d];

						}
					}
					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, ds,
							LMparam[2 * ndims], LMparam[2 * ndims + 1], endfit, iniparam.fixedpos, newslope,
							newintercept, iniparam.originalslope, iniparam.originalintercept, iniparam.originalds);
					System.out.println("New X: " + endfit[0] + " New Y: " + endfit[1]);
					System.out.println("Number of Gaussians used: " + numgaussians);
					
					return PointofInterest;

				}
			}

			else if (model == UserChoiceModel.LineHF) {
				if (startorend == StartorEnd.Start) {

					final double ds = LMparam[2 * ndims];
					final double lineIntensity = LMparam[2 * ndims + 1];
					final double background = LMparam[2 * ndims + 2];
					double newslope = iniparam.originalslope;
					double newintercept = iniparam.originalintercept;
					double dx = ds / Math.sqrt(1 + newslope * newslope);
					double dy = newslope * dx;
					double[] dxvector = { dx, dy };
					double[] startfit = new double[ndims];
					double sigmas = 0;
					 
					for (int d  = 0; d < ndims; ++d){
						
						sigmas+=psf[d] * psf[d];
					}
					sigmas = Math.min(psf[0], psf[1]);
					final int numgaussians = (int) Math.min(Math.round(sigmas /  ds), 6);
					try {
						startfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, startpos.clone(), psf, numgaussians,
								iterations, dxvector, newslope, newintercept, maxintensityline, halfgaussian,
								EndfitMSER.StartfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(startfit, startpos);
					
					

				
					
					if (Math.abs(startpos[0] - startfit[0]) >= cutoffdistance || Math.abs(startpos[1] - startfit[1]) >= cutoffdistance){
						System.out.println("Mask fits fail, returning LM solver results!");
						for (int d = 0; d < ndims; ++d) {
						startfit[d] = startpos[d];
						}
					}
					for (int d = 0; d < ndims; ++d) {
						if (Double.isNaN(startfit[d])) {
							System.out.println("Mask fits fail, returning LM solver results!");
							startfit[d] = startpos[d];

						}
					}
					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, ds, lineIntensity,
							background, startfit, iniparam.fixedpos, newslope, newintercept, iniparam.originalslope,
							iniparam.originalintercept, iniparam.originalds);
					System.out.println("New X: " + startfit[0] + " New Y: " + startfit[1]);
					System.out.println("Number of Gaussians used: " + numgaussians);
					return PointofInterest;
				} else {

					final double ds = LMparam[2 * ndims];
					final double lineIntensity = LMparam[2 * ndims + 1];
					final double background = LMparam[2 * ndims + 2];
					double newslope = iniparam.originalslope;
					double newintercept = iniparam.originalintercept;
					double dx = ds / Math.sqrt(1 + newslope * newslope);
					double dy = newslope * dx;
					double[] dxvector = { dx, dy };
					double sigmas = 0;
					 
					for (int d  = 0; d < ndims; ++d){
						
						sigmas+=psf[d] * psf[d];
					}
					sigmas = Math.min(psf[0], psf[1]);
					final int numgaussians = (int) Math.min(Math.round(sigmas /  ds), 6);
					double[] endfit = new double[ndims];
					try {
						endfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, endpos.clone(), psf, numgaussians, iterations,
								dxvector, newslope, newintercept, maxintensityline, halfgaussian, EndfitMSER.EndfitMSER,
								label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(endfit, endpos);
					
						
				
					
					
					for (int d = 0; d < ndims; ++d) {
						if (Double.isNaN(endfit[d])) {
							System.out.println("Mask fits fail, returning LM solver results!");
							endfit[d] = endpos[d];

						}
					}
					
					if (Math.abs(endpos[0] - endfit[0]) >= cutoffdistance || Math.abs(endpos[1] - endfit[1]) >= cutoffdistance){
						System.out.println("Mask fits fail, returning LM solver results!");
						for (int d = 0; d < ndims; ++d) {
						endfit[d] = endpos[d];
						}
					}
					
					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, ds, lineIntensity,
							background, endfit, iniparam.fixedpos, newslope, newintercept, iniparam.originalslope,
							iniparam.originalintercept, iniparam.originalds);
					System.out.println("New X: " + endfit[0] + " New Y: " + endfit[1]);
					System.out.println("Number of Gaussians used: " + numgaussians);
					return PointofInterest;

				}
			}

			else if (model == UserChoiceModel.Splineordersecfixedds) {
				if (startorend == StartorEnd.Start) {
					final double Curvature = LMparam[2 * ndims];
					final double currentslope = iniparam.originalslope;
					final double currentintercept = iniparam.originalintercept;
					final double ds = fixed_param[ndims + 2];
					final double lineIntensity = LMparam[2 * ndims + 1];
					final double background = LMparam[2 * ndims + 2];
					double[] startfit = new double[ndims];

					final double newslope = (startpos[1] - endpos[1])/ (startpos[0] - endpos[0]) - Curvature * (startpos[0] + endpos[0]);

					//+ 2 * Curvature * startpos[0]
					double dx = ds / Math.sqrt(1 + (newslope + 2 * Curvature * startpos[0]) * (newslope + 2 * Curvature * startpos[0]) );
					double dy = (newslope + 2 * Curvature * startpos[0]) * dx;
					double[] dxvector = { dx, dy };
					double sigmas = 0;
					 
					for (int d  = 0; d < ndims; ++d){
						
						sigmas+=psf[d] * psf[d];
					}

					sigmas = Math.min(psf[0], psf[1]);
					final int numgaussians = (int) Math.min(Math.round(sigmas /  ds), 6);

					try {
						startfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, startpos.clone(), psf, numgaussians,
								iterations, dxvector, currentslope, currentintercept, maxintensityline, halfgaussian,
								EndfitMSER.StartfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

				
					

				
					
					if (Math.abs(startpos[0] - startfit[0]) >= cutoffdistance || Math.abs(startpos[1] - startfit[1]) >= cutoffdistance){
						System.out.println("Mask fits fail, returning LM solver results!");
						for (int d = 0; d < ndims; ++d) {
						startfit[d] = startpos[d];
						}
					}
					for (int d = 0; d < ndims; ++d) {
						if (Double.isNaN(startfit[d])) {
							System.out.println("Mask fits fail, returning LM solver results!");
							startfit[d] = startpos[d];

						}
					}
					System.out.println("Curvature: " + Curvature);

					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, ds, lineIntensity,
							background, startfit, iniparam.fixedpos, currentslope, currentintercept,
							iniparam.originalslope, iniparam.originalintercept, Curvature, 0, iniparam.originalds);
					System.out.println("New X: " + startfit[0] + " New Y: " + startfit[1]+ " " + "New Xlm: " + startpos[0]
							+ " New Ylm: " + endpos[1]);
					System.out.println("Number of Gaussians used: " + (numgaussians ) + " " + ds );
					
					
						
					return PointofInterest;
				} else {

					final double Curvature = LMparam[2 * ndims];

					
					final double currentslope = iniparam.originalslope;
					final double currentintercept = iniparam.originalintercept;
					final double ds = fixed_param[ndims + 2];
					final double lineIntensity = LMparam[2 * ndims + 1];
					final double background = LMparam[2 * ndims + 2];
					System.out.println("Curvature: " + Curvature);
					double[] endfit = new double[ndims];
					// + 2 * Curvature * endpos[0]
					final double newslope = (endpos[1] - startpos[1])/ (endpos[0] - startpos[0]) - Curvature * (endpos[0] + startpos[0]);

					//+ 2 * Curvature * startpos[0]
					double dx = ds / Math.sqrt(1 + (newslope + 2 * Curvature * endpos[0]) * (newslope + 2 * Curvature * endpos[0]) );
					double dy = (newslope+ 2 * Curvature * endpos[0]) * dx;
					double[] dxvector = { dx, dy };
					double sigmas = 0;
 
					for (int d  = 0; d < ndims; ++d){
						
						sigmas+=psf[d] * psf[d];
					}
					sigmas = Math.min(psf[0], psf[1]);
					final int numgaussians = (int) Math.min(Math.round(sigmas /  ds), 6);
				
					try {
						endfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, endpos.clone(), psf, numgaussians, iterations,
								dxvector, currentslope, currentintercept, maxintensityline, halfgaussian,
								EndfitMSER.EndfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(endfit, endpos);
				
					
					
					
					if (Math.abs(endpos[0] - endfit[0]) >= cutoffdistance || Math.abs(endpos[1] - endfit[1]) >= cutoffdistance){
						System.out.println("Mask fits fail, returning LM solver results!");
						for (int d = 0; d < ndims; ++d) {
						endfit[d] = endpos[d];
						}
					}
					for (int d = 0; d < ndims; ++d) {
						if (Double.isNaN(endfit[d])) {
							System.out.println("Mask fits fail, returning LM solver results!");
							endfit[d] = endpos[d];

						}
					}
					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, ds, lineIntensity,
							background, endfit, iniparam.fixedpos, currentslope, currentintercept,
							iniparam.originalslope, iniparam.originalintercept, Curvature, 0, iniparam.originalds);
					System.out.println("New X: " + endfit[0] + " New Y: " + endfit[1] + " "+ "New Xlm: " + endpos[0]
							+ " New Ylm: " + endpos[1]);
					System.out.println("Number of Gaussians used: " + (numgaussians ) + " " + ds);
				
					
					return PointofInterest;

				}
			}
			else if (model == UserChoiceModel.Splineordersec) {
				if (startorend == StartorEnd.Start) {
					final double Curvature = LMparam[2 * ndims + 1];
										
					final double currentslope = iniparam.slope;

					final double currentintercept = iniparam.originalintercept;

					final double ds = Math.abs(LMparam[2 * ndims]);
					final double lineIntensity = LMparam[2 * ndims + 2];
					final double background = LMparam[2 * ndims + 3];
					double[] startfit = new double[ndims];
					final double newslope = (startpos[1] - endpos[1])/ (startpos[0] - endpos[0]) - Curvature * (startpos[0] + endpos[0]);

					//+ 2 * Curvature * startpos[0]
					double dx = ds / Math.sqrt(1 + (newslope + 2 * Curvature * startpos[0]) * (newslope + 2 * Curvature * startpos[0]) );
					double dy = (newslope + 2 * Curvature * startpos[0]) * dx;
					double[] dxvector = { dx, dy };
					double sigmas = 0;
					 
					for (int d  = 0; d < ndims; ++d){
						
						sigmas+=psf[d] * psf[d];
					}
				sigmas = Math.min(psf[0], psf[1]);
				final int numgaussians = (int) Math.max(Math.round(sigmas /  ds), 2);

					try {
						startfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, startpos.clone(), psf, numgaussians,
								iterations, dxvector, currentslope, currentintercept, maxintensityline, halfgaussian,
								EndfitMSER.StartfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(startfit, startpos);
					
					
					
					for (int d = 0; d < ndims; ++d) {
					//	if (Double.isNaN(startfit[d])) {
							System.out.println("Mask fits fail, returning LM solver results!");
							startfit[d] = startpos[d];

					//	}
					}
					
					if (Math.abs(startpos[0] - startfit[0]) >= cutoffdistance || Math.abs(startpos[1] - startfit[1]) >= cutoffdistance){
						System.out.println("Mask fits fail, returning LM solver results!");
						for (int d = 0; d < ndims; ++d) {
						startfit[d] = startpos[d];
						}
					}
				
					System.out.println("Curvature: " + Curvature);

					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, ds, lineIntensity,
							background, startfit, iniparam.fixedpos, newslope, currentintercept,
							iniparam.originalslope, iniparam.originalintercept, Curvature, 0, iniparam.originalds);
					System.out.println("New X: " + startfit[0] + " New Y: " + startfit[1]+ " " + "New Xlm: " + startpos[0]
							+ " New Ylm: " + endpos[1]);
					System.out.println("Number of Gaussians used: " + (numgaussians ) + " " + ds );
				
					
						
					return PointofInterest;
				} else {

					final double Curvature = LMparam[2 * ndims + 1];
             
					
					final double currentslope = iniparam.slope;
					final double currentintercept = iniparam.originalintercept;
					final double ds = Math.abs(LMparam[2 * ndims]);
					final double lineIntensity = LMparam[2 * ndims + 2];
					final double background = LMparam[2 * ndims + 3];
					System.out.println("Curvature: " + Curvature);
					final double newslope = (endpos[1] - startpos[1])/ (endpos[0] - startpos[0]) - Curvature * (endpos[0] + startpos[0]);
					double[] endfit = new double[ndims];
					// + 2 * Curvature * endpos[0]
					double dx = ds / Math.sqrt(1 + (newslope + 2 * Curvature * endpos[0]) * (newslope + 2 * Curvature * endpos[0]));
					double dy = (newslope + 2 * Curvature * endpos[0]) * dx;
					double[] dxvector = { dx, dy };
					double sigmas = 0;
 
					for (int d  = 0; d < ndims; ++d){
						
						sigmas+=psf[d] * psf[d];
					}
					sigmas = Math.min(psf[0], psf[1]);
					final int numgaussians = (int) Math.max(Math.round(sigmas /  ds), 2);
				
					try {
						endfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, endpos.clone(), psf, numgaussians, iterations,
								dxvector, currentslope, currentintercept, maxintensityline, halfgaussian,
								EndfitMSER.EndfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(endfit, endpos);
					
				
				

					for (int d = 0; d < ndims; ++d) {
				//		if (Double.isNaN(endfit[d])) {
					//		System.out.println("Mask fits fail, returning LM solver results!");
							endfit[d] = endpos[d];

				//		}
						
						
					}
					
				
					
					if (Math.abs(endpos[0] - endfit[0]) >= cutoffdistance || Math.abs(endpos[1] - endfit[1]) >= cutoffdistance){
						System.out.println("Mask fits fail, returning LM solver results!");
						for (int d = 0; d < ndims; ++d) {
						endfit[d] = endpos[d];
						}
					}

					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, ds, lineIntensity,
							background, endfit, iniparam.fixedpos, newslope, currentintercept,
							iniparam.originalslope, iniparam.originalintercept, Curvature, 0, iniparam.originalds);
					System.out.println("New X: " + endfit[0] + " New Y: " + endfit[1] + " "+ "New Xlm: " + endpos[0]
							+ " New Ylm: " + endpos[1]);
					System.out.println("Number of Gaussians used: " + (numgaussians ) + " " + ds);
					
					
					
					return PointofInterest;

				}
			}
			else
				return null;

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

			if (localcursor.get().get() > 0){
			Point newpoint = new Point(localcursor);
			datalist.add(newpoint, localcursor.get().copy());
			}
		}

		return datalist;
	}

	public int Getlabel(final Point fixedpoint, final double originalslope, final double originalintercept) {

		
		
		
		
		ArrayList<Integer> currentlabel = new ArrayList<Integer>();

		int finallabel = Integer.MIN_VALUE;
		int pointonline = Integer.MAX_VALUE;
		for (int index = 0; index < imgs.size(); ++index) {

			if (imgs.get(index).intimg!= null){
				
				RandomAccess<IntType> intranac = imgs.get(index).intimg.randomAccess();

				
				intranac.setPosition(fixedpoint);
				finallabel = intranac.get().get();

				return finallabel;
				
			}
			else{
			
			
			RandomAccessibleInterval<FloatType> currentimg = imgs.get(index).Actualroi;
			FinalInterval interval = imgs.get(index).interval;
			currentimg = Views.interval(currentimg, interval);

			
			if (fixedpoint.getIntPosition(0) >= interval.min(0) && fixedpoint.getIntPosition(0) <= interval.max(0)
					&& fixedpoint.getIntPosition(1) >= interval.min(1)
					&& fixedpoint.getIntPosition(1) <= interval.max(1)) {

				finallabel = imgs.get(index).roilabel;
			}
			

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

	public static double Xcorddist(final double Xcordone, final double Xcordtwo) {

		double distance = Math.abs(Xcordone - Xcordtwo);

		return distance;
	}

	public static double dsdist(final double[] cordone, final double[] cordtwo) {

		double distance = Math.pow((cordone[0] - cordtwo[0]), 2) + Math.pow((cordone[1] - cordtwo[1]), 2);

		return distance;
	}

	public static double[] Midpoint(final double[] cordone, final double[] cordtwo) {
		int ndims = cordone.length;
		final double[] midpoint = new double[ndims];

		for (int d = 0; d < ndims; ++d) {

			midpoint[d] = (cordone[d] + cordtwo[d]) / 2;
		}

		return midpoint;

	}
	
	
	private void Testerror(final double[] point, int label, int rate) {

		try {
			FileWriter writer = new FileWriter("../res/error-moving.txt", true);
			if (rate ==1){
			if (label == 0)
				writer.write(rate + " " +(point[0] -383.7869900096266) + " " + (point[1] - 214.6620845470553));
			writer.write("\r\n");
			
			if (label == 1)
				writer.write(rate + " " +(point[0] - 406.5606110921211) + " " + (point[1] - 772.7696058689498) );
			writer.write("\r\n");
			if (label == 4)
				writer.write(rate + " " +(point[0] - 726.4888354281538) + " " + (point[1] - 175.931829613848));
			writer.write("\r\n");
			if (label == 6)
				writer.write(rate + " " +(point[0] - 826.0305494697972) + " " + (point[1] - 748.4597998409396) );
			writer.write("\r\n");
			if (label == 8)
				writer.write(rate + " " +(point[0] - 889.4867734500586) + " " + (point[1] - 398.56288813577913) );
			writer.write("\r\n");
			if (label == 2)
				writer.write(rate + " " +(point[0] - 441.74655759707014) + " " + (point[1] - 242.8911171431671 ) );
			writer.write("\r\n");

			if (label == 3)
				writer.write(rate + " " +(point[0] - 472.3859943567383) + " " + (point[1] - 797.0381138658016 ) );
			writer.write("\r\n");
			if (label == 5)
				writer.write(rate + " " +(point[0] - 798.1629795446817) + " " + (point[1] - 185.14679594793904) );
			writer.write("\r\n");
			
			if (label == 7)
				writer.write(rate + " " +(point[0] - 900.7642251667279) + " " + (point[1] - 749.8423126238637) );
			writer.write("\r\n");
			if (label == 9)
				writer.write(rate + " " +(point[0] - 954.8049347521577) + " " + (point[1] - 379.3048166471907));
			writer.write("\r\n");
			
			}
			
			if (rate == 2){
				if (label == 0)
					writer.write(rate + " " +(point[0] -380.353507802382) + " " + (point[1] - 213.0539454407681));
				writer.write("\r\n");
				
				if (label == 1)
					writer.write(rate + " " +(point[0] - 403.1271288848765) + " " + (point[1] - 771.5547418672202) );
				writer.write("\r\n");
				if (label == 4)
					writer.write(rate + " " +(point[0] - 723.0553532209092) + " " + (point[1] - 175.52440736347455));
				writer.write("\r\n");
				if (label == 6)
					writer.write(rate + " " +(point[0] - 822.5970672625525) + " " + (point[1] - 748.4798964103497) );
				writer.write("\r\n");
				if (label == 7)
					writer.write(rate + " " +(point[0] - 886.053291242814) + " " + (point[1] - 399.66197560921444) );
				writer.write("\r\n");
				if (label == 2)
					writer.write(rate + " " +(point[0] - 449.89687874290297) + " " + (point[1] - 249.07035580540654 ) );
				writer.write("\r\n");

				if (label == 3)
					writer.write(rate + " " +(point[0] - 479.92781431064776) + " " + (point[1] - 801.768490518385 ) );
				writer.write("\r\n");
				if (label == 5)
					writer.write(rate + " " +(point[0] - 802.1351123735927 ) + " " + (point[1] - 186.44647186352123 ) );
				writer.write("\r\n");
				
				if (label == 8)
					writer.write(rate + " " +(point[0] - 912.7640196182127) + " " + (point[1] - 754.4519161093077) );
				writer.write("\r\n");
				if (label == 9)
					writer.write(rate + " " +(point[0] - 967.1860579991517) + " " + (point[1] - 380.05819504153664));
				writer.write("\r\n");
				
				}
			if (rate == 3){
				if (label == 0)
					writer.write(rate + " " +(point[0] - 377.19947344282707) + " " + (point[1] - 211.576691245167));
				writer.write("\r\n");
				
				if (label == 1)
					writer.write(rate + " " +(point[0] - 399.9730945253216) + " " + (point[1] - 770.4387544898699) );
				writer.write("\r\n");
				if (label == 4)
					writer.write(rate + " " +(point[0] - 719.9013188613543) + " " + (point[1] - 175.1501448221357));
				writer.write("\r\n");
				if (label == 6)
					writer.write(rate + " " +(point[0] - 819.4430329029976) + " " + (point[1] - 748.4983573391104) );
				writer.write("\r\n");
				if (label == 7)
					writer.write(rate + " " +(point[0] - 882.8992568832591) + " " + (point[1] - 400.67160939967266) );
				writer.write("\r\n");
				if (label == 2)
					writer.write(rate + " " +(point[0] - 456.23601741188406) + " " + (point[1] - 254.79493727052903 ) );
				writer.write("\r\n");

				if (label == 3)
					writer.write(rate + " " +(point[0] - 485.58417927607985) + " " + (point[1] - 806.0628105156728 ) );
				writer.write("\r\n");
				if (label == 5)
					writer.write(rate + " " +(point[0] - 806.1072452025038 ) + " " + (point[1] - 188.0617045633137 ) );
				writer.write("\r\n");
				
				if (label == 8)
					writer.write(rate + " " +(point[0] - 924.7638140696976) + " " + (point[1] - 761.9414209323095) );
				writer.write("\r\n");
				if (label == 9)
					writer.write(rate + " " +(point[0] - 979.5671812461457) + " " + (point[1] - 383.87741769302727));
				writer.write("\r\n");
				
				}
			if (rate == 4){
				if (label == 0)
					writer.write(rate + " " +(point[0] - 374.4506283277463) + " " + (point[1] - 210.28921542328547 ));
				writer.write("\r\n");
				
				if (label == 1)
					writer.write(rate + " " +(point[0] - 397.2242494102408) + " " + (point[1] - 769.4661346319322) );
				writer.write("\r\n");
				if (label == 4)
					writer.write(rate + " " +(point[0] - 717.1524737462735) + " " + (point[1] - 174.82396265630092));
				writer.write("\r\n");
				if (label == 6)
					writer.write(rate + " " +(point[0] - 816.6941877879168) + " " + (point[1] - 748.5144466482486) );
				writer.write("\r\n");
				if (label == 7)
					writer.write(rate + " " +(point[0] - 880.1504117681783) + " " + (point[1] - 401.55153859401196) );
				writer.write("\r\n");
				if (label == 2)
					writer.write(rate + " " +(point[0] - 460.7639736040134) + " " + (point[1] - 259.375981325999 ) );
				writer.write("\r\n");

				if (label == 3)
					writer.write(rate + " " +(point[0] - 489.3550892530346) + " " + (point[1] - 809.2811845652224  ) );
				writer.write("\r\n");
				if (label == 5)
					writer.write(rate + " " +(point[0] - 809.0863448241871 ) + " " + (point[1] - 189.48021322779607) );
				writer.write("\r\n");
				
				if (label == 8)
					writer.write(rate + " " +(point[0] - 934.7636427792683) + " " + (point[1] - 770.3825995843341) );
				writer.write("\r\n");
				if (label == 9)
					writer.write(rate + " " +(point[0] - 989.0911222053718) + " " + (point[1] - 388.9015066531777));
				writer.write("\r\n");
				
				}
			
			if (rate == 5){
				if (label == 0)
					writer.write(rate + " " +(point[0] - 372.2165602373956 ) + " " + (point[1] - 209.24284557327698));
				writer.write("\r\n");
				
				if (label == 1)
					writer.write(rate + " " +(point[0] - 394.9901813198901) + " " + (point[1] - 768.6756575778634) );
				writer.write("\r\n");
				if (label == 4)
					writer.write(rate + " " +(point[0] - 714.9184056559228) + " " + (point[1] - 174.55886471959468));
				writer.write("\r\n");
				if (label == 6)
					writer.write(rate + " " +(point[0] - 814.4601196975661) + " " + (point[1] - 748.5275229077819) );
				writer.write("\r\n");
				if (label == 7)
					writer.write(rate + " " +(point[0] - 877.9163436778276) + " " + (point[1] - 402.2666831920311) );
				writer.write("\r\n");
				if (label == 2)
					writer.write(rate + " " +(point[0] - 464.3863385577169) + " " + (point[1] - 263.336050947176  ) );
				writer.write("\r\n");

				if (label == 3)
					writer.write(rate + " " +(point[0] - 493.1259992299893) + " " + (point[1] - 812.7839538558577  ) );
				writer.write("\r\n");
				if (label == 5)
					writer.write(rate + " " +(point[0] - 810.0793780314149 ) + " " + (point[1] - 189.99249404731648) );
				writer.write("\r\n");
				
				if (label == 8)
					writer.write(rate + " " +(point[0] - 942.7635057469248) + " " + (point[1] - 778.5754931747326) );
				writer.write("\r\n");
				if (label == 9)
					writer.write(rate + " " +(point[0] - 997.6626690686753) + " " + (point[1] - 394.9742499361645));
				writer.write("\r\n");
				
				}
			
			

			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	
	

}