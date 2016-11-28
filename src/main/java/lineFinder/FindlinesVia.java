package lineFinder;

import java.util.ArrayList;

import com.sun.tools.javac.util.Pair;

import graphconstructs.Trackproperties;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import labeledObjects.Indexedlength;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.real.FloatType;
import peakFitter.SubpixelLengthPCLine;
import peakFitter.SubpixelVelocityPCLine;

public  class FindlinesVia {

	public static  enum LinefindingMethod {

		MSER, Hough, MSERwHough;

	}
	
      protected LinefindingMethod MSER, Hough, MSERwHough;

	public static Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> LinefindingMethod(final RandomAccessibleInterval<FloatType> source,
			final RandomAccessibleInterval<FloatType> Preprocessedsource, final int minlength, final int framenumber, final double[] psf,  final LinefindingMethod findlines ) {

		
		Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>	PrevFrameparam = null;
		switch (findlines){
		
		case MSER:
		{
			LinefinderMSER newlineMser = new LinefinderMSER(source, Preprocessedsource, minlength, framenumber);
			newlineMser.setMaxlines(40);

			SubpixelLengthPCLine MTline = new SubpixelLengthPCLine(source, newlineMser, psf, minlength, 0);
			MTline.checkInput();
			MTline.process();
			PrevFrameparam = MTline.getResult();
			 Overlay overlay = newlineMser.getOverlay();
			 ImagePlus impcurr = IJ.getImage();
			 impcurr.setOverlay(overlay);
			
			
			break;
		}
			
		case Hough:
		{
			LinefinderHough newlineHough = new LinefinderHough(source, Preprocessedsource, minlength, framenumber);

			SubpixelLengthPCLine MTline = new SubpixelLengthPCLine(source, newlineHough, psf, minlength, 0);
			MTline.checkInput();
			MTline.process();
			PrevFrameparam = MTline.getResult();
			
			
			break;
		}
		case MSERwHough:
		{

			LinefinderMSERwHough newlineMserwHough = new LinefinderMSERwHough(source, Preprocessedsource, minlength, framenumber);
			newlineMserwHough.setMaxlines(40);

			SubpixelLengthPCLine MTline = new SubpixelLengthPCLine(source, newlineMserwHough, psf, minlength, 0);
			MTline.checkInput();
			MTline.process();
			PrevFrameparam = MTline.getResult();
			 Overlay overlay = newlineMserwHough.getOverlay();
			 ImagePlus impcurr = IJ.getImage();
			 impcurr.setOverlay(overlay);
			
			
			break;
		}
		
		default:
		{
			
			LinefinderMSER newlineMser = new LinefinderMSER(source, Preprocessedsource, minlength, framenumber);
			newlineMser.setMaxlines(40);

			SubpixelLengthPCLine MTline = new SubpixelLengthPCLine(source, newlineMser, psf, minlength, 0);
			MTline.checkInput();
			MTline.process();
			PrevFrameparam = MTline.getResult();
			Overlay overlay = newlineMser.getOverlay();
			 ImagePlus impcurr = IJ.getImage();
			 impcurr.setOverlay(overlay);
			
			
		}
		
			
		
		}
		
		return PrevFrameparam;

	}

	public static Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>> LinefindingMethodHF(final RandomAccessibleInterval<FloatType> source,
			final RandomAccessibleInterval<FloatType> Preprocessedsource,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> PrevFrameparam,
			final int minlength, final int framenumber, final double[] psf,  final LinefindingMethod findlines ) {

		Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>> returnVector = null;
		
		switch (findlines){
		
		case MSER:
		{
			LinefinderHFMSER newlinenextMser = new LinefinderHFMSER(source, Preprocessedsource, minlength, framenumber);
			newlinenextMser.setMaxlines(40);

			final SubpixelVelocityPCLine growthtracker = new SubpixelVelocityPCLine(source, newlinenextMser,
					PrevFrameparam.fst, PrevFrameparam.snd, psf, framenumber);
			growthtracker.checkInput();
			growthtracker.process();
			
			
			Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> NewFrameparam = growthtracker.getResult();
			ArrayList<Trackproperties> startStateVectors = growthtracker.getstartStateVectors();
			ArrayList<Trackproperties> endStateVectors = growthtracker.getendStateVectors();
			Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>> Statevectors = new Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>(startStateVectors, endStateVectors); 
			returnVector = 
					new Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>>(Statevectors, NewFrameparam);
			
			 Overlay overlay = newlinenextMser.getOverlay();
			 ImagePlus impcurr = IJ.getImage();
			 impcurr.setOverlay(overlay);
			
			
			break;
		}
			
		case Hough:
		{
			LinefinderHFHough newlineHough = new LinefinderHFHough(source, Preprocessedsource, minlength, framenumber);
			final SubpixelVelocityPCLine growthtracker = new SubpixelVelocityPCLine(source, newlineHough,
					PrevFrameparam.fst, PrevFrameparam.snd, psf, framenumber);
			growthtracker.checkInput();
			growthtracker.process();
			
			
			Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> NewFrameparam = growthtracker.getResult();
			ArrayList<Trackproperties> startStateVectors = growthtracker.getstartStateVectors();
			ArrayList<Trackproperties> endStateVectors = growthtracker.getendStateVectors();
			Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>> Statevectors = new Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>(startStateVectors, endStateVectors); 
			returnVector = 
					new Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>>(Statevectors, NewFrameparam);
			
			
			break;
		}
		case MSERwHough:
		{

			LinefinderHFMSERwHough newlinenextMser = new LinefinderHFMSERwHough(source, Preprocessedsource, minlength, framenumber);
			newlinenextMser.setMaxlines(40);

			final SubpixelVelocityPCLine growthtracker = new SubpixelVelocityPCLine(source, newlinenextMser,
					PrevFrameparam.fst, PrevFrameparam.snd, psf, framenumber);
			growthtracker.checkInput();
			growthtracker.process();
			
			
			

			Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> NewFrameparam = growthtracker.getResult();
			ArrayList<Trackproperties> startStateVectors = growthtracker.getstartStateVectors();
			ArrayList<Trackproperties> endStateVectors = growthtracker.getendStateVectors();
			Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>> Statevectors = new Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>(startStateVectors, endStateVectors); 
			returnVector = 
					new Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>>(Statevectors, NewFrameparam);
			 Overlay overlay = newlinenextMser.getOverlay();
			 ImagePlus impcurr = IJ.getImage();
			 impcurr.setOverlay(overlay);
			
			break;
		}
		
		default:
		{
			LinefinderHFMSER newlinenextMser = new LinefinderHFMSER(source, Preprocessedsource, minlength, framenumber);
			newlinenextMser.setMaxlines(40);

			final SubpixelVelocityPCLine growthtracker = new SubpixelVelocityPCLine(source, newlinenextMser,
					PrevFrameparam.fst, PrevFrameparam.snd, psf, framenumber);
			growthtracker.checkInput();
			growthtracker.process();
			
			
			Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>> NewFrameparam = growthtracker.getResult();
			ArrayList<Trackproperties> startStateVectors = growthtracker.getstartStateVectors();
			ArrayList<Trackproperties> endStateVectors = growthtracker.getendStateVectors();
			Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>> Statevectors = new Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>(startStateVectors, endStateVectors); 
			returnVector = 
					new Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>,Pair<ArrayList<Indexedlength>,ArrayList<Indexedlength>>>(Statevectors, NewFrameparam);
			
			 Overlay overlay = newlinenextMser.getOverlay();
			 ImagePlus impcurr = IJ.getImage();
			 impcurr.setOverlay(overlay);
			
			
			
			
		}
		
			
		
		}
		
		return returnVector;

	}
	
	
}
