package peakFitter;

import java.util.ArrayList;
import com.sun.tools.javac.util.Pair;

import LineModels.GaussianLineds;
import LineModels.GaussianLinedsHF;
import LineModels.GaussianSplinesecorder;
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
	public int maxiter = 200;
	public double lambda = 1e-3;
	public double termepsilon = 1e-1;
	// Mask fits iteration param
	public int iterations = 300;
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

	public enum Fitfunctiontype {

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

			final Trackproperties startedge = new Trackproperties(labelstart, oldstartpoint, newstartpoint,
					newstartslope, newstartintercept, originalslope, originalintercept,
					PrevFrameparamstart.get(index).seedLabel, PrevFrameparamstart.get(index).fixedpos);

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

			final Trackproperties endedge = new Trackproperties(labelend, oldendpoint, newendpoint, newendslope,
					newendintercept, originalslopeend, originalinterceptend, PrevFrameparamend.get(index).seedLabel,
					PrevFrameparamend.get(index).fixedpos);

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
		if (model == UserChoiceModel.LineHF) {

			double slope = iniparam.originalslope;
			double intercept = iniparam.originalintercept;
			while (outcursor.hasNext()) {

				outcursor.fwd();

				outcursor.localize(newposition);
				long pointonline = (long) (outcursor.getLongPosition(1) - slope * outcursor.getLongPosition(0)
						- intercept);

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

		if (model == UserChoiceModel.Splineordersec) {
			double slope = iniparam.originalslope;
			double intercept = iniparam.originalintercept;
			double Curvature = iniparam.Curvature;

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

			MinandMax[2 * ndims] = 0.5 * Math.min(psf[0], psf[1]);
			MinandMax[2 * ndims + 2] = iniparam.lineintensity;
			MinandMax[2 * ndims + 3] = iniparam.background;
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

		else
			return null;

	}

	public Indexedlength Getfinaltrackparam(final Indexedlength iniparam, final int label, final double[] psf,
			final int rate, final StartorEnd startorend) {

		final double[] LMparam = MakerepeatedLineguess(iniparam, label);
		if (LMparam == null)
			return iniparam;

		else {

			final double[] inistartpos = { LMparam[0], LMparam[1] };
			final double[] iniendpos = { LMparam[2], LMparam[3] };
			final double[] inipos = iniparam.currentpos;
			double inicutoffdistance = Distance(inistartpos, iniendpos);

			final long radius = (long) (Math.min(psf[0], psf[1]));
			RandomAccessibleInterval<FloatType> currentimg = imgs.get(label).Actualroi;

			FinalInterval interval = imgs.get(label).interval;

			currentimg = Views.interval(currentimg, interval);

			final double[] fixed_param = new double[ndims + 2];

			for (int d = 0; d < ndims; ++d) {

				fixed_param[d] = 1.0 / Math.pow(psf[d], 2);

			}
			fixed_param[ndims] = iniparam.originalslope;
			fixed_param[ndims + 1] = iniparam.originalintercept;

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

			if (model == UserChoiceModel.LineHF) {

				UserChoiceFunction = new GaussianLinedsHF();
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

			final double[] startpos = new double[ndims];
			final double[] endpos = new double[ndims];

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
					final double newslope = (startpos[1] - inipos[1]) / (startpos[0] - inipos[0]);
					final double newintercept = startpos[1] - newslope * startpos[0];
					double dx = ds / Math.sqrt(1 + newslope * newslope);
					double dy = newslope * dx;
					double[] dxvector = { dx, dy };

					double[] startfit = new double[ndims];
					try {
						startfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, startpos.clone(), psf,
								iterations, dxvector, newslope, newintercept, maxintensityline, halfgaussian,
								EndfitMSER.StartfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(startfit, startpos);

					if (Math.sqrt(LMdist) > cutoffdistance) {

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
							newintercept, iniparam.originalslope, iniparam.originalintercept);
					System.out.println("New X: " + startfit[0] + " New Y: " + startfit[1]);
					return PointofInterest;
				} else {
					double ds = LMparam[2 * ndims];
					final double newslope = (endpos[1] - inipos[1]) / (endpos[0] - inipos[0]);
					final double newintercept = endpos[1] - newslope * endpos[0];

					double dx = ds / Math.sqrt(1 + newslope * newslope);
					double dy = newslope * dx;
					double[] dxvector = { dx, dy };

					double[] endfit = new double[ndims];
					try {
						endfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, endpos.clone(), psf, iterations,
								dxvector, newslope, newintercept, maxintensityline, halfgaussian, EndfitMSER.EndfitMSER,
								label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(endfit, endpos);

					if (Math.sqrt(LMdist) > cutoffdistance) {

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
					Indexedlength PointofInterest = new Indexedlength(label, seedLabel, framenumber, LMparam[2 * ndims],
							LMparam[2 * ndims + 1], LMparam[2 * ndims + 2], endfit, iniparam.fixedpos, newslope,
							newintercept, iniparam.originalslope, iniparam.originalintercept);
					System.out.println("New X: " + endfit[0] + " New Y: " + endfit[1]);

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
					try {
						startfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, startpos.clone(), psf,
								iterations, dxvector, newslope, newintercept, maxintensityline, halfgaussian,
								EndfitMSER.StartfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(startfit, startpos);

					if (Math.sqrt(LMdist) > cutoffdistance) {

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
							iniparam.originalintercept);
					System.out.println("New X: " + startfit[0] + " New Y: " + startfit[1]);
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

					double[] endfit = new double[ndims];
					try {
						endfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, endpos.clone(), psf, iterations,
								dxvector, newslope, newintercept, maxintensityline, halfgaussian, EndfitMSER.EndfitMSER,
								label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(endfit, endpos);

					if (Math.sqrt(LMdist) > cutoffdistance) {

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
							background, endfit, iniparam.fixedpos, newslope, newintercept, iniparam.originalslope,
							iniparam.originalintercept);
					System.out.println("New X: " + endfit[0] + " New Y: " + endfit[1]);

					return PointofInterest;

				}
			}

			else if (model == UserChoiceModel.Splineordersec) {
				if (startorend == StartorEnd.Start) {
					final double Curvature = LMparam[2 * ndims + 1];
					final double currentslope = iniparam.originalslope;

					final double currentintercept = iniparam.originalintercept;

					final double ds = LMparam[2 * ndims];
					final double lineIntensity = LMparam[2 * ndims + 2];
					final double background = LMparam[2 * ndims + 3];
					double[] startfit = new double[ndims];

					// 2 * Curvature * startpos[0]
					double dx = ds / Math.sqrt(1 + (currentslope )* (currentslope ));
					double dy = (currentslope ) * dx;
					double[] dxvector = { dx, dy };

					try {
						startfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, startpos.clone(), psf,
								iterations, dxvector, currentslope, currentintercept, maxintensityline, halfgaussian,
								EndfitMSER.StartfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(startfit, startpos);

					if (Math.sqrt(LMdist) > cutoffdistance) {

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
							iniparam.originalslope, iniparam.originalintercept, Curvature, 0);
					System.out.println("New X: " + startfit[0] + " New Y: " + startfit[1]);
					return PointofInterest;
				} else {

					final double Curvature = LMparam[2 * ndims + 1];
					final double currentslope = iniparam.originalslope;
					final double currentintercept = iniparam.originalintercept;
					final double ds = LMparam[2 * ndims];
					final double lineIntensity = LMparam[2 * ndims + 2];
					final double background = LMparam[2 * ndims + 3];
					System.out.println("Curvature: " + Curvature);
					double[] endfit = new double[ndims];
					// 2 * Curvature * endpos[0]
					double dx = ds / Math.sqrt(1+ (currentslope ) * (currentslope ));
					double dy = (currentslope ) * dx;
					double[] dxvector = { dx, dy };

					try {
						endfit = GaussianMaskFitMSER.sumofgaussianMaskFit(currentimg, endpos.clone(), psf, iterations,
								dxvector, currentslope, currentintercept, maxintensityline, halfgaussian,
								EndfitMSER.EndfitMSER, label);
					} catch (Exception e) {
						e.printStackTrace();
					}

					final double LMdist = sqDistance(endfit, endpos);

					if (Math.sqrt(LMdist) > cutoffdistance) {

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
							iniparam.originalslope, iniparam.originalintercept, Curvature, 0);
					System.out.println("New X: " + endfit[0] + " New Y: " + endfit[1] + "New Xlm: " + endpos[0]
							+ " New Ylm: " + endpos[1]);

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

			Point newpoint = new Point(localcursor);
			datalist.add(newpoint, localcursor.get().copy());

		}

		return datalist;
	}

	public int Getlabel(final Point fixedpoint, final double originalslope, final double originalintercept) {

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
			int distfromline = (int) Math.abs(
					fixedpoint.getIntPosition(1) - originalslope * fixedpoint.getIntPosition(0) - originalintercept);
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