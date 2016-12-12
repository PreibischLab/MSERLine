package velocityanalyser;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.sun.tools.javac.util.Pair;

import LineModels.MTFitFunction;
import LineModels.UseLineModel.UserChoiceModel;
import drawandOverlay.DisplayGraph;
import drawandOverlay.DisplaysubGraphend;
import drawandOverlay.DisplaysubGraphstart;
import drawandOverlay.PushCurves;
import graphconstructs.Trackproperties;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import labeledObjects.Indexedlength;
import labeledObjects.Subgraphs;
import lineFinder.FindlinesVia;
import lineFinder.LinefinderHough;
import lineFinder.LinefinderMSER;
import lineFinder.LinefinderMSERwHough;
import lineFinder.FindlinesVia.LinefindingMethod;
import lineFinder.LinefinderHFHough;
import lineFinder.LinefinderHFMSER;
import lineFinder.LinefinderHFMSERwHough;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import preProcessing.Kernels;
import preProcessing.MedianFilter2D;

public class Velocitydetector {

	public static void main(String[] args) throws Exception {

		/***
		 * Detect Microtubules and track the growth at Sub-pixel accuracy. Line
		 * finding Interface: MSER, HOUGH, MSERwHOUGH. Sub Pixel Optimizers
		 * used: Levenberg-Marqurat solver and Weighted centre of mass fits.
		 * 
		 * Program reqires PSF of the microscope to be computed and analysed and
		 * takes the determined Sigmas as the input. @ Varun Kapoor
		 */
		new ImageJ();

		// Load the stack of images
		RandomAccessibleInterval<FloatType> img = util.ImgLib2Util.openAs32Bit(new File("../res/super_bent.tif"),
				new ArrayImgFactory<FloatType>());

		RandomAccessibleInterval<FloatType> preprocessedimg = util.ImgLib2Util
				.openAs32Bit(new File("../res/super_bent.tif"), new ArrayImgFactory<FloatType>());
		int ndims = img.numDimensions();

		// Normalize the intensity of the whole stack to be between min and max
		// value specified here
		new Normalize();

		FloatType minval = new FloatType(0);
		FloatType maxval = new FloatType(1);
		Normalize.normalize(Views.iterable(img), minval, maxval);
		Normalize.normalize(Views.iterable(preprocessedimg), minval, maxval);
		final double[] psf = { 1.65, 1.47 };
		// Declare all the constants needed by the program here:

		ArrayList<ArrayList<Trackproperties>> Allstart = new ArrayList<ArrayList<Trackproperties>>();
		ArrayList<ArrayList<Trackproperties>> Allend = new ArrayList<ArrayList<Trackproperties>>();
		final long radius = (long) (Math.ceil(Math.sqrt(psf[0] * psf[0] + psf[1] * psf[1])));

		// minimum length of the lines to be detected, the smallest possible
		// number is 2.
		final int minlength = (int) radius;

		if (ndims == 2) {

			// Preprocess image using Median Filter and suppress background
			final MedianFilter2D<FloatType> medfilter = new MedianFilter2D<FloatType>(preprocessedimg, 1);
			medfilter.process();
			RandomAccessibleInterval<FloatType> inputimg = medfilter.getResult();

			Normalize.normalize(Views.iterable(inputimg), minval, maxval);

			ImageJFunctions.show(img);

			LinefindingMethod findLinesVia = LinefindingMethod.MSER;
			Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> PrevFrameparam = null;
			if (findLinesVia == LinefindingMethod.MSER) {

				LinefinderMSER newlineMser = new LinefinderMSER(img, inputimg, minlength, 0);
				newlineMser.setMaxlines(4);
				Overlay overlay = newlineMser.getOverlay();
				ImagePlus impcurr = IJ.getImage();
				impcurr.setOverlay(overlay);
				PrevFrameparam = FindlinesVia.LinefindingMethod(img, inputimg, minlength, 0, psf, newlineMser,
						UserChoiceModel.Line);
			}

			if (findLinesVia == LinefindingMethod.Hough) {
				LinefinderHough newlineHough = new LinefinderHough(img, inputimg, minlength, 0);

				PrevFrameparam = FindlinesVia.LinefindingMethod(img, inputimg, minlength, 0, psf, newlineHough,
						UserChoiceModel.Line);
			}
			if (findLinesVia == LinefindingMethod.MSERwHough) {

				LinefinderMSERwHough newlineMserwHough = new LinefinderMSERwHough(img, inputimg, minlength, 0);
				newlineMserwHough.setMaxlines(4);
				Overlay overlay = newlineMserwHough.getOverlay();
				ImagePlus impcurr = IJ.getImage();
				impcurr.setOverlay(overlay);
				PrevFrameparam = FindlinesVia.LinefindingMethod(img, inputimg, minlength, 0, psf, newlineMserwHough,
						UserChoiceModel.Line);
			}

			// Draw the detected lines
			RandomAccessibleInterval<FloatType> gaussimg = new ArrayImgFactory<FloatType>().create(img,
					new FloatType());
			PushCurves.DrawstartLine(gaussimg, PrevFrameparam.fst, PrevFrameparam.snd, psf);
			ImageJFunctions.show(gaussimg).setTitle("Exact-line-start");

		}

		if (ndims > 2) {
			// Do Hough transform on the First seed image

			RandomAccessibleInterval<FloatType> groundframe = Views.hyperSlice(img, ndims - 1, 0);
			RandomAccessibleInterval<FloatType> groundframepre = Views.hyperSlice(preprocessedimg, ndims - 1, 0);
			Normalize.normalize(Views.iterable(groundframe), minval, maxval);
			Normalize.normalize(Views.iterable(groundframepre), minval, maxval);

			final MedianFilter2D<FloatType> medfilter = new MedianFilter2D<FloatType>(groundframepre, 1);
			medfilter.process();
			RandomAccessibleInterval<FloatType> preinputimg = medfilter.getResult();
			RandomAccessibleInterval<FloatType> inputimg = Kernels.CannyEdgeandMean(preinputimg, radius);
			/**
			 * 
			 * Line finder using MSER or Hough or a combination
			 * 
			 */
			LinefindingMethod findLinesVia = LinefindingMethod.MSER;
			Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> PrevFrameparam = null;

			if (findLinesVia == LinefindingMethod.MSER) {

				LinefinderMSER newlineMser = new LinefinderMSER(groundframe, inputimg, minlength, 0);
				newlineMser.setMaxlines(5);
				PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, inputimg, minlength, 0, psf, newlineMser,
						UserChoiceModel.Line);

				Overlay overlay = newlineMser.getOverlay();
				ImagePlus impcurr = IJ.getImage();
				impcurr.setOverlay(overlay);
			}

			if (findLinesVia == LinefindingMethod.Hough) {
				LinefinderHough newlineHough = new LinefinderHough(groundframe, inputimg, minlength, 0);

				PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, inputimg, minlength, 0, psf, newlineHough,
						UserChoiceModel.Line);
			}

			if (findLinesVia == LinefindingMethod.MSERwHough) {
				LinefinderMSERwHough newlineMserwHough = new LinefinderMSERwHough(groundframe, inputimg, minlength, 0);
				newlineMserwHough.setMaxlines(5);
				PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, inputimg, minlength, 0, psf,
						newlineMserwHough, UserChoiceModel.Line);

				Overlay overlay = newlineMserwHough.getOverlay();
				ImagePlus impcurr = IJ.getImage();
				impcurr.setOverlay(overlay);
			}

			// Now start tracking the moving ends of the Microtubule and make
			// seperate graph for both ends

			final int maxframe = (int) img.dimension(ndims - 1);

			for (int frame = 1; frame < maxframe; ++frame) {

				IntervalView<FloatType> currentframe = Views.hyperSlice(img, ndims - 1, frame);
				IntervalView<FloatType> currentframepre = Views.hyperSlice(preprocessedimg, ndims - 1, frame);
				Normalize.normalize(Views.iterable(currentframe), minval, maxval);
				Normalize.normalize(Views.iterable(currentframepre), minval, maxval);
				final MedianFilter2D<FloatType> medfiltercurr = new MedianFilter2D<FloatType>(currentframepre, 1);
				medfiltercurr.process();
				RandomAccessibleInterval<FloatType> preinputimgpre = medfiltercurr.getResult();
				RandomAccessibleInterval<FloatType> inputimgpre = Kernels.CannyEdgeandMean(preinputimgpre, radius);

				/**
				 * 
				 * Getting tracks for both the ends
				 * 
				 */
				LinefindingMethod findLinesViaHF = LinefindingMethod.MSER;
				UserChoiceModel userChoiceModelHF = UserChoiceModel.Splineordersecfixedds;
				Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>, Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>>> returnVector = null;

				if (findLinesViaHF == LinefindingMethod.MSER) {

					LinefinderHFMSER newlineMser = new LinefinderHFMSER(currentframe, inputimgpre, minlength, frame);
					newlineMser.setMaxlines(10);
					returnVector = FindlinesVia.LinefindingMethodHF(currentframe, inputimgpre, PrevFrameparam,
							minlength, frame, psf, newlineMser, userChoiceModelHF);
					Overlay overlay = newlineMser.getOverlay();
					ImagePlus impcurr = IJ.getImage();
					impcurr.setOverlay(overlay);
				}

				if (findLinesViaHF == LinefindingMethod.Hough) {
					LinefinderHFHough newlineHough = new LinefinderHFHough(currentframe, inputimgpre, minlength, frame);

					returnVector = FindlinesVia.LinefindingMethodHF(currentframe, inputimgpre, PrevFrameparam,
							minlength, frame, psf, newlineHough, userChoiceModelHF);
				}

				if (findLinesViaHF == LinefindingMethod.MSERwHough) {

					LinefinderHFMSERwHough newlineMserwHough = new LinefinderHFMSERwHough(currentframe, inputimgpre,
							minlength, frame);
					newlineMserwHough.setMaxlines(10);
					returnVector = FindlinesVia.LinefindingMethodHF(currentframe, inputimgpre, PrevFrameparam,
							minlength, frame, psf, newlineMserwHough, userChoiceModelHF);
					Overlay overlay = newlineMserwHough.getOverlay();
					ImagePlus impcurr = IJ.getImage();
					impcurr.setOverlay(overlay);
				}

				Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> NewFrameparam = returnVector.snd;

				ArrayList<Trackproperties> startStateVectors = returnVector.fst.fst;
				ArrayList<Trackproperties> endStateVectors = returnVector.fst.snd;

				PrevFrameparam = NewFrameparam;

				Allstart.add(startStateVectors);
				Allend.add(endStateVectors);
				// Draw the lines detected in the current frame
				RandomAccessibleInterval<FloatType> newgaussimg = new ArrayImgFactory<FloatType>().create(groundframe,
						new FloatType());
				PushCurves.DrawstartLine(newgaussimg, NewFrameparam.fst, NewFrameparam.snd, psf);

			}

			// Make graph to track the start and the end point
			// Show the stack

			ImagePlus impstart = ImageJFunctions.show(img);
			ImagePlus impend = ImageJFunctions.show(preprocessedimg);

			ImagePlus impstartsec = ImageJFunctions.show(img);
			ImagePlus impendsec = ImageJFunctions.show(preprocessedimg);

			final Trackstart trackerstart = new Trackstart(Allstart, maxframe);
			final Trackend trackerend = new Trackend(Allend, maxframe);
			trackerstart.process();
			SimpleWeightedGraph<double[], DefaultWeightedEdge> graphstart = trackerstart.getResult();
			ArrayList<Subgraphs> subgraphstart = trackerstart.getFramedgraph();

			DisplaysubGraphstart displaytrackstart = new DisplaysubGraphstart(impstart, subgraphstart);
			displaytrackstart.getImp();
			impstart.draw();

			DisplayGraph displaygraphtrackstart = new DisplayGraph(impstartsec, graphstart);
			displaygraphtrackstart.getImp();
			impstartsec.draw();

			trackerend.process();
			SimpleWeightedGraph<double[], DefaultWeightedEdge> graphend = trackerend.getResult();
			ArrayList<Subgraphs> subgraphend = trackerend.getFramedgraph();

			DisplaysubGraphend displaytrackend = new DisplaysubGraphend(impend, subgraphend);
			displaytrackend.getImp();
			impend.draw();

			DisplayGraph displaygraphtrackend = new DisplayGraph(impendsec, graphend);
			displaygraphtrackend.getImp();
			impendsec.draw();

			ArrayList<Pair<Integer, double[]>> lengthliststart = new ArrayList<Pair<Integer, double[]>>();
			for (int index = 1; index < Allstart.size(); ++index) {

				final int framenumber = index + 1;
				final ArrayList<Trackproperties> currentframe = Allstart.get(index);

				for (int frameindex = 0; frameindex < currentframe.size(); ++frameindex) {

					final double[] newpoint = currentframe.get(frameindex).newpoint;
					final double[] oldpoint = currentframe.get(frameindex).oldpoint;
					final double length = util.Boundingboxes.Distance(newpoint, oldpoint);
					final double[] startinfo = { oldpoint[0], oldpoint[1], newpoint[0], newpoint[1], length };
					Pair<Integer, double[]> lengthpair = new Pair<Integer, double[]>(framenumber, startinfo);

					lengthliststart.add(lengthpair);

				}

			}

			FileWriter writer = new FileWriter("../res/HNlength-movingstart.txt", true);

			for (int index = 0; index < lengthliststart.size(); ++index) {

				writer.write(lengthliststart.get(index).fst + " " + lengthliststart.get(index).snd[0] + " "
						+ lengthliststart.get(index).snd[1] + " " + lengthliststart.get(index).snd[2] + " "
						+ lengthliststart.get(index).snd[3] + " " + lengthliststart.get(index).snd[4]);
				writer.write("\r\n");
			}

			writer.close();

			ArrayList<Pair<Integer, double[]>> lengthlistend = new ArrayList<Pair<Integer, double[]>>();
			for (int index = 1; index < Allend.size(); ++index) {

				final int framenumber = index + 1;
				final ArrayList<Trackproperties> currentframe = Allend.get(index);

				for (int frameindex = 0; frameindex < currentframe.size(); ++frameindex) {

					final double[] newpoint = currentframe.get(frameindex).newpoint;
					final double[] oldpoint = currentframe.get(frameindex).oldpoint;
					final double length = util.Boundingboxes.Distance(newpoint, oldpoint);
					final double[] endinfo = { oldpoint[0], oldpoint[1], newpoint[0], newpoint[1], length };
					Pair<Integer, double[]> lengthpair = new Pair<Integer, double[]>(framenumber, endinfo);

					lengthlistend.add(lengthpair);

				}

			}

			FileWriter writerend = new FileWriter("../res/HNlength-movingend.txt", true);

			for (int index = 0; index < lengthlistend.size(); ++index) {

				writerend.write(lengthlistend.get(index).fst + " " + lengthlistend.get(index).snd[0] + " "
						+ lengthlistend.get(index).snd[1] + " " + lengthlistend.get(index).snd[2] + " "
						+ lengthlistend.get(index).snd[3] + " " + lengthlistend.get(index).snd[4]);
				writerend.write("\r\n");
			}

			writerend.close();

		}

	}

}