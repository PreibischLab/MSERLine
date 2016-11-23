package velocityanalyser;

import java.io.File;
import java.util.ArrayList;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.sun.tools.javac.util.Pair;

import drawandOverlay.DisplaysubGraphend;
import drawandOverlay.DisplaysubGraphstart;
import drawandOverlay.OverlayLines;
import drawandOverlay.PushCurves;
import graphconstructs.Trackproperties;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import labeledObjects.CommonOutput;
import labeledObjects.CommonOutputHF;
import labeledObjects.Subgraphs;
import lineFinder.LinefinderHFHough;
import lineFinder.LinefinderHFMSER;
import lineFinder.LinefinderHough;
import lineFinder.LinefinderMSER;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import peakFitter.SubpixelLengthPCLine;
import peakFitter.SubpixelVelocityPCLine;
import preProcessing.MedianFilter2D;

public class VelocitydetectionMSER {

	public static void main(String[] args) throws Exception {

		/***
		 * MSER Roi's to detect Microtubules and track
		 * the growth at Sub-pixel accuracy. Optimizers used: Levenberg-Marqurat
		 * solver and Weighted centre of mass fits. Program reqires PSF of the
		 * microscope to be computed and analysed and takes the determined
		 * Sigmas as the input. @ Varun Kapoor
		 */

		new ImageJ();

		// Load the stack of images
		RandomAccessibleInterval<FloatType> img = util.ImgLib2Util.openAs32Bit(
				// new
				// File("../res/2016-09-28_bovine_cy5seeds_cy3tub_6uM_seeds.tif"),

				// new File("../res/test-bent.tif"),
				// new File("../res/Pnoise1snr15.tif"),
				new File("../res/test_moving.tif"), 
			//	new File("../res/small_mt.tif"), 
							new ArrayImgFactory<FloatType>());
		
		RandomAccessibleInterval<FloatType> preprocessedimg = util.ImgLib2Util.openAs32Bit(
				// new
				 //File("../res/2016-09-28_bovine_cy5seeds_cy3tub_6uM_seeds.tif"),

				// new File("../res/test-bent.tif"),
				// new File("../res/Pnoise1snr15.tif"),
				new File("../res/test_moving.tif"), 
			//	new File("../res/small_mt.tif"), 
							new ArrayImgFactory<FloatType>());
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

			ImageJFunctions.show(inputimg);
			
			
			LinefinderMSER newlineMser = new LinefinderMSER(img, inputimg, minlength, 0);
			LinefinderHough newlineHough = new LinefinderHough(img, inputimg, minlength, 0);
            Overlay overlay = newlineMser.getOverlay();
			ImageJFunctions.show(inputimg).setTitle("Preprocessed extended image");
			ImagePlus impcurr = IJ.getImage();
			impcurr.setOverlay(overlay);
			
			
			
			SubpixelLengthPCLine MTline = new SubpixelLengthPCLine(img, newlineMser, psf, minlength, 0);
			MTline.checkInput();
			MTline.process();
			Pair<ArrayList<double[]>,ArrayList<double[]>> PrevFrameparam = MTline.getResult();

			// Draw the detected lines
			RandomAccessibleInterval<FloatType> gaussimg = new ArrayImgFactory<FloatType>().create(img,
					new FloatType());
			PushCurves.DrawallLine(gaussimg, PrevFrameparam.fst, PrevFrameparam.snd, psf);
			ImageJFunctions.show(gaussimg).setTitle("Exact-line");
		}

		if (ndims > 2) {
			// Do Hough transform on the First seed image

			RandomAccessibleInterval<FloatType> groundframe = Views.hyperSlice(img, ndims - 1, 0);
			RandomAccessibleInterval<FloatType> groundframepre = Views.hyperSlice(preprocessedimg, ndims - 1, 0);

			
			final MedianFilter2D<FloatType> medfilter = new MedianFilter2D<FloatType>(groundframepre, 1);
			medfilter.process();
			RandomAccessibleInterval<FloatType> inputimg = medfilter.getResult();
			Normalize.normalize(Views.iterable(inputimg), minval, maxval);
			ImageJFunctions.show(inputimg);
			
			/**
			 * 
			 * Line finder using MSER or Hough
			 * 
			 */
			LinefinderMSER newlineMser = new LinefinderMSER(groundframe, inputimg, minlength, 0);
			LinefinderHough newlineHough = new LinefinderHough(groundframe, inputimg, minlength, 0);
			
			
            Overlay overlay = newlineMser.getOverlay();
            ImageJFunctions.show(inputimg).setTitle("Preprocessed extended image");
		

           
			ImagePlus impcurr = IJ.getImage();
			impcurr.setOverlay(overlay);
		
			RandomAccessibleInterval<FloatType> gaussimg = new ArrayImgFactory<FloatType>().create(groundframe,
					new FloatType());
			
			

			SubpixelLengthPCLine MTline = new SubpixelLengthPCLine(groundframe, newlineMser, psf, minlength, 0);
			MTline.checkInput();
			
			MTline.process();
			Pair<ArrayList<double[]>,ArrayList<double[]>> PrevFrameparam = MTline.getResult();

			// Draw the detected lines
			PushCurves.DrawallLine(gaussimg, PrevFrameparam.fst, PrevFrameparam.snd, psf);
			ImageJFunctions.show(gaussimg).setTitle("Exact-line");

			
			

			
			// Now start tracking the moving ends of the Microtubule and make
			// seperate graph for both ends
			
			
			final int maxframe =(int) img.dimension(ndims - 1);

			for (int frame = 1; frame < maxframe; ++frame) {

				IntervalView<FloatType> currentframe = Views.hyperSlice(img, ndims - 1, frame);
				IntervalView<FloatType> currentframepre = Views.hyperSlice(preprocessedimg, ndims - 1, frame);

				final MedianFilter2D<FloatType> medfiltercurr = new MedianFilter2D<FloatType>(currentframepre, 1);
				medfiltercurr.process();
				RandomAccessibleInterval<FloatType> inputimgpre = medfiltercurr.getResult();
				Normalize.normalize(Views.iterable(inputimgpre), minval, maxval);

				ImageJFunctions.show(inputimgpre);

				LinefinderHFMSER newlinenextMser = new LinefinderHFMSER(currentframe, inputimgpre, minlength, frame);
				LinefinderHFHough newlinenextHough = new LinefinderHFHough(currentframe, inputimgpre, minlength, frame);
				
				Overlay overlaynext = newlinenextMser.getOverlay();
	            ImageJFunctions.show(inputimgpre).setTitle("Preprocessed extended image");
			

	            ImagePlus impcurrnext = IJ.getImage();
				impcurrnext.setOverlay(overlaynext);
				/**
				 * 
				 * Getting tracks for both the ends
				 * 
				 */

			
					final SubpixelVelocityPCLine growthtracker = new SubpixelVelocityPCLine(currentframe, newlinenextMser,
							PrevFrameparam.fst, PrevFrameparam.snd, psf, frame);
					growthtracker.checkInput();
					growthtracker.process();
					Pair<ArrayList<double[]>, ArrayList<double[]>> NewFrameparam = growthtracker.getResult();
					ArrayList<Trackproperties> startStateVectors = growthtracker.getstartStateVectors();
					ArrayList<Trackproperties> endStateVectors = growthtracker.getendStateVectors();
			
				PrevFrameparam = NewFrameparam;
				
				Allstart.add(startStateVectors);
				Allend.add(endStateVectors);
				// Draw the lines detected in the current frame
				RandomAccessibleInterval<FloatType> newgaussimg = new ArrayImgFactory<FloatType>().create(groundframe,
						new FloatType());
				PushCurves.DrawallLine(newgaussimg, NewFrameparam.fst, NewFrameparam.snd, psf);
				ImageJFunctions.show(newgaussimg).setTitle("Exact-line");

			}

			// Overlay the graphs on the stack

			// Make graph to track the start and the end point
			// Show the stack
			
			ImagePlus impstart = ImageJFunctions.show(img);
			ImagePlus impend = ImageJFunctions.show(preprocessedimg);
			
			final Trackstart trackerstart = new Trackstart(Allstart, maxframe);
			final Trackend trackerend = new Trackend(Allend, maxframe);
			trackerstart.process();
			SimpleWeightedGraph<double[], DefaultWeightedEdge> graphstart = trackerstart.getResult();
			ArrayList<Subgraphs> subgraphstart = trackerstart.getFramedgraph();

			DisplaysubGraphstart displaytrackstart = new DisplaysubGraphstart(impstart, subgraphstart);
			displaytrackstart.getImp();
			impstart.draw();

			trackerend.process();
			SimpleWeightedGraph<double[], DefaultWeightedEdge> graphend = trackerend.getResult();
			ArrayList<Subgraphs> subgraphend = trackerend.getFramedgraph();

			DisplaysubGraphend displaytrackend = new DisplaysubGraphend(impend, subgraphend);
			displaytrackend.getImp();
			impend.draw();

		}
		
	}
	
	
}