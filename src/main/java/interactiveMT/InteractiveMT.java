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
import java.util.List;

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
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import labeledObjects.CommonOutputHF;
import labeledObjects.Indexedlength;
import lineFinder.FindlinesVia;
import lineFinder.LinefinderHFHough;
import lineFinder.LinefinderHFMSER;
import lineFinder.LinefinderHFMSERwHough;
import lineFinder.LinefinderHough;
import lineFinder.LinefinderMSER;
import lineFinder.LinefinderMSERwHough;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.util.Util;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * An interactive tool for MT tracking using MSER and Hough Transform
 * 
 * @author Varun Kapoor
 */

public class InteractiveMT implements PlugIn {

	final int extraSize = 40;
	final int scrollbarSize = 1000;
	// steps per octave
	public static int standardSensitivity = 100;
	int sensitivity = standardSensitivity;
	float deltaMin = 1f;
	float deltaMax = 500f;
	float delta = 1f;
	int deltaInit = 10;
	int MaxLines = 5;
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
	boolean NormalizeImage = false;
	boolean AutoDelta = false;
	int channel = 0;
	Img<FloatType> img;
	Img<FloatType> Preprocessedimg;
	
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

	// first and last slice to process
	int slice2, currentslice;

	public static enum ValueChange {
		SLICE, ROI, ALL, DELTA, FindLinesVia;
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

		Roi roi = imp.getRoi();

		if (roi == null) {
			// IJ.log( "A rectangular ROI is required to define the area..." );
			imp.setRoi(standardRectangle);
			roi = imp.getRoi();
		}

		if (roi.getType() != Roi.RECTANGLE) {
			IJ.log("Only rectangular rois are supported...");
			return;
		}

		currentslice = imp.getFrame();
		imp.setPosition(imp.getChannel(), imp.getSlice(), imp.getFrame());
		img = ImageJFunctions.convertFloat(imp);
		Preprocessedimg = ImageJFunctions.convertFloat(preprocessedimp);
		int z = currentslice;
		
		
		// copy the ImagePlus into an ArrayImage<FloatType> for faster access
		displaySliders();
		// add listener to the imageplus slice slider
				sliceObserver = new SliceObserver(imp, new ImagePlusListener());
				// compute first version#
		updatePreview(ValueChange.ALL);
		isStarted = true;
		
		// check whenever roi is modified to update accordingly
				roiListener = new RoiListener();
				imp.getCanvas().addMouseListener(roiListener);

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
		Roi roi = imp.getRoi();
		if (roi == null || roi.getType() != Roi.RECTANGLE) {
			imp.setRoi(new Rectangle(standardRectangle));
			roi = imp.getRoi();
			roiChanged = true;
		}

		final Rectangle rect = roi.getBounds();
		if (roiChanged || img == null || change == ValueChange.SLICE || rect.getMinX() != standardRectangle.getMinX()
				|| rect.getMaxX() != standardRectangle.getMaxX() || rect.getMinY() != standardRectangle.getMinY()
				|| rect.getMaxY() != standardRectangle.getMaxY()) {
			standardRectangle = rect;
			if (ndims == 2){
				
				currentimg = extractImage(img, standardRectangle, extraSize);
				currentPreprocessedimg = extractImage(Preprocessedimg, standardRectangle, extraSize);
				roiChanged = true;
				
			}
			if (ndims > 2){
			currentimg = extractImage(Views.hyperSlice(img, ndims - 1, currentslice - 1), standardRectangle, extraSize);
			currentPreprocessedimg = extractImage(Views.hyperSlice(Preprocessedimg, ndims - 1, currentslice - 1), standardRectangle, extraSize);
			roiChanged = true;
			}
			
		}

		// if we got some mouse click but the ROI did not change we can return
		if (!roiChanged && change == ValueChange.ROI) {
			isComputing = false;
			return;
		}
		
/*
		ArrayList<ArrayList<Trackproperties>> Allstart = new ArrayList<ArrayList<Trackproperties>>();
		ArrayList<ArrayList<Trackproperties>> Allend = new ArrayList<ArrayList<Trackproperties>>();
		
		if (ndims == 2) {
			Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> PrevFrameparam = null;
			if (FindLinesViaMSER) {

				LinefinderMSER newlineMser = new LinefinderMSER(img, Preprocessedimg, minlength, 0);
				newlineMser.setMaxlines(5);

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
				newlineMserwHough.setMaxlines(4);
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
			
			RandomAccessibleInterval<FloatType> groundframe = Views.hyperSlice(img, ndims - 1, 0);
			RandomAccessibleInterval<FloatType> groundframepre = Views.hyperSlice(Preprocessedimg, ndims - 1, 0);

			Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> PrevFrameparam = null;
			if (FindLinesViaMSER) {
				
				LinefinderMSER newlineMser = new LinefinderMSER(groundframe, groundframepre, minlength, 0);
				newlineMser.setMaxlines(MaxLines);
				newlineMser.setDelta(delta);
				PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength, 0, psf, newlineMser,
						UserChoiceModel.Line);

				Overlay overlay = newlineMser.getOverlay();
				ImagePlus impcurr = IJ.getImage();
				impcurr.setOverlay(overlay);
			}

			if (FindLinesViaHOUGH) {
				LinefinderHough newlineHough = new LinefinderHough(groundframe, groundframepre, minlength, 0);

				PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength, 0, psf, newlineHough,
						UserChoiceModel.Line);
				
			}

			if (FindLinesViaMSERwHOUGH) {
				LinefinderMSERwHough newlineMserwHough = new LinefinderMSERwHough(groundframe, groundframepre, minlength, 0);
				newlineMserwHough.setMaxlines(MaxLines);
				newlineMserwHough.setDelta(delta);
				PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength, 0, psf,
						newlineMserwHough, UserChoiceModel.Line);

				Overlay overlay = newlineMserwHough.getOverlay();
				ImagePlus impcurr = IJ.getImage();
				impcurr.setOverlay(overlay);
			}
			
			
			final int maxframe = (int) img.dimension(ndims - 1);

			for (int frame = 1; frame < maxframe; ++frame) {

				IntervalView<FloatType> currentframe = Views.hyperSlice(img, ndims - 1, frame);
				IntervalView<FloatType> currentframepre = Views.hyperSlice(Preprocessedimg, ndims - 1, frame);
				
				UserChoiceModel userChoiceModelHF = UserChoiceModel.Splineordersec;
				Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>, Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>>> returnVector = null;

				if (FindLinesViaMSER) {

					LinefinderHFMSER newlineMser = new LinefinderHFMSER(currentframe, currentframepre, minlength, frame);
					newlineMser.setMaxlines(2 * MaxLines);
					newlineMser.setDelta(delta);
					ImageJFunctions.show(currentframepre).setTitle("Preprocessed extended image");
					returnVector = FindlinesVia.LinefindingMethodHF(currentframe, currentframepre, PrevFrameparam,
							minlength, frame, psf, newlineMser, userChoiceModelHF);
					Overlay overlay = newlineMser.getOverlay();
					ImagePlus impcurr = IJ.getImage();
					impcurr.setOverlay(overlay);
					Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> NewFrameparam = returnVector.snd;

					ArrayList<Trackproperties> startStateVectors = returnVector.fst.fst;
					ArrayList<Trackproperties> endStateVectors = returnVector.fst.snd;

					PrevFrameparam = NewFrameparam;

					Allstart.add(startStateVectors);
					Allend.add(endStateVectors);
				}

				if (FindLinesViaHOUGH) {
					LinefinderHFHough newlineHough = new LinefinderHFHough(currentframe, currentframepre, minlength, frame);
					
					ImageJFunctions.show(currentframepre).setTitle("Preprocessed extended image");
					returnVector = FindlinesVia.LinefindingMethodHF(currentframe, currentframepre, PrevFrameparam,
							minlength, frame, psf, newlineHough, userChoiceModelHF);
					Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> NewFrameparam = returnVector.snd;

					ArrayList<Trackproperties> startStateVectors = returnVector.fst.fst;
					ArrayList<Trackproperties> endStateVectors = returnVector.fst.snd;

					PrevFrameparam = NewFrameparam;

					Allstart.add(startStateVectors);
					Allend.add(endStateVectors);
				}

				if (FindLinesViaMSERwHOUGH) {

					LinefinderHFMSERwHough newlineMserwHough = new LinefinderHFMSERwHough(currentframe, currentframepre,
							minlength, frame);
					ImageJFunctions.show(currentframepre).setTitle("Preprocessed extended image");
					newlineMserwHough.setMaxlines(2 * MaxLines);
					newlineMserwHough.setDelta(delta);
					returnVector = FindlinesVia.LinefindingMethodHF(currentframe, currentframepre, PrevFrameparam,
							minlength, frame, psf, newlineMserwHough, userChoiceModelHF);
					Overlay overlay = newlineMserwHough.getOverlay();
					ImagePlus impcurr = IJ.getImage();
					impcurr.setOverlay(overlay);
					Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> NewFrameparam = returnVector.snd;

					ArrayList<Trackproperties> startStateVectors = returnVector.fst.fst;
					ArrayList<Trackproperties> endStateVectors = returnVector.fst.snd;

					PrevFrameparam = NewFrameparam;

					Allstart.add(startStateVectors);
					Allend.add(endStateVectors);
				}

				
			
			}
			
			
		}
*/
	
	
		imp.updateAndDraw();

		isComputing = false;
	}

	public void displaySliders() {

		final Frame frame = new Frame("Find MT's and Track");
		frame.setSize(550, 550);
		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		final Checkbox Normalize = new Checkbox("Normailze Image Intensity (recommended)", NormalizeImage);
		CheckboxGroup Finders = new CheckboxGroup();
		
		final Label MTText = new Label("Step 1) Choose your method to find lines ", Label.CENTER);
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
			sliceObserver = new SliceObserver(imp, new ImagePlusListener());

			imp.setSlice(1);
			
			IJ.log("Starting Chosen Line finder");
			
			
			
			currentslice = 1;

			if (imp.getType() == ImagePlus.COLOR_RGB || imp.getType() == ImagePlus.COLOR_256) {
				IJ.log("Color images are not supported, please convert to 8, 16 or 32-bit grayscale");
				return;
			}

			Roi roi = imp.getRoi();
			if (roi == null) {
				// IJ.log( "A rectangular ROI is required to define the area..."
				// );
				imp.setRoi(standardRectangle);
				roi = imp.getRoi();
			}
			updatePreview(ValueChange.ALL);
			if (ndims == 2) {
				Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> PrevFrameparam = null;
				if (FindLinesViaMSER) {

					LinefinderMSER newlineMser = new LinefinderMSER(img, Preprocessedimg, minlength, 0);
					newlineMser.setMaxlines(MaxLines);

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
					newlineMserwHough.setMaxlines(MaxLines);
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
					
					LinefinderMSER newlineMser = new LinefinderMSER(groundframe, groundframepre, minlength, currentslice - 1);
					newlineMser.setMaxlines(MaxLines);
					newlineMser.setDelta(delta);
					PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength, currentslice - 1, psf, newlineMser,
							UserChoiceModel.Line);

					Overlay overlay = newlineMser.getOverlay();
					ImagePlus impcurr = IJ.getImage();
					impcurr.setOverlay(overlay);
				}

				if (FindLinesViaHOUGH) {
					LinefinderHough newlineHough = new LinefinderHough(groundframe, groundframepre, minlength, currentslice - 1);

					PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength, currentslice - 1, psf, newlineHough,
							UserChoiceModel.Line);
					
				}

				if (FindLinesViaMSERwHOUGH) {
					LinefinderMSERwHough newlineMserwHough = new LinefinderMSERwHough(groundframe, groundframepre, minlength, currentslice - 1);
					newlineMserwHough.setMaxlines(MaxLines);
					newlineMserwHough.setDelta(delta);
					PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength, currentslice - 1, psf,
							newlineMserwHough, UserChoiceModel.Line);

					Overlay overlay = newlineMserwHough.getOverlay();
					ImagePlus impcurr = IJ.getImage();
					impcurr.setOverlay(overlay);
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
				DialogueMSER();
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
	
	
	
	private boolean DialogueMSER(){
		
		// Create dialog
				GenericDialog gd = new GenericDialog("Mser Parameters");
				final Scrollbar delta1 = new Scrollbar(Scrollbar.HORIZONTAL, deltaInit, 10, 0, 10 + scrollbarSize);
				this.delta = computeValueFromScrollbarPosition(deltaInit, deltaMin, deltaMax, scrollbarSize);
				final Label deltaText = new Label("delta = " + this.delta, Label.CENTER);
				gd.addSlider("delta = ", deltaMin, deltaMax, deltaInit);
				
				delta1.addAdjustmentListener(
						new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta1));
				
				 gd .addCheckbox("Auto determine optimal delta", false);
				 gd.addNumericField("Maximum Lines (Green Channel)", MaxLines, 0);
				 
				 AutoDelta = gd.getNextBoolean();
				 MaxLines = (int) gd.getNextNumber();
				

				gd.showDialog();
				
				return !gd.wasCanceled();
		
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

	protected class FrameListener extends WindowAdapter {
		final Frame parent;

		public FrameListener(Frame parent) {
			super();
			this.parent = parent;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			close(parent, sliceObserver, imp);
		}
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
			location[2] = (imp.getCurrentSlice() - 1) / imp.getNChannels();

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
			location[2] = (imp.getCurrentSlice() - 1) / imp.getNChannels();

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
	protected final void close(final Frame parent, final SliceObserver sliceObserver, final ImagePlus imp) {
		if (parent != null)
			parent.dispose();

		if (sliceObserver != null)
			sliceObserver.unregister();

		if (imp != null) {

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
			final Roi roi = imp.getRoi();

			if (roi == null || roi.getType() != Roi.RECTANGLE)
				return;

			while (isComputing)
				SimpleMultiThreading.threadWait(10);

			updatePreview(ValueChange.ROI);
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
