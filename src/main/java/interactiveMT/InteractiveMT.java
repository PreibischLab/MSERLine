package interactiveMT;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.tools.javac.util.Pair;

import LineModels.UseLineModel.UserChoiceModel;
import drawandOverlay.PushCurves;
import fiji.tool.SliceListener;
import fiji.tool.SliceObserver;
import graphconstructs.Trackproperties;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.EllipseRoi;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import labeledObjects.CommonOutput;
import labeledObjects.CommonOutputHF;
import labeledObjects.Indexedlength;
import lineFinder.FindlinesVia;
import lineFinder.LinefinderHFHough;
import lineFinder.LinefinderHFMSER;
import lineFinder.LinefinderHFMSERwHough;
import lineFinder.LinefinderHough;
import lineFinder.LinefinderInteractiveMSER;
import lineFinder.LinefinderMSER;
import lineFinder.LinefinderMSERwHough;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.util.Util;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.componenttree.mser.Mser;
import net.imglib2.algorithm.componenttree.mser.MserTree;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import peakFitter.SortListbyproperty;
import preProcessing.MedianFilter2D;
import roiFinder.Roifinder;
import roiFinder.RoifinderMSER;

/**
 * An interactive tool for MT tracking using MSER and Hough Transform
 * 
 * @author Varun Kapoor
 */

public class InteractiveMT implements PlugIn {

	final int extraSize = 40;
	final int scrollbarSize = 1000;
	final int scrollbarSizebig = 1000;
	// steps per octave
	public static int standardSensitivity = 4;
	int sensitivity = standardSensitivity;
	float deltaMin = 1f;
	float deltaMax = 500f;
	float maxVarMin = 0;
	float maxVarMax = 1;
	boolean darktobright = false;
	long minSize = 1;
	long maxSize = 1000;
	long minSizemin = 0;
	long  minSizemax = 100;
	long maxSizemin = 100;
	long maxSizemax = 100000;
	
	
	float minDiversityMin = 0;
	float minDiversityMax = 1;
	
	float delta = 1f;
	int deltaInit = 10;
	int maxVarInit = 1;
	int minSizeInit = 1;
	int maxSizeInit = 100;
	
   
	
	public int minDiversityInit = 0;
	public int radius = 1;
	public long Size = 1;
	
	public  float maxVar = 1;
	public float minDiversity = 1;
	
	
	Color colorDraw = null;
	FloatType minval =  new FloatType(0);
	FloatType maxval = new FloatType(1);
	SliceObserver sliceObserver;
	RoiListener roiListener;
	boolean isComputing = false;
	boolean isStarted = false;
	boolean FindLinesViaMSER = false;
	boolean FindLinesViaHOUGH = false;
	boolean FindLinesViaMSERwHOUGH = false;
	boolean ShowMser = false;
	
	boolean RoisViaMSER = false;
	boolean RoisViaWatershed = false;
	
	boolean NormalizeImage = false;
	boolean Mediancurr = false;
	boolean MedianAll = false;
	boolean AutoDelta = false;
	int channel = 0;
	Img<FloatType> img;
	Img<FloatType> Preprocessedimg;
	
	MserTree<UnsignedByteType> newtree;
	// Image 2d at the current slice
	Img<FloatType> currentimg;
	Img<FloatType> currentPreprocessedimg;
	Color originalColor = new Color(0.8f, 0.8f, 0.8f);
	Color inactiveColor = new Color(0.95f, 0.95f, 0.95f);
	ImagePlus imp;
	ImagePlus preprocessedimp;
	final double[] psf;
	final int minlength;
	int stacksize;
	private int ndims;
	ArrayList<CommonOutputHF> output;
	public Rectangle standardRectangle;
	Img<UnsignedByteType> newimg;

	// first and last slice to process
	int slice2, currentframe;

	public static enum ValueChange {
		SLICE, ROI, ALL, DELTA, FindLinesVia, MAXVAR, MINDIVERSITY, DARKTOBRIGHT, MINSIZE, MAXSIZE, SHOWMSER;
	}

	boolean isFinished = false;
	boolean wasCanceled = false;

	public boolean isFinished() {
		return isFinished;
	}

	public boolean wasCanceled() {
		return wasCanceled;
	}

	public boolean getFindLinesViaMSER() {
		return FindLinesViaMSER;
	}

	public boolean getRoisViaMSER(){
		
		return RoisViaMSER;
	}
	
	public boolean getRoisViaWatershed(){
		
		return RoisViaWatershed;
	}
	
	public void setRoisViaMSER(final boolean RoisViaMSER){
		
		this.RoisViaMSER = RoisViaMSER;
	}
	
    public void setRoisViaWatershed(final boolean RoisViaWatershed){
		
		this.RoisViaWatershed = RoisViaWatershed;
	}
	
	public boolean getFindLinesViaHOUGH() {
		return FindLinesViaHOUGH;
	}

	public boolean getFindLinesViaMSERwHOUGH() {
		return FindLinesViaMSERwHOUGH;
	}

	public void setFindLinesViaMSER(final boolean FindLinesViaMSER) {
		this.FindLinesViaMSER = FindLinesViaMSER;
	}

	public void setFindLinesViaHOUGH(final boolean FindLinesViaHOUGH) {
		this.FindLinesViaHOUGH = FindLinesViaHOUGH;
	}

	public void setFindLinesViaMSERwHOUGH(final boolean FindLinesViaMSERwHOUGH) {
		this.FindLinesViaMSERwHOUGH = FindLinesViaMSERwHOUGH;
	}

	public void setInitialDelta(final float value) {
		delta = value;
		deltaInit = computeScrollbarPositionFromValue(delta, deltaMin, deltaMax, scrollbarSize);
	}



	public InteractiveMT(final ImagePlus imp, final ImagePlus preprocessedimp, final double[] psf,
			final int minlength) {
		this.imp = imp;
		this.preprocessedimp = preprocessedimp;
		this.psf = psf;
		this.minlength = minlength;
		ndims = imp.getNDimensions();
		standardRectangle = new Rectangle(20, 20, imp.getWidth() - 40, imp.getHeight() - 40);
		


	}

	@Override
	public void run(String arg) {
		stacksize = imp.getStackSize();
		output = new ArrayList<CommonOutputHF>();
		slice2 = stacksize;

		if (imp == null)
			imp = WindowManager.getCurrentImage();
		if (preprocessedimp == null)
			preprocessedimp = WindowManager.getCurrentImage();
		
		if (imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.COLOR_256) {
			IJ.log("Color images are not supported, please convert to 8, 16 or 32-bit grayscale");
			return;
		}

		Roi roi = preprocessedimp.getRoi();

		if (roi == null) {
			// IJ.log( "A rectangular ROI is required to define the area..." );
			preprocessedimp.setRoi(standardRectangle);
			roi = preprocessedimp.getRoi();
		}

		if (roi.getType() != Roi.RECTANGLE) {
			IJ.log("Only rectangular rois are supported...");
			return;
		}

		currentframe = preprocessedimp.getFrame() - 1;
		preprocessedimp.setPosition(preprocessedimp.getChannel(), preprocessedimp.getSlice(), preprocessedimp.getFrame());

		imp.setPosition(preprocessedimp.getChannel(), preprocessedimp.getSlice(), preprocessedimp.getFrame());
		img = ImageJFunctions.convertFloat(imp.duplicate());
		Preprocessedimg = ImageJFunctions.convertFloat(preprocessedimp.duplicate());
	

				
		int z = currentframe;
		
		
		// copy the ImagePlus into an ArrayImage<FloatType> for faster access
		displaySliders();
		// add listener to the imageplus slice slider
				sliceObserver = new SliceObserver(preprocessedimp, new ImagePlusListener());
				// compute first version#
		updatePreview(ValueChange.ALL);
		isStarted = true;
		
		// check whenever roi is modified to update accordingly
				roiListener = new RoiListener();
				preprocessedimp.getCanvas().addMouseListener(roiListener);

	}
	
	
	

	/**
	 * Updates the Preview with the current parameters (sigma, threshold, roi,
	 * slicenumber)
	 * 
	 * @param change
	 *            - what did change
	 */

	protected void updatePreview(final ValueChange change) {

		// check if Roi changed
		boolean roiChanged = false;
		Roi roi = preprocessedimp.getRoi();
		if (roi == null || roi.getType() != Roi.RECTANGLE) {
			preprocessedimp.setRoi(new Rectangle(standardRectangle));
			roi = preprocessedimp.getRoi();
			roiChanged = true;
		}

		final Rectangle rect = roi.getBounds();
		if (roiChanged || currentimg == null || currentPreprocessedimg == null || newimg == null  ||
				change == ValueChange.SLICE || rect.getMinX() != standardRectangle.getMinX()
				|| rect.getMaxX() != standardRectangle.getMaxX() || rect.getMinY() != standardRectangle.getMinY()
				|| rect.getMaxY() != standardRectangle.getMaxY()) {
			standardRectangle = rect;
			if (ndims == 2){
				
				currentimg = extractImage(img, standardRectangle, extraSize);
				currentPreprocessedimg = extractImage(Preprocessedimg, standardRectangle, extraSize);
				newimg = extractImageUnsignedByteType(Preprocessedimg, standardRectangle, extraSize);
				roiChanged = true;
				
			}
			if (ndims > 2){
			currentimg = extractImage(Views.hyperSlice(img, ndims - 1, currentframe  ), standardRectangle, extraSize);
			currentPreprocessedimg = extractImage(Views.hyperSlice(Preprocessedimg, ndims - 1, currentframe  ), standardRectangle, extraSize);
			newimg = extractImageUnsignedByteType(Views.hyperSlice(Preprocessedimg, ndims - 1, currentframe  ), standardRectangle, extraSize);
			roiChanged = true;
			}
			
		}

		// if we got some mouse click but the ROI did not change we can return
		if (!roiChanged && change == ValueChange.ROI) {
			isComputing = false;
			return;
		}
		
		
			
			
		
		
		// Re-compute MSER ellipses if neccesary
				ArrayList<EllipseRoi> Rois = new ArrayList<EllipseRoi>();
				RoiManager roimanager = RoiManager.getInstance();

				if (roimanager == null) {
					roimanager = new RoiManager();
				}
				if (change == ValueChange.SHOWMSER ){

					MouseEvent mev = new MouseEvent(preprocessedimp.getCanvas(), MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 0, 0,
							1, false);
					
					if (mev != null) {

						roimanager.close();

						roimanager = new RoiManager();

						
					}
			
					
					IJ.log(" Computing the Component tree");
					 newtree = MserTree.buildMserTree(newimg, delta, minSize, maxSize, maxVar,
							minDiversity, darktobright);
						Rois = getcurrentRois(newtree);
						
				
						Overlay o = preprocessedimp.getOverlay();

						if (o == null) {
							o = new Overlay();
							preprocessedimp.setOverlay(o);
						}

						o.clear();
						
						
						
						
			           for (int index = 0; index < Rois.size(); ++index) {
							
							EllipseRoi or = Rois.get(index);
							
							or.setStrokeColor(Color.red);
							o.add(or);
							roimanager.addRoi(or);
						}
						
					
				}
				
			
			
				preprocessedimp.updateAndDraw();

				isComputing = false;
		
	
	
		
	}

	public void displaySliders() {

		final Frame frame = new Frame("Find MT's and Track");
		frame.setSize(550, 550);
		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		final Checkbox Normalize = new Checkbox("Normailze Image Intensity (recommended)", NormalizeImage);
		final Checkbox MedFiltercur = new Checkbox("Apply Median Filter to current Frame", Mediancurr);
		final Checkbox MedFilterAll = new Checkbox("Apply Median Filter to Stack", MedianAll);
		CheckboxGroup Finders = new CheckboxGroup();
		
		
		
		final Label MTText = new Label("Step 1) Choose method to do sub-pixel localization ", Label.CENTER);
		final Button FindLinesListener = new Button("Find Lines in current Frame:");
		final Checkbox mser = new Checkbox("MSER",Finders, FindLinesViaMSER );
		final Checkbox hough = new Checkbox("HOUGH", Finders, FindLinesViaHOUGH );
		final Checkbox mserwhough = new Checkbox("MSERwHOUGH",Finders, FindLinesViaMSERwHOUGH );
       
       
		/* Location */
		frame.setLayout(layout);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;

		
		c.insets = new Insets(10, 10, 0, 0);
		frame.add(Normalize, c);
		
		++c.gridy;
		c.insets = new Insets(10, 10, 0, 0);
		frame.add(MedFiltercur, c);
		
		++c.gridy;
		c.insets = new Insets(10, 10, 0, 0);
		frame.add(MedFilterAll, c);
		
		++c.gridy;
		frame.add(MTText, c);
		
		
		
		++c.gridy;
		c.insets = new Insets(10, 10, 0, 0);
		frame.add(mser, c);
	
		++c.gridy;
		c.insets = new Insets(10, 10, 0, 0);
		frame.add(hough, c);
		
		++c.gridy;
		c.insets = new Insets(10, 10, 0, 0);
		frame.add(mserwhough, c);
		++c.gridy;
		c.insets = new Insets(10, 160, 0,160);
		frame.add(FindLinesListener, c);
		
		
		
		
		
		Normalize.addItemListener(new NormalizeListener());
		MedFiltercur.addItemListener(new MediancurrListener() );
		MedFilterAll.addItemListener(new MedianAllListener() );
		FindLinesListener.addActionListener(new FindLinesListener());
		mser.addItemListener(new MserListener());
		hough.addItemListener(new HoughListener());
		mserwhough.addItemListener(new MserwHoughListener());
		
		
		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);
		MTText.setFont(MTText.getFont().deriveFont(Font.BOLD));
	
	}
	
	
	
	protected class FindLinesListener implements ActionListener {
		
		@Override
		public void actionPerformed(final ActionEvent arg0) {

			// add listener to the imageplus slice slider
			sliceObserver = new SliceObserver(preprocessedimp, new ImagePlusListener());

			preprocessedimp.setSlice(1);
			
			IJ.log("Starting Chosen Line finder");
			
			
			
			currentframe = 1;

			if (preprocessedimp.getType() == ImagePlus.COLOR_RGB || preprocessedimp.getType() == ImagePlus.COLOR_256) {
				IJ.log("Color images are not supported, please convert to 8, 16 or 32-bit grayscale");
				return;
			}

			Roi roi = preprocessedimp.getRoi();
			if (roi == null) {
				// IJ.log( "A rectangular ROI is required to define the area..."
				// );
				preprocessedimp.setRoi(standardRectangle);
				roi = preprocessedimp.getRoi();
			}
			updatePreview(ValueChange.ALL);
			if (ndims == 2) {
				Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> PrevFrameparam = null;
				if (FindLinesViaMSER) {

					LinefinderMSER newlineMser = new LinefinderMSER(img, Preprocessedimg, minlength, 0);

					Overlay overlay = newlineMser.getOverlay();
					ImagePlus impcurr = IJ.getImage();
					impcurr.setOverlay(overlay);
					PrevFrameparam = FindlinesVia.LinefindingMethod(img, Preprocessedimg, minlength, 0, psf, newlineMser,
							UserChoiceModel.Line);

				}
				if (FindLinesViaHOUGH) {

					LinefinderHough newlineHough = new LinefinderHough(img, Preprocessedimg, minlength, 0);

					PrevFrameparam = FindlinesVia.LinefindingMethod(img, Preprocessedimg, minlength, 0, psf, newlineHough,
							UserChoiceModel.Line);

				}

				if (FindLinesViaMSERwHOUGH) {

					LinefinderMSERwHough newlineMserwHough = new LinefinderMSERwHough(img, Preprocessedimg, minlength, 0);
					Overlay overlay = newlineMserwHough.getOverlay();
					ImagePlus impcurr = IJ.getImage();
					impcurr.setOverlay(overlay);
					PrevFrameparam = FindlinesVia.LinefindingMethod(img, Preprocessedimg, minlength, 0, psf,
							newlineMserwHough, UserChoiceModel.Line);

				}
				// Draw the detected lines
				RandomAccessibleInterval<FloatType> gaussimg = new ArrayImgFactory<FloatType>().create(img,
						new FloatType());
				PushCurves.DrawstartLine(gaussimg, PrevFrameparam.fst, PrevFrameparam.snd, psf);
				ImageJFunctions.show(gaussimg).setTitle("Exact-line-start");

			} 
			
			
			if (ndims > 2) {
				
				RandomAccessibleInterval<FloatType> groundframe = currentimg;
				RandomAccessibleInterval<FloatType> groundframepre = currentPreprocessedimg;

				Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> PrevFrameparam = null;
				if (FindLinesViaMSER) {
					

					LinefinderInteractiveMSER newlineMser = new LinefinderInteractiveMSER(groundframe, groundframepre, newtree, minlength, currentframe );
					
					PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength, currentframe , psf, newlineMser,
							UserChoiceModel.Line);

				
				}

				if (FindLinesViaHOUGH) {
					LinefinderHough newlineHough = new LinefinderHough(groundframe, groundframepre, minlength, currentframe );

					PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength, currentframe , psf, newlineHough,
							UserChoiceModel.Line);
					
				}

				if (FindLinesViaMSERwHOUGH) {
					LinefinderMSERwHough newlineMserwHough = new LinefinderMSERwHough(groundframe, groundframepre, minlength, currentframe);
					newlineMserwHough.setDelta(delta);
					PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength, currentframe, psf,
							newlineMserwHough, UserChoiceModel.Line);

				
				}
				
			
		}
		
	}
	}
	
    protected class NormalizeListener implements ItemListener{

		@Override
		public void itemStateChanged(ItemEvent arg0) {
              
               if (arg0.getStateChange() == ItemEvent.DESELECTED)
            	   NormalizeImage = false;
               else if (arg0.getStateChange() == ItemEvent.SELECTED){
            	   
            	   NormalizeImage = true;
            	   
            	  
            	DialogueNormalize();
            	   
            	new Normalize();
            	IJ.log("Image Stack Intnesity Normalized between: " 
            	+ (int)minval.get() + " and " + (int)maxval.get());
           		Normalize.normalize(Views.iterable(img), minval, maxval);
           		Normalize.normalize(Views.iterable(Preprocessedimg), minval, maxval);
            	
               }
		}
    	
    	
    }
    
    protected class MediancurrListener implements ItemListener{

  		@Override
  		public void itemStateChanged(ItemEvent arg0) {
                
                 if (arg0.getStateChange() == ItemEvent.DESELECTED)
              	   Mediancurr = false;
                 else if (arg0.getStateChange() == ItemEvent.SELECTED){
              	   
              	  Mediancurr = true;
              	   
              	  
              	DialogueMedian();
              	   
              	IJ.log(" Applying Median Filter to current Image" );
              	
              	final MedianFilter2D<FloatType> medfilter = new MedianFilter2D<FloatType>(currentPreprocessedimg, radius);
    			medfilter.process();
    		   currentPreprocessedimg = medfilter.getResult();
              	
              	
                 }
  		}
      	
      	
      }
    
    protected class MedianAllListener implements ItemListener{

  		@Override
  		public void itemStateChanged(ItemEvent arg0) {
                
                 if (arg0.getStateChange() == ItemEvent.DESELECTED)
              	   MedianAll = false;
                 else if (arg0.getStateChange() == ItemEvent.SELECTED){
              	   
              	  MedianAll = true;
              	   
              	  
              	DialogueMedian();
              	   
              	IJ.log(" Applying Median Filter to the whole stack (takes some time)"  );
              	
              	final MedianFilter2D<FloatType> medfilter = new MedianFilter2D<FloatType>(Preprocessedimg, radius);
    			medfilter.process();
    			Preprocessedimg = medfilter.getResult();
              	
              	
                 }
  		}
      	
      	
      }
    
	
	protected class MserListener implements ItemListener {
		@Override
		public void itemStateChanged(final ItemEvent arg0) {
			boolean oldState = FindLinesViaMSER;

			if (arg0.getStateChange() == ItemEvent.DESELECTED)
				FindLinesViaMSER= false;
			else if (arg0.getStateChange() == ItemEvent.SELECTED){
			
				FindLinesViaMSER = true;
				FindLinesViaHOUGH = false;
				FindLinesViaMSERwHOUGH= false;
				DisplayMSER();
				
				
			}

			if (FindLinesViaMSER != oldState) {
				while (isComputing)
					SimpleMultiThreading.threadWait(10);

				updatePreview(ValueChange.FindLinesVia);
			}
		}
	}
	
	
	
	
	
	protected class HoughListener implements ItemListener {
		@Override
		public void itemStateChanged(final ItemEvent arg0) {
			boolean oldState = FindLinesViaHOUGH;

			if (arg0.getStateChange() == ItemEvent.DESELECTED)
				FindLinesViaHOUGH= false;
			else if (arg0.getStateChange() == ItemEvent.SELECTED){
			
				
				FindLinesViaHOUGH = true;
				FindLinesViaMSER= false;
				FindLinesViaMSERwHOUGH= false;
			}

			if (FindLinesViaHOUGH != oldState) {
				while (isComputing)
					SimpleMultiThreading.threadWait(10);

				updatePreview(ValueChange.FindLinesVia);
			}
		}
	}
	
	protected class MserwHoughListener implements ItemListener {
		@Override
		public void itemStateChanged(final ItemEvent arg0) {
			boolean oldState = FindLinesViaMSERwHOUGH;

			if (arg0.getStateChange() == ItemEvent.DESELECTED)
				FindLinesViaMSERwHOUGH= false;
			else if (arg0.getStateChange() == ItemEvent.SELECTED){
			
				
				FindLinesViaMSERwHOUGH = true;
				FindLinesViaMSER= false;
				FindLinesViaHOUGH= false;
			}

			if (FindLinesViaMSERwHOUGH != oldState) {
				while (isComputing)
					SimpleMultiThreading.threadWait(10);

				updatePreview(ValueChange.FindLinesVia);
			}
		}
	}
	
	
	
	private void DisplayMSER(){
		
		// Create dialog
		final Frame frame = new Frame("Interactive Mser");
		frame.setSize(550, 550);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		
				
				final Scrollbar delta = new Scrollbar(Scrollbar.HORIZONTAL, deltaInit, 10, 0, 10 + scrollbarSize);
				final Scrollbar maxVar = new Scrollbar(Scrollbar.HORIZONTAL, maxVarInit, 10, 0, 10 + scrollbarSize);
				final Scrollbar minDiversity = new Scrollbar(Scrollbar.HORIZONTAL, minDiversityInit, 10, 0, 10 + scrollbarSize);
				final Scrollbar minSize= new Scrollbar(Scrollbar.HORIZONTAL, minSizeInit, 10, 0, 10 + scrollbarSize);
				final Scrollbar maxSize = new Scrollbar(Scrollbar.HORIZONTAL, maxSizeInit, 10, 0, 10 + scrollbarSize);
				final Button ComputeTree = new Button("Compute Tree and display");
				
				this.maxVar = computeValueFromScrollbarPosition(maxVarInit, maxVarMin, maxVarMax, scrollbarSize);
				this.delta = computeValueFromScrollbarPosition(deltaInit, deltaMin, deltaMax, scrollbarSize);
				this.minDiversity = computeValueFromScrollbarPosition(minDiversityInit, minDiversityMin, minDiversityMax, scrollbarSize);
				this.minSize = (int)computeValueFromScrollbarPosition(minSizeInit, minSizemin, minSizemax, scrollbarSize);
				this.maxSize = (int)computeValueFromScrollbarPosition(maxSizeInit, maxSizemin, maxSizemax, scrollbarSize);
				
				final Checkbox min = new Checkbox("Look for Minima ", darktobright);
				

				
				final Label deltaText = new Label("delta = " + this.delta, Label.CENTER);
				final Label maxVarText = new Label("maxVar = " + this.maxVar, Label.CENTER);
				final Label minDiversityText = new Label("minDiversity = " + this.minDiversity, Label.CENTER);
				final Label minSizeText = new Label("MinSize = " + this.minSize, Label.CENTER);
				final Label maxSizeText = new Label("MaxSize = " + this.maxSize, Label.CENTER);
				/* Location */
				frame.setLayout(layout);

				c.fill = GridBagConstraints.HORIZONTAL;
				c.gridx = 0;
				c.gridy = 0;
				c.weightx = 1;
				
				frame.add(deltaText, c);
				
				++c.gridy;
				frame.add(delta, c);
				
				++c.gridy;
				
               frame.add(maxVarText, c);
				
				++c.gridy;
				frame.add(maxVar, c);
				
				++c.gridy;
				
	            frame.add(minDiversityText, c);
					
				++c.gridy;
				frame.add(minDiversity, c);
				
	        
				
				
				++c.gridy;
					
		        frame.add(minSizeText, c);
						
			    ++c.gridy;
			    frame.add(minSize, c);
			    
			    ++c.gridy;
				
		        frame.add(maxSizeText, c);
						
			    ++c.gridy;
			    frame.add(maxSize, c);
			    
				++c.gridy;
				c.insets = new Insets(10, 175, 0, 175);
				frame.add(min, c);

			
				
				++c.gridy;
				c.insets = new Insets(10, 175, 0, 175);
				frame.add(ComputeTree, c);
				
				delta.addAdjustmentListener(
						new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta));
				
				
				
				
				maxVar.addAdjustmentListener(
						new maxVarListener(maxVarText, maxVarMin, maxVarMax, scrollbarSize, maxVar));
				
				
				
			
				
				minDiversity.addAdjustmentListener(
						new minDiversityListener(minDiversityText, minDiversityMin, minDiversityMax, scrollbarSize, minDiversity));
				
				
		
				
			
				
				minSize.addAdjustmentListener(
						new minSizeListener(minSizeText, minSizemin, minSizemax, scrollbarSize, minSize));
				
				
				
				
				maxSize.addAdjustmentListener(
						new maxSizeListener(maxSizeText, maxSizemin, maxSizemax, scrollbarSize, maxSize));
				
				
				delta.addAdjustmentListener(new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta));
				maxVar.addAdjustmentListener(new maxVarListener(maxVarText, maxVarMin, maxVarMax, scrollbarSize, maxVar));
				minDiversity.addAdjustmentListener(new minDiversityListener(minDiversityText, minDiversityMin, minDiversityMax, scrollbarSize, minDiversity));
				minSize.addAdjustmentListener(new minSizeListener(minSizeText, minSizemin, minSizemax, scrollbarSize, minSize));
				maxSize.addAdjustmentListener(new maxSizeListener(maxSizeText, maxSizemin, maxSizemax, scrollbarSize, maxSize));
				min.addItemListener(new DarktobrightListener());
				ComputeTree.addActionListener(new ComputeTreeListener());
				frame.addWindowListener(new FrameListener(frame));

				frame.setVisible(true);

				originalColor = delta.getBackground();
				
			
		
	}
	
	protected class ComputeTreeListener implements ActionListener {
	

		@Override
		public void actionPerformed(ActionEvent arg0) {
			ShowMser = true;
			updatePreview(ValueChange.SHOWMSER);
			
		}
	}
	
	protected class DarktobrightListener implements ItemListener {
		@Override
		public void itemStateChanged(final ItemEvent arg0) {
			boolean oldState = darktobright;

			if (arg0.getStateChange() == ItemEvent.DESELECTED)
				darktobright = false;
			else if (arg0.getStateChange() == ItemEvent.SELECTED)
				darktobright = true;

			if (darktobright != oldState) {
				while (isComputing)
					SimpleMultiThreading.threadWait(10);

				updatePreview(ValueChange.DARKTOBRIGHT);
			}
		}
	}
	
	
	
	protected class FrameListener extends WindowAdapter {
		final Frame parent;

		public FrameListener(Frame parent) {
			super();
			this.parent = parent;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			close(parent, sliceObserver, preprocessedimp, roiListener);
		}
	}
	
	
	protected class DeltaListener implements AdjustmentListener {
		final Label label;
		final float min, max;
		final int scrollbarSize;

		final Scrollbar deltaScrollbar;

		public DeltaListener(final Label label, final float min, final float max, final int scrollbarSize,
				final Scrollbar deltaScrollbar) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;

			this.deltaScrollbar = deltaScrollbar;
			
		}
		
		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			delta = computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize);

			
				deltaScrollbar.setValue(computeScrollbarPositionFromValue(delta, min, max, scrollbarSize));
			

			label.setText("Delta = " + delta);

			// if ( !event.getValueIsAdjusting() )
			{
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.DELTA);
			}
		}
	}
	
	protected class minSizeListener implements AdjustmentListener {
		final Label label;
		final float min, max;
		final int scrollbarSize;

		final Scrollbar minsizeScrollbar;

		public minSizeListener(final Label label, final float min, final float max, final int scrollbarSize,
				final Scrollbar minsizeScrollbar) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;

			this.minsizeScrollbar = minsizeScrollbar;
			
		}
		
		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			minSize = (int) computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize);

			
				minsizeScrollbar.setValue(computeScrollbarPositionFromValue(minSize, min, max, scrollbarSize));
			

			label.setText("MinSize = " + minSize);

			// if ( !event.getValueIsAdjusting() )
			{
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.MINSIZE);
			}
		}
	}
	
	protected class maxSizeListener implements AdjustmentListener {
		final Label label;
		final float min, max;
		final int scrollbarSize;

		final Scrollbar maxsizeScrollbar;

		public maxSizeListener(final Label label, final float min, final float max, final int scrollbarSize,
				final Scrollbar maxsizeScrollbar) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;

			this.maxsizeScrollbar = maxsizeScrollbar;
			
		}
		
		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			maxSize = (int) computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize);

			
				maxsizeScrollbar.setValue(computeScrollbarPositionFromValue(maxSize, min, max, scrollbarSize));
			

			label.setText("MaxSize = " + maxSize);

			// if ( !event.getValueIsAdjusting() )
			{
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.MAXSIZE);
			}
		}
	}
	
	
	protected class maxVarListener implements AdjustmentListener {
		final Label label;
		final float min, max;
		final int scrollbarSize;

		final Scrollbar maxVarScrollbar;

		public maxVarListener(final Label label, final float min, final float max, final int scrollbarSize,
				final Scrollbar maxVarScrollbar) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;
			this.maxVarScrollbar = maxVarScrollbar;
			
		}
		
		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			maxVar = (computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize));

			
				maxVarScrollbar.setValue(computeScrollbarPositionFromValue((float)maxVar, min, max, scrollbarSize));
			

			label.setText("MaxVar = " + maxVar);

			// if ( !event.getValueIsAdjusting() )
			{
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.MAXVAR);
			}
		}
	}
	
	protected class minDiversityListener implements AdjustmentListener {
		final Label label;
		final float min, max;
		final int scrollbarSize;

		final Scrollbar minDiversityScrollbar;

		public minDiversityListener(final Label label, final float min, final float max, final int scrollbarSize,
				final Scrollbar minDiversityScrollbar) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;
			this.minDiversityScrollbar = minDiversityScrollbar;
			
		}
		
		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			minDiversity = (computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize));

			
				minDiversityScrollbar.setValue(computeScrollbarPositionFromValue((float)minDiversity, min, max, scrollbarSize));
			

			label.setText("MinDiversity = " + minDiversity);

			// if ( !event.getValueIsAdjusting() )
			{
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.MINDIVERSITY);
			}
		}
	}
	
	
	public  ArrayList<EllipseRoi> getcurrentRois( MserTree<UnsignedByteType> newtree){
		
		final HashSet<Mser<UnsignedByteType>> rootset = newtree.roots();
		
		ArrayList<EllipseRoi> Allrois = new ArrayList<EllipseRoi>();
		final Iterator<Mser<UnsignedByteType>> rootsetiterator = rootset.iterator();
		
		ArrayList<double[]> ellipselist = new ArrayList<double[]>();
		ArrayList<double[]> meanandcovlist = new ArrayList<double[]>();
		
		
		while (rootsetiterator.hasNext()) {

			Mser<UnsignedByteType> rootmser = rootsetiterator.next();

			if (rootmser.size() > 0) {

				final double[] meanandcov = { rootmser.mean()[0], rootmser.mean()[1], rootmser.cov()[0],
						rootmser.cov()[1], rootmser.cov()[2] };
				meanandcovlist.add(meanandcov);
				ellipselist.add(meanandcov);

			}
		}
		
		// We do this so the ROI remains attached the the same label and is not changed if the program is run again
	       SortListbyproperty.sortpointList(ellipselist);
			for (int index = 0; index < ellipselist.size(); ++index) {
				
			
				
				final double[] mean = { ellipselist.get(index)[0], ellipselist.get(index)[1] };
				final double[] covar = { ellipselist.get(index)[2], ellipselist.get(index)[3],
						ellipselist.get(index)[4] };
				
				
				EllipseRoi roi = createEllipse(mean, covar, 3);
		        Allrois.add(roi);
			}
			
			return Allrois;
		
	}
	
	public boolean DialogueNormalize() {
		// Create dialog
		GenericDialog gd = new GenericDialog("Image Intensity Normalization");
		gd.addNumericField("Mininum Value for Intensity Normalization:", minval.get(), 0);
		gd.addNumericField("Maximum Value for Intensity Normalization:", maxval.get(), 0);
		gd.showDialog();
		minval = new FloatType((int)gd.getNextNumber());
		maxval = new FloatType((int)gd.getNextNumber());
		
	
		
		
		return !gd.wasCanceled();
	}

	
	public boolean DialogueMedian() {
		// Create dialog
		GenericDialog gd = new GenericDialog("Choose the radius of the filter");
		gd.addNumericField("Radius:", radius, 0);
		gd.showDialog();
		radius = ((int)gd.getNextNumber());
		
	
		
		
		return !gd.wasCanceled();
	}
	
	/**
	 * Extract the current 2d region of interest from the souce image
	 * 
	 * @param intervalView
	 *            - the source image, a {@link Image} which is a copy of the
	 *            {@link ImagePlus}
	 * @param rectangle
	 *            - the area of interest
	 * @param extraSize
	 *            - the extra size around so that detections at the border of
	 *            the roi are not messed up
	 * @return
	 */
	protected Img<FloatType> extractImage(final IntervalView<FloatType> intervalView,
			final Rectangle rectangle, final int extraSize) {
		
		final FloatType type = intervalView.randomAccess().get().createVariable();
		final ImgFactory<FloatType> factory = net.imglib2.util.Util.getArrayOrCellImgFactory(intervalView, type);
		final Img<FloatType> img = factory.create(new int[] { rectangle.width + extraSize, rectangle.height + extraSize },
				type);
		

		final int offsetX = rectangle.x - extraSize / 2;
		final int offsetY = rectangle.y - extraSize / 2;

		final int[] location = new int[intervalView.numDimensions()];

		if (location.length > 2)
			location[2] = (preprocessedimp.getCurrentSlice() - 1) / preprocessedimp.getNChannels();

		final Cursor<FloatType> cursor = img.localizingCursor();
		final RandomAccess<net.imglib2.type.numeric.real.FloatType> positionable;

		if (offsetX >= 0 && offsetY >= 0 && offsetX + img.dimension(0) < intervalView.dimension(0)
				&& offsetY + img.dimension(1) < intervalView.dimension(1)) {
			// it is completely inside so we need no outofbounds for copying
			positionable = intervalView.randomAccess();
		} else {
			positionable = Views.extendMirrorSingle(intervalView).randomAccess();
		}

		while (cursor.hasNext()) {
			cursor.fwd();
			
			cursor.localize(location);

			location[0] += offsetX;
			location[1] += offsetY;

			positionable.setPosition(location);

			cursor.get().set(positionable.get().get());
		}
	
		return img;
	}
	protected Img<FloatType> extractImage(final Img<FloatType> intervalView,
			final Rectangle rectangle, final int extraSize) {
		
		final FloatType type = intervalView.randomAccess().get().createVariable();
		final ImgFactory<FloatType> factory = net.imglib2.util.Util.getArrayOrCellImgFactory(intervalView, type);
		final Img<FloatType> img = factory.create(new int[] { rectangle.width + extraSize, rectangle.height + extraSize },
				type);
		

		final int offsetX = rectangle.x - extraSize / 2;
		final int offsetY = rectangle.y - extraSize / 2;

		final int[] location = new int[intervalView.numDimensions()];

		if (location.length > 2)
			location[2] = (preprocessedimp.getCurrentSlice() - 1) / preprocessedimp.getNChannels();

		final Cursor<FloatType> cursor = img.localizingCursor();
		final RandomAccess<net.imglib2.type.numeric.real.FloatType> positionable;

		if (offsetX >= 0 && offsetY >= 0 && offsetX + img.dimension(0) < intervalView.dimension(0)
				&& offsetY + img.dimension(1) < intervalView.dimension(1)) {
			// it is completely inside so we need no outofbounds for copying
			positionable = intervalView.randomAccess();
		} else {
			positionable = Views.extendMirrorSingle(intervalView).randomAccess();
		}

		while (cursor.hasNext()) {
			cursor.fwd();
			
			cursor.localize(location);

			location[0] += offsetX;
			location[1] += offsetY;

			positionable.setPosition(location);

			cursor.get().set(positionable.get().get());
		}

		return img;
	}
	
	/**
	 * Extract the current 2d region of interest from the souce image
	 * 
	 * @param intervalView
	 *            - the source image, a {@link Image} which is a copy of the
	 *            {@link ImagePlus}
	 * @param rectangle
	 *            - the area of interest
	 * @param extraSize
	 *            - the extra size around so that detections at the border of
	 *            the roi are not messed up
	 * @return
	 */
	protected Img<UnsignedByteType> extractImageUnsignedByteType(final IntervalView<FloatType> intervalView,
			final Rectangle rectangle, final int extraSize) {
		
		final UnsignedByteType type = new UnsignedByteType();
		final ImgFactory<UnsignedByteType> factory = net.imglib2.util.Util.getArrayOrCellImgFactory(intervalView, type);
		final Img<UnsignedByteType> img = factory.create(new int[] { rectangle.width + extraSize, rectangle.height + extraSize },
				type);
		

		Normalize.normalize(intervalView, new FloatType(0), new FloatType(255));
		final int offsetX = rectangle.x - extraSize / 2;
		final int offsetY = rectangle.y - extraSize / 2;

		final int[] location = new int[intervalView.numDimensions()];

		if (location.length > 2)
			location[2] = (preprocessedimp.getCurrentSlice() - 1) / preprocessedimp.getNChannels();

		final Cursor<UnsignedByteType> cursor = img.localizingCursor();
		final RandomAccess<FloatType> positionable;

		if (offsetX >= 0 && offsetY >= 0 && offsetX + img.dimension(0) < intervalView.dimension(0)
				&& offsetY + img.dimension(1) < intervalView.dimension(1)) {
			// it is completely inside so we need no outofbounds for copying
			positionable = intervalView.randomAccess();
		} else {
			positionable = Views.extendMirrorSingle(intervalView).randomAccess();
		}

		while (cursor.hasNext()) {
			cursor.fwd();
			
			cursor.localize(location);

			location[0] += offsetX;
			location[1] += offsetY;

			positionable.setPosition(location);

			cursor.get().set( (int)positionable.get().get());
		}

		return img;
	}
	protected Img<UnsignedByteType> extractImageUnsignedByteType(final Img<FloatType> intervalView,
			final Rectangle rectangle, final int extraSize) {
		
		final UnsignedByteType type = new UnsignedByteType();
		final ImgFactory<UnsignedByteType> factory = net.imglib2.util.Util.getArrayOrCellImgFactory(intervalView, type);
		final Img<UnsignedByteType> img = factory.create(new int[] { rectangle.width + extraSize, rectangle.height + extraSize },
				type);
		
		Normalize.normalize(intervalView, new FloatType(0), new FloatType(255));
		final int offsetX = rectangle.x - extraSize / 2;
		final int offsetY = rectangle.y - extraSize / 2;

		final int[] location = new int[intervalView.numDimensions()];

		if (location.length > 2)
			location[2] = (preprocessedimp.getCurrentSlice() - 1) / preprocessedimp.getNChannels();

		final Cursor<UnsignedByteType> cursor = img.localizingCursor();
		final RandomAccess<FloatType> positionable;

		if (offsetX >= 0 && offsetY >= 0 && offsetX + img.dimension(0) < intervalView.dimension(0)
				&& offsetY + img.dimension(1) < intervalView.dimension(1)) {
			// it is completely inside so we need no outofbounds for copying
			positionable = intervalView.randomAccess();
		} else {
			positionable = Views.extendMirrorSingle(intervalView).randomAccess();
		}

		while (cursor.hasNext()) {
			cursor.fwd();
			
			cursor.localize(location);

			location[0] += offsetX;
			location[1] += offsetY;

			positionable.setPosition(location);

			cursor.get().set((int)positionable.get().get());
		}

		return img;
	}
	
	
	protected final void close(final Frame parent, final SliceObserver sliceObserver, final ImagePlus imp, RoiListener roiListener) {
		if (parent != null)
			parent.dispose();

		if (sliceObserver != null)
			sliceObserver.unregister();

		if (imp != null) {
			if (roiListener != null)
				imp.getCanvas().removeMouseListener(roiListener);
			imp.getOverlay().clear();
			imp.updateAndDraw();
		}

		isFinished = true;
	}

	/**
	 * Tests whether the ROI was changed and will recompute the preview
	 * 
	 * @author Stephan Preibisch
	 */
	protected class RoiListener implements MouseListener {
		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			// here the ROI might have been modified, let's test for that
			final Roi roi = preprocessedimp.getRoi();

			if (roi == null || roi.getType() != Roi.RECTANGLE)
				return;

			while (isComputing)
				SimpleMultiThreading.threadWait(10);

			updatePreview(ValueChange.ROI);
		}

	}
	
	

	protected static float computeValueFromScrollbarPosition(final int scrollbarPosition, final float min,
			final float max, final int scrollbarSize) {
		return min + (scrollbarPosition / (float) scrollbarSize) * (max - min);
	}

	protected static int computeScrollbarPositionFromValue(final float sigma, final float min, final float max,
			final int scrollbarSize) {
		return Util.round(((sigma - min) / (max - min)) * scrollbarSize);
	}

	
	public static FloatImagePlus<net.imglib2.type.numeric.real.FloatType> createImgLib2(final List<float[]> img,
			final int w, final int h) {
		final ImagePlus imp;

		if (img.size() > 1) {
			final ImageStack stack = new ImageStack(w, h);
			for (int z = 0; z < img.size(); ++z)
				stack.addSlice(new FloatProcessor(w, h, img.get(z)));
			imp = new ImagePlus("ImgLib2 FloatImagePlus (3d)", stack);
		} else {
			imp = new ImagePlus("ImgLib2 FloatImagePlus (2d)", new FloatProcessor(w, h, img.get(0)));
		}

		return ImagePlusAdapter.wrapFloat(imp);
	}
	protected class ImagePlusListener implements SliceListener {
		@Override
		public void sliceChanged(ImagePlus arg0) {
			if (isStarted) {
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.SLICE);

			}
		}
	}
	
	/**
     * Generic, type-agnostic method to create an identical copy of an Img
     *
     * @param input - the Img to copy
     * @return - the copy of the Img
     */
    public < T extends Type< T > > Img< T > copyImage( final Img< T > input )
    {
        // create a new Image with the same properties
        // note that the input provides the size for the new image as it implements
        // the Interval interface
        Img< T > output = input.factory().create( input, input.firstElement() );
 
        // create a cursor for both images
        Cursor< T > cursorInput = input.cursor();
        Cursor< T > cursorOutput = output.cursor();
 
        // iterate over the input
        while ( cursorInput.hasNext())
        {
            // move both cursors forward by one pixel
            cursorInput.fwd();
            cursorOutput.fwd();
 
            // set the value of this pixel of the output image to the same as the input,
            // every Type supports T.set( T type )
            cursorOutput.get().set( cursorInput.get() );
        }
 
        // return the copy
        return output;
    }
    /**
	 * 2D correlated Gaussian
	 * 
	 * @param mean
	 *            (x,y) components of mean vector
	 * @param cov
	 *            (xx, xy, yy) components of covariance matrix
	 * @return ImageJ roi
	 */
	public EllipseRoi createEllipse(final double[] mean, final double[] cov, final double nsigmas) {
		

		final double a = cov[0];
		final double b = cov[1];
		final double c = cov[2];
		final double d = Math.sqrt(a * a + 4 * b * b - 2 * a * c + c * c);
		final double scale1 = Math.sqrt(0.5 * (a + c + d)) * nsigmas;
		final double scale2 = Math.sqrt(0.5 * (a + c - d)) * nsigmas;
		final double theta = 0.5 * Math.atan2((2 * b), (a - c));
		final double x = mean[0] + standardRectangle.x;
		final double y = mean[1] + standardRectangle.y;
		final double dx = scale1 * Math.cos(theta);
		final double dy = scale1 * Math.sin(theta);
		final EllipseRoi ellipse = new EllipseRoi(x - dx, y - dy, x + dx, y + dy, scale2 / scale1);
		return ellipse;
	}
	
	
	public static void main(String[] args) {
		new ImageJ();

		ImagePlus imp = new Opener().openImage("/Users/varunkapoor/res/super_bent.tif");
		ImagePlus Preprocessedimp = new Opener().openImage("/Users/varunkapoor/res/super_bent.tif");
		imp.show();
		// Convert the image to 8-bit or 16-bit, very crucial for snakes
		IJ.run("16-bit");
		final ImagePlus currentimp = IJ.getImage();
		Preprocessedimp.show();
		IJ.run("16-bit");
		final ImagePlus currentPreprocessedimp = IJ.getImage();
		final double[] psf = { 1.65, 1.47 };
		final long radius = (long) (Math.ceil(Math.sqrt(psf[0] * psf[0] + psf[1] * psf[1])));

		// minimum length of the lines to be detected, the smallest possible
		// number is 2.
		final int minlength = (int) (radius);

		
		new InteractiveMT(currentimp, currentPreprocessedimp, psf, minlength).run(null);

	}
}
