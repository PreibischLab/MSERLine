package peakFitter;

import java.util.ArrayList;
import com.sun.tools.javac.util.Pair;

import LineModels.GaussianLineds;
import LineModels.GaussianSplineds;
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
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
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
	public int maxiter = 500;
	public double lambda = 1e-3;
	public double termepsilon = 1e-1;
	// Mask fits iteration param
	public int iterations = 100;
	public double cutoffdistance = 50;
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

	
	public enum Fitfunctiontype{
		
		
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

			final double Curvature = PrevFrameparamstart.get(index).Curvature;

			final double Inflection= PrevFrameparamstart.get(index).Inflection;

			Point linepoint = new Point(ndims);
			linepoint.setPosition(
					new long[] { (long) PrevFrameparamstart.get(index).currentpos[0], (long) PrevFrameparamstart.get(index).currentpos[1] });
			Point fixedstartpoint = new Point(ndims);
			fixedstartpoint.setPosition(
					new long[] { (long) PrevFrameparamstart.get(index).fixedpos[0], (long) PrevFrameparamstart.get(index).fixedpos[1] });

			
		

			int labelstart = Getlabel(fixedstartpoint, originalslope, originalintercept);
			Indexedlength paramnextframestart;
			
			if (labelstart!=Integer.MIN_VALUE)
			
				paramnextframestart = Getfinaltrackparam(PrevFrameparamstart.get(index), labelstart, psf,
						framenumber, StartorEnd.Start);
			else
				paramnextframestart = PrevFrameparamstart.get(index);
           if (paramnextframestart == null)
        	   paramnextframestart = PrevFrameparamstart.get(index);
		

			final_paramliststart.add(paramnextframestart);

			final double[] oldstartpoint =  PrevFrameparamstart.get(index).currentpos;

			final double[] newstartpoint = paramnextframestart.currentpos;

			final double newstartslope = originalslope;
			final double newstartintercept = originalintercept;
			final double[] directionstart = { (newstartpoint[0] - oldstartpoint[0]) / framediff,
					(newstartpoint[1] - oldstartpoint[1]) / framediff };

			final Trackproperties startedge = new Trackproperties(labelstart, oldstartpoint, newstartpoint,
					newstartslope, newstartintercept, originalslope, originalintercept, PrevFrameparamstart.get(index).seedLabel, PrevFrameparamstart.get(index).fixedpos);

			startinframe.add(startedge);
			
			}
			for (int index = 0; index < PrevFrameparamend.size(); ++index) {
				
				Point secondlinepoint = new Point(ndims);
				secondlinepoint.setPosition(
						new long[] { (long) PrevFrameparamend.get(index).currentpos[0], (long) PrevFrameparamend.get(index).currentpos[1] });
				Point fixedendpoint = new Point(ndims);
				fixedendpoint.setPosition(
						new long[] { (long) PrevFrameparamend.get(index).fixedpos[0], (long) PrevFrameparamend.get(index).fixedpos[1] });

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

			final double[] oldendpoint =  PrevFrameparamend.get(index).currentpos;

			double[] newendpoint = paramnextframeend.currentpos;

			final double newendslope = originalslopeend;
			final double newendintercept = originalinterceptend;
			final double[] directionend = { (newendpoint[0] - oldendpoint[0]) / framediff,
					(newendpoint[1] - oldendpoint[1]) / framediff };

			final Trackproperties endedge = new Trackproperties(labelend, oldendpoint, newendpoint, newendslope,
					newendintercept, originalslopeend, originalinterceptend, PrevFrameparamend.get(index).seedLabel, PrevFrameparamend.get(index).fixedpos);

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

		RandomAccessibleInterval<FloatType> currentimg = imgs.get(label).Actualroi;

		FinalInterval interval = imgs.get(label).interval;

		currentimg = Views.interval(currentimg, interval);

		double slope = iniparam.originalslope;
		double intercept = iniparam.originalintercept;
		double Curvature = iniparam.Curvature;
		double Inflection = iniparam.Inflection;

		final Cursor<FloatType> outcursor = Views.iterable(currentimg).localizingCursor();

		final double maxintensityline = GetLocalmaxmin.computeMaxIntensity(currentimg);
		
		
		if (model ==  UserChoiceModel.Line){
			while (outcursor.hasNext()) {

				outcursor.fwd();

				if (outcursor.get().get() / maxintensityline > Intensityratio) {

					outcursor.localize(newposition);

					long pointonline = (long) (outcursor.getLongPosition(1) - slope * outcursor.getLongPosition(0)
							- intercept);

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
		MinandMax[2 * ndims] = 0.5 * Math.min(psf[0], psf[1]);
		MinandMax[2 * ndims + 1] = iniparam.lineintensity;
		MinandMax[2 * ndims + 2] = iniparam.background;
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
		
		else {
			while (outcursor.hasNext()) {

				outcursor.fwd();

				if (outcursor.get().get() / maxintensityline > Intensityratio) {

					outcursor.localize(newposition);

					long pointoncurve = (long) (outcursor.getLongPosition(1) - slope * outcursor.getLongPosition(0) - Curvature* outcursor.getLongPosition(0) *outcursor.getLongPosition(0) 
							- Inflection *outcursor.getLongPosition(0) *outcursor.getLongPosition(0) *outcursor.getLongPosition(0) 
							- intercept);

					// To get the min and max co-rodinates along the line so we
					// have starting points to
					// move on the line smoothly

					if (pointoncurve == 0) {
						for (int d = 0; d < ndims; ++d) {
							if (outcursor.getDoublePosition(d) <= minVal[d])
								minVal[d] = outcursor.getDoublePosition(d);

							if (outcursor.getDoublePosition(d) >= maxVal[d])
								maxVal[d] = outcursor.getDoublePosition(d);

						}
					}
				}
			}
			final double[] MinandMax = new double[2 * ndims + 6];

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
			MinandMax[2 * ndims] = 0.5 * Math.min(psf[0], psf[1]);
			MinandMax[2 * ndims + 1] = iniparam.lineintensity;
			MinandMax[2 * ndims + 2] = iniparam.background;
			
			
			MinandMax[2 * ndims + 3] = slope;
			MinandMax[2 * ndims + 4] = Curvature;
			MinandMax[2 * ndims + 5] = Inflection;
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
		
			
		
		
		
		

	}

	public Indexedlength Getfinaltrackparam(final Indexedlength iniparam, final int label, final double[] psf, final int rate,
			final StartorEnd startorend) {

			

		
			final double[] LMparam = MakerepeatedLineguess(iniparam, label);
			if (LMparam == null)
				return iniparam;

			else {
				
				final double[] inistartpos = { LMparam[0], LMparam[1] };
				final double[] iniendpos = { LMparam[2], LMparam[3] };
				final double[] inipos = { iniparam.currentpos[0], iniparam.currentpos[1] };
				double inicutoffdistance = Distance(inistartpos, iniendpos);

				final long radius = (long) ( Math.min(psf[0], psf[1]));
				RandomAccessibleInterval<FloatType> currentimg = imgs.get(label).Actualroi;

				FinalInterval interval = imgs.get(label).interval;

				currentimg = Views.interval(currentimg, interval);

				final double[] fixed_param = new double[ndims];

				for (int d = 0; d < ndims; ++d) {

					fixed_param[d] = 1.0 / Math.pow(psf[d], 2);

				}
				
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
				if (model == UserChoiceModel.Line){
					UserChoiceFunction = new GaussianLineds();
				}
				if (model == UserChoiceModel.Spline){
					
					UserChoiceFunction = new GaussianSplineds();
				}
				if (inicutoffdistance >  radius){
				// LM solver part
					
						try {
							LevenbergMarquardtSolverLine.solve(X, LMparam, fixed_param, I, UserChoiceFunction,
									lambda, termepsilon, maxiter);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					
						for (int j = 0; j < LMparam.length; j++) {
							if (Double.isNaN(LMparam[j]))
								LMparam[j] = safeparam[j];
						}
					
					final double[] startpos = new double[ndims];
					final double[] endpos = new double[ndims];

					for (int d = 0; d < ndims; ++d) {
						startpos[d] = LMparam[d];
						endpos[d] = LMparam[d + ndims];

					}


					final double maxintensityline = GetLocalmaxmin.computeMaxIntensity(currentimg);

					double newslope = iniparam.originalslope;
					double newintercept = iniparam.originalintercept;
					double dx = LMparam[ 2 *ndims] / Math.sqrt(1 + newslope * newslope);
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
						startfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, startpos.clone(), psf,
								iterations, dxvector, newslope, newintercept, maxintensityline, halfgaussian,
								EndfitMSER.StartfitMSER, label);
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

					

					for (int d = 0; d < ndims; ++d) {
						if (Double.isNaN(startfit[d]) || Double.isNaN(endfit[d])) {
							System.out.println("Mask fits fail, returning LM solver results!");
							LMparam[d] = startpos[d];
							LMparam[d + ndims] = endpos[d];

						}

					}

					final int seedLabel = iniparam.seedLabel;

					final double[] finalstartpoint = { LMparam[0], LMparam[1] };
					final double[] finalendpoint = { LMparam[2], LMparam[3] };
					
					if (model == UserChoiceModel.Line){
					if (startorend== StartorEnd.Start){
						final double currentslope = (finalstartpoint[1] - inipos[1]) / (finalstartpoint[0] - inipos[0]);
						final double currentintercept = finalstartpoint[1] - currentslope * finalstartpoint[0];
						
						Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, 
								LMparam[2 * ndims], LMparam[2 * ndims + 1], 
								LMparam[2 * ndims + 2], finalstartpoint, iniparam.fixedpos, currentslope, currentintercept,
								iniparam.originalslope, iniparam.originalintercept);
					System.out.println("New X: " + finalstartpoint[0] + " New Y: " + finalstartpoint[1]);
						return PointofInterest;
					}
					else{
						

						final double currentslope = (finalendpoint[1] - inipos[1]) / (finalendpoint[0] - inipos[0]);
						final double currentintercept = finalendpoint[1] - currentslope * finalendpoint[0];
						
						Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, 
								LMparam[2 * ndims], LMparam[2 * ndims + 1], 
								LMparam[2 * ndims + 2], finalendpoint, iniparam.fixedpos, currentslope, currentintercept,
								iniparam.originalslope, iniparam.originalintercept);
						System.out.println("New X: " + finalendpoint[0] + " New Y: " + finalendpoint[1]);
							
						return PointofInterest;
						
						
					}
				}
					
					else{
						if (startorend== StartorEnd.Start){
							final double currentslope = LMparam[2 * ndims + 3];
							

							final double Curvature = LMparam[2 * ndims + 4];
							final double Inflection = LMparam[2 * ndims + 5];
							final double currentintercept = finalendpoint[1] - currentslope *finalendpoint[0] - Curvature * finalendpoint[0] * finalendpoint[0]- Inflection * finalendpoint[0] * finalendpoint[0] *  finalendpoint[0] ;
							System.out.println("Curvature: " + Curvature + " " + "Inflection" + Inflection  );
							Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, 
									LMparam[2 * ndims], LMparam[2 * ndims + 1], 
									LMparam[2 * ndims + 2], finalstartpoint, iniparam.fixedpos, currentslope, currentintercept,
									iniparam.originalslope, iniparam.originalintercept, Curvature, Inflection);
						System.out.println("New X: " + finalstartpoint[0] + " New Y: " + finalstartpoint[1]);
							return PointofInterest;
						}
						else{
							

							final double currentslope = (finalendpoint[1] - inipos[1]) / (finalendpoint[0] - inipos[0]);
							final double currentintercept = finalendpoint[1] - currentslope * finalendpoint[0];
							final double Curvature = LMparam[2 * ndims + 3];
							final double Inflection = LMparam[2 * ndims + 4];
							System.out.println("Curvature: " + Curvature + " " + "Inflection" + Inflection  );
							Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, 
									LMparam[2 * ndims], LMparam[2 * ndims + 1], 
									LMparam[2 * ndims + 2], finalendpoint, iniparam.fixedpos, currentslope, currentintercept,
									iniparam.originalslope, iniparam.originalintercept, Curvature, Inflection);
							System.out.println("New X: " + finalendpoint[0] + " New Y: " + finalendpoint[1]);
								
							return PointofInterest;
							
							
						}
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

			
			Point newpoint = new Point(localcursor);
			datalist.add(newpoint, localcursor.get().copy());

		}

		return datalist;
	}

	public int Getlabel(final Point fixedpoint, final double originalslope,
			final double originalintercept) {

		ArrayList<Integer> currentlabel = new ArrayList<Integer>();
		
		int finallabel = Integer.MIN_VALUE;
		int pointonline = Integer.MAX_VALUE;
		for (int index = 0; index < imgs.size(); ++index) {

			RandomAccessibleInterval<FloatType> currentimg = imgs.get(index).Actualroi;
			FinalInterval interval = imgs.get(index).interval;
			currentimg = Views.interval(currentimg, interval);

			if (fixedpoint.getIntPosition(0) >= interval.min(0) && fixedpoint.getIntPosition(0) <= interval.max(0)
					&& fixedpoint.getIntPosition(1) >= interval.min(1)
					&& fixedpoint.getIntPosition(1) <= interval.max(1)) {

				currentlabel.add(imgs.get(index).roilabel);
			}

		}

		for (int index = 0; index < currentlabel.size(); ++index) {
			int distfromline = (int) Math
					.abs(fixedpoint.getIntPosition(1) - originalslope * fixedpoint.getIntPosition(0) - originalintercept);

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

}
