package lineFinder;

import java.util.ArrayList;

import com.sun.tools.javac.util.Pair;

import LineModels.UseLineModel.UserChoiceModel;
import graphconstructs.Trackproperties;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import labeledObjects.Indexedlength;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import peakFitter.SubpixelLengthPCLine;
import peakFitter.SubpixelVelocityPCLine;
import preProcessing.Kernels;

public  class FindlinesVia {

	public static  enum LinefindingMethod {

		MSER, Hough, MSERwHough;

	}
	
      protected LinefindingMethod MSER, Hough, MSERwHough;

	public static Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> LinefindingMethod(final RandomAccessibleInterval<FloatType> source,
			final RandomAccessibleInterval<FloatType> Preprocessedsource, final int minlength, final int framenumber, final double[] psf, final Linefinder linefinder, final UserChoiceModel model ) {

		
		Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>	PrevFrameparam = null;
		

			
			SubpixelLengthPCLine MTline = new SubpixelLengthPCLine(source, linefinder, psf, minlength, model, 0);
			MTline.checkInput();
			MTline.process();
			PrevFrameparam = MTline.getResult();
		
		return PrevFrameparam;

	}

	public static Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>, Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>>> 
	LinefindingMethodHF(final RandomAccessibleInterval<FloatType> source,
			final RandomAccessibleInterval<FloatType> Preprocessedsource,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> PrevFrameparam,
			final int minlength, final int framenumber, final double[] psf,  final LinefinderHF linefinder, final UserChoiceModel model ) {

		Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>> returnVector = null;
		
		

			final SubpixelVelocityPCLine growthtracker = new SubpixelVelocityPCLine(source, linefinder,
					PrevFrameparam.fst, PrevFrameparam.snd, psf, framenumber, model);
			growthtracker.checkInput();
			growthtracker.process();
			
			
			Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> NewFrameparam = growthtracker.getResult();
			ArrayList<Trackproperties> startStateVectors = growthtracker.getstartStateVectors();
			ArrayList<Trackproperties> endStateVectors = growthtracker.getendStateVectors();
			Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>> Statevectors = new Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>(startStateVectors, endStateVectors); 
			returnVector = 
					new Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>>(Statevectors, NewFrameparam);
			
			
			
			
		
			
		
		
		
		return returnVector;

	}
	
	
}
