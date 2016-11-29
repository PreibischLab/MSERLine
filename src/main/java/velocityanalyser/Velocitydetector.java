package velocityanalyser;

import java.io.File;
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
import ij.ImageJ;
import ij.ImagePlus;
import labeledObjects.Indexedlength;
import labeledObjects.Subgraphs;
import lineFinder.FindlinesVia;
import lineFinder.FindlinesVia.LinefindingMethod;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import preProcessing.MedianFilter2D;

public class Velocitydetector {

	public static void main(String[] args) throws Exception {

		/***
		 * Detect Microtubules and track
		 * the growth at Sub-pixel accuracy. 
		 * Line finding Interface: MSER, HOUGH, MSERwHOUGH. 
		 * Sub Pixel Optimizers used: Levenberg-Marqurat
		 * solver and Weighted centre of mass fits. 
		 * 
		 * Program reqires PSF of the
		 * microscope to be computed and analysed and takes the determined
		 * Sigmas as the input. @ Varun Kapoor
		 */

		new ImageJ();

		// Load the stack of images
		RandomAccessibleInterval<FloatType> img = util.ImgLib2Util.openAs32Bit(new File("../res/test_bent.tif"), new ArrayImgFactory<FloatType>());
		
		RandomAccessibleInterval<FloatType> preprocessedimg = util.ImgLib2Util.openAs32Bit( new File("../res/test_bent.tif"), new ArrayImgFactory<FloatType>());
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
		final long radius =  (long) Math.ceil(Math.sqrt(psf[0] * psf[0] + psf[1] * psf[1]));

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
			
		    LinefindingMethod findLinesVia =  LinefindingMethod.MSER;
		
		    Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> PrevFrameparam = FindlinesVia.LinefindingMethod(img, inputimg, minlength, 0, psf, findLinesVia, UserChoiceModel.Line);
			

			// Draw the detected lines
			RandomAccessibleInterval<FloatType> gaussimg = new ArrayImgFactory<FloatType>().create(img,
					new FloatType());
			PushCurves.DrawstartLine(gaussimg, PrevFrameparam.fst,PrevFrameparam.snd, psf);
			ImageJFunctions.show(gaussimg).setTitle("Exact-line-start");
			
			
		}

		if (ndims > 2) {
			// Do Hough transform on the First seed image

			RandomAccessibleInterval<FloatType> groundframe = Views.hyperSlice(img, ndims - 1, 0);
			RandomAccessibleInterval<FloatType> groundframepre = Views.hyperSlice(preprocessedimg, ndims - 1, 0);

			
			final MedianFilter2D<FloatType> medfilter = new MedianFilter2D<FloatType>(groundframepre, 1);
			medfilter.process();
			RandomAccessibleInterval<FloatType> inputimg = medfilter.getResult();
			Normalize.normalize(Views.iterable(inputimg), minval, maxval);
			ImageJFunctions.show(groundframe);
			
			/**
			 * 
			 * Line finder using MSER or Hough or a combination
			 * 
			 */
			 LinefindingMethod findLinesVia =  LinefindingMethod.MSER;
			    Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, inputimg, minlength, 0, psf, findLinesVia, UserChoiceModel.Line);
				
	            ImageJFunctions.show(inputimg).setTitle("Preprocessed extended image");
			

	    
			// Draw the detected lines
	            RandomAccessibleInterval<FloatType> gaussimg = new ArrayImgFactory<FloatType>().create(groundframe,
						new FloatType());
			PushCurves.DrawstartLine(gaussimg, PrevFrameparam.fst,PrevFrameparam.snd, psf);
			ImageJFunctions.show(gaussimg).setTitle("Exact-line-start");
			
			

			
			// Now start tracking the moving ends of the Microtubule and make
			// seperate graph for both ends
			
			
			final int maxframe = (int) img.dimension(ndims - 1);

			for (int frame = 1; frame < maxframe; ++frame) {

				IntervalView<FloatType> currentframe = Views.hyperSlice(img, ndims - 1, frame);
				IntervalView<FloatType> currentframepre = Views.hyperSlice(preprocessedimg, ndims - 1, frame);

				final MedianFilter2D<FloatType> medfiltercurr = new MedianFilter2D<FloatType>(currentframepre, 1);
				medfiltercurr.process();
				RandomAccessibleInterval<FloatType> inputimgpre = medfiltercurr.getResult();
				Normalize.normalize(Views.iterable(inputimgpre), minval, maxval);

				ImageJFunctions.show(currentframe);

				
				/**
				 * 
				 * Getting tracks for both the ends
				 * 
				 */
				 LinefindingMethod findLinesViaHF =  LinefindingMethod.MSER;
				 UserChoiceModel userChoiceModelHF = UserChoiceModel.Splineordersec;
				 Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>> returnVector =
						 FindlinesVia.LinefindingMethodHF(currentframe, inputimgpre, PrevFrameparam, minlength, frame, psf, findLinesViaHF, userChoiceModelHF);
				 
				 Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> NewFrameparam = returnVector.snd;
				 
					
					ArrayList<Trackproperties> startStateVectors = returnVector.fst.fst;
					ArrayList<Trackproperties> endStateVectors = returnVector.fst.snd;

		            ImageJFunctions.show(inputimgpre).setTitle("Preprocessed extended image");
				

				PrevFrameparam = NewFrameparam;
				
				Allstart.add(startStateVectors);
				Allend.add(endStateVectors);
				// Draw the lines detected in the current frame
				RandomAccessibleInterval<FloatType> newgaussimg = new ArrayImgFactory<FloatType>().create(groundframe,
						new FloatType());
				PushCurves.DrawstartLine(newgaussimg, NewFrameparam.fst,NewFrameparam.snd, psf);
				ImageJFunctions.show(newgaussimg).setTitle("Exact-line-start");

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
			

		}
		
	}
	
	
}