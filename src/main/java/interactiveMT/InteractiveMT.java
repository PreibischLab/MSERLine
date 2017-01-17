package interactiveMT;

import java.awt.BorderLayout;
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
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.border.EmptyBorder;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.sun.tools.javac.util.Pair;

import LineModels.UseLineModel.UserChoiceModel;
import drawandOverlay.DisplayGraph;
import drawandOverlay.PushCurves;
import fiji.tool.SliceListener;
import fiji.tool.SliceObserver;
import graphconstructs.Trackproperties;
import houghandWatershed.WatershedDistimg;
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
import lineFinder.LinefinderInteractiveHFHough;
import lineFinder.LinefinderInteractiveHFMSER;
import lineFinder.LinefinderInteractiveHFMSERwHough;
import lineFinder.LinefinderInteractiveHough;
import lineFinder.LinefinderInteractiveMSER;
import lineFinder.LinefinderInteractiveMSERwHough;
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
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import peakFitter.SortListbyproperty;
import preProcessing.GetLocalmaxmin;
import preProcessing.GlobalThresholding;
import preProcessing.Kernels;
import preProcessing.MedianFilter2D;
import preProcessing.MedianFilterImg2D;
import roiFinder.Roifinder;
import roiFinder.RoifinderMSER;
import velocityanalyser.Trackend;
import velocityanalyser.Trackstart;

/**
 * An interactive tool for MT tracking using MSER and Hough Transform
 * 
 * @author Varun Kapoor
 */

public class InteractiveMT implements PlugIn {

	final int scrollbarSize = 1000;
	final int scrollbarSizebig = 1000;
	// steps per octave
	public static int standardSensitivity = 4;
	int sensitivity = standardSensitivity;
	float deltaMin = 0;
	float thetaPerPixelMin = new Float(0.2);
	float rhoPerPixelMin = new Float(0.2);
	float thresholdHoughMin = 0;
	float thresholdHoughMax = 250;
	float deltaMax = 400f;
	float maxVarMin = 0;
	float maxVarMax = 1;
	float thetaPerPixelMax = 2;
	float rhoPerPixelMax = 2;

	boolean darktobright = false;
	boolean displayBitimg = false;
	boolean displayWatershedimg = false;
	long minSize = 1;
	long maxSize = 1000;
	long minSizemin = 0;
	long minSizemax = 100;
	long maxSizemin = 100;
	long maxSizemax = 10000;

	float minDiversityMin = 0;
	float minDiversityMax = 1;

	UserChoiceModel userChoiceModel;
	float delta = 1f;
	int deltaInit = 10;
	int maxVarInit = 1;

	int minSizeInit = 1;
	int maxSizeInit = 100;
	float thresholdHoughInit = 100;
	float rhoPerPixelInit = new Float(0.5);
	float thetaPerPixelInit = new Float(0.5);

	public int minDiversityInit = 1;
	public int radius = 1;
	public long Size = 1;
	public float thetaPerPixel = 1;
	public float rhoPerPixel = 1;
	boolean enablerhoPerPixel = false;
	public float maxVar = 1;
	public float minDiversity = 1;
	public float thresholdHough = 1;

	Color colorDraw = null;
	FloatType minval = new FloatType(0);
	FloatType maxval = new FloatType(1);
	SliceObserver sliceObserver;
	RoiListener roiListener;
	boolean isComputing = false;
	boolean isStarted = false;
	boolean FindLinesViaMSER = false;
	boolean FindLinesViaHOUGH = false;
	boolean FindLinesViaMSERwHOUGH = false;
	boolean ShowMser = false;
	boolean ShowHough = false;
    boolean update = false;
	boolean Canny = false;
	boolean RoisViaMSER = false;
	boolean RoisViaWatershed = false;
    boolean displayTree = false;
	boolean GaussianLines = true;
	boolean NormalizeImage = false;
	boolean Mediancurr = false;
	boolean MedianAll = false;
	boolean AutoDelta = false;
	
	ArrayList<ArrayList<Trackproperties>> Allstart = new ArrayList<ArrayList<Trackproperties>>();
	ArrayList<ArrayList<Trackproperties>> Allend = new ArrayList<ArrayList<Trackproperties>>();
	
	
	int channel = 0;
	Img<FloatType> originalimg;
	Img<FloatType> originalPreprocessedimg;
	int inix = 20;
	int iniy = 20;
	MserTree<UnsignedByteType> newtree;
	// Image 2d at the current slice
	RandomAccessibleInterval<FloatType> currentimg;
	RandomAccessibleInterval<FloatType> currentPreprocessedimg;
	RandomAccessibleInterval<IntType> intimg;
	Color originalColor = new Color(0.8f, 0.8f, 0.8f);
	Color inactiveColor = new Color(0.95f, 0.95f, 0.95f);
	ImagePlus imp;
	ImagePlus preprocessedimp;
	final double[] psf;
	final int minlength;
	int stacksize;
	int Maxlabel;
	private int ndims;
	Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> PrevFrameparam; 
	Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> NewFrameparam;
	Pair<Pair<ArrayList<Trackproperties>, ArrayList<Trackproperties>>, 
	Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>>> returnVector;
	ArrayList<CommonOutputHF> output;
	
	public Rectangle standardRectangle;
	public FinalInterval interval;
	RandomAccessibleInterval<UnsignedByteType> newimg;
	ArrayList<double[]> AllmeanCovar;
	// first and last slice to process
	int endStack, currentframe;

	public static enum ValueChange {
		ROI, ALL, DELTA, FindLinesVia, MAXVAR, MINDIVERSITY, DARKTOBRIGHT, MINSIZE, MAXSIZE, SHOWMSER, FRAME, SHOWHOUGH, thresholdHough, DISPLAYBITIMG, DISPLAYWATERSHEDIMG, rhoPerPixel, thetaPerPixel;
	}

	boolean isFinished = false;
	boolean wasCanceled = false;
	boolean SecondOrderSpline;
	boolean ThirdOrderSpline;

	public boolean isFinished() {
		return isFinished;
	}

	public boolean wasCanceled() {
		return wasCanceled;
	}

	public boolean getFindLinesViaMSER() {
		return FindLinesViaMSER;
	}

	public boolean getRoisViaMSER() {

		return RoisViaMSER;
	}

	public boolean getRoisViaWatershed() {

		return RoisViaWatershed;
	}

	public void setRoisViaMSER(final boolean RoisViaMSER) {

		this.RoisViaMSER = RoisViaMSER;
	}

	public void setRoisViaWatershed(final boolean RoisViaWatershed) {

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

	public double getInitialDelta(final float value) {

		return delta;

	}

	public void setInitialmaxVar(final float value) {
		maxVar = value;
		maxVarInit = computeScrollbarPositionFromValue(maxVar, maxVarMin, maxVarMax, scrollbarSize);
	}

	public double getInitialmaxVar(final float value) {

		return maxVar;

	}

	public void setInitialthresholdHough(final float value) {
		thresholdHough = value;
		thresholdHoughInit = computeScrollbarPositionFromValue(thresholdHough, thresholdHoughMin, thresholdHoughMax,
				scrollbarSize);
	}

	public void setInitialthetaPerPixel(final float value) {
		thetaPerPixel = value;
		thetaPerPixelInit = computeScrollbarPositionFromValue(thetaPerPixel, thetaPerPixelMin, thetaPerPixelMax,
				scrollbarSize);
	}

	public void setInitialrhoPerPixel(final float value) {
		rhoPerPixel = value;
		rhoPerPixelInit = computeScrollbarPositionFromValue(rhoPerPixel, rhoPerPixelMin, rhoPerPixelMax, scrollbarSize);
	}

	public double getInitialthresholdHough(final float value) {

		return thresholdHough;

	}

	public double getInitialthetaPerPixel(final float value) {

		return thetaPerPixel;

	}

	public double getInitialrhoPerPixel(final float value) {

		return rhoPerPixel;

	}

	public void setInitialminDiversity(final float value) {
		minDiversity = value;
		minDiversityInit = computeScrollbarPositionFromValue(minDiversity, minDiversityMin, minDiversityMax,
				scrollbarSize);
	}

	public double getInitialminDiversity(final float value) {

		return minDiversity;

	}

	public void setInitialminSize(final int value) {
		minSize = value;
		minSizeInit = computeScrollbarPositionFromValue(minSize, minSizemin, minSizemax, scrollbarSize);
	}

	public double getInitialminSize(final int value) {

		return minSize;

	}

	public void setInitialmaxSize(final int value) {
		maxSize = value;
		maxSizeInit = computeScrollbarPositionFromValue(maxSize, maxSizemin, maxSizemax, scrollbarSize);
	}

	public double getInitialmaxSize(final int value) {

		return maxSize;

	}

	public InteractiveMT(final ImagePlus imp, final ImagePlus preprocessedimp, final double[] psf,
			final int minlength) {
		this.imp = imp;
		this.preprocessedimp = preprocessedimp;
		this.psf = psf;
		this.minlength = minlength;
		ndims = imp.getNDimensions();
		standardRectangle = new Rectangle(inix, iniy, imp.getWidth() - 2 * inix, imp.getHeight() - 2 * iniy);
		originalimg = ImageJFunctions.convertFloat(imp.duplicate());
		originalPreprocessedimg = ImageJFunctions.convertFloat(preprocessedimp.duplicate());
		
		

	}

	@Override
	public void run(String arg) {
		stacksize = preprocessedimp.getNFrames();
		output = new ArrayList<CommonOutputHF>();
		endStack = stacksize;
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

		
		preprocessedimp.setPosition(preprocessedimp.getChannel(), preprocessedimp.getSlice(),
				0);
		imp.setPosition(preprocessedimp.getChannel(), preprocessedimp.getSlice(), 0);
		currentframe = preprocessedimp.getFrame();

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

		Rectangle rect = roi.getBounds();
		if (roiChanged || currentimg == null || currentPreprocessedimg == null || newimg == null
				|| change == ValueChange.FRAME || rect.getMinX() != standardRectangle.getMinX()
				|| rect.getMaxX() != standardRectangle.getMaxX() || rect.getMinY() != standardRectangle.getMinY()
				|| rect.getMaxY() != standardRectangle.getMaxY() || change == ValueChange.ALL) {
			standardRectangle = rect;

			long[] min = { (long) standardRectangle.getMinX(), (long) standardRectangle.getMinY() };
			long[] max = { (long) standardRectangle.getMaxX(), (long) standardRectangle.getMaxY() };
			interval = new FinalInterval(min, max);
			final long Cannyradius = (long) (Math.ceil(Math.sqrt(psf[0] * psf[0] + psf[1] * psf[1])));
			if (ndims == 2) {

				currentimg = extractImage(originalimg);
				currentPreprocessedimg = extractImage(originalPreprocessedimg);
				
			
				if (Canny)
				newimg = copytoByteImage(Kernels.CannyEdgeandMean(currentPreprocessedimg, Cannyradius));
				else
				newimg = copytoByteImage(currentPreprocessedimg);

				roiChanged = true;
			}
			if (ndims > 2) {

				currentimg = extractImage(Views.hyperSlice(originalimg, ndims - 1, currentframe - 1));
				currentPreprocessedimg = extractImage(
						Views.hyperSlice(originalPreprocessedimg, ndims - 1, currentframe - 1));
				
				if (Canny)
					newimg = copytoByteImage(Kernels.CannyEdgeandMean(currentPreprocessedimg, Cannyradius));
					else
					newimg = copytoByteImage(currentPreprocessedimg);
				

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

		if (change == ValueChange.SHOWHOUGH) {

			MouseEvent mev = new MouseEvent(preprocessedimp.getCanvas(), MouseEvent.MOUSE_RELEASED,
					System.currentTimeMillis(), 0, 0, 0, 1, false);

			if (mev != null) {

				roimanager.close();

				roimanager = new RoiManager();

			}

			IJ.log("Doing watershedding on the distance transformed image ");

			RandomAccessibleInterval<BitType> bitimg = new ArrayImgFactory<BitType>().create(newimg, new BitType());
			GetLocalmaxmin.ThresholdingBit(newimg, bitimg, thresholdHough);

			if (displayBitimg)
				ImageJFunctions.show(bitimg);

			WatershedDistimg<UnsignedByteType> WaterafterDisttransform = new WatershedDistimg<UnsignedByteType>(newimg,
					bitimg);
			WaterafterDisttransform.checkInput();
			WaterafterDisttransform.process();
			intimg = WaterafterDisttransform.getResult();
			Maxlabel = WaterafterDisttransform.GetMaxlabelsseeded(intimg);
			if (displayWatershedimg)
				ImageJFunctions.show(intimg);

		}

		if (change == ValueChange.SHOWMSER) {

			MouseEvent mev = new MouseEvent(preprocessedimp.getCanvas(), MouseEvent.MOUSE_RELEASED,
					System.currentTimeMillis(), 0, 0, 0, 1, false);

			if (mev != null) {

				roimanager.close();

				roimanager = new RoiManager();

			}

			IJ.log(" Computing the Component tree");
			
			ImageJFunctions.show(newimg);
			newtree = MserTree.buildMserTree(newimg, delta, minSize, maxSize, maxVar, minDiversity, darktobright);
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

	
	private boolean maxStack(){
		GenericDialog gd = new GenericDialog("Choose Final Frame");
		if (stacksize > 1) {
			
			gd.addNumericField("Do till frame", stacksize, 0);

			assert (int) gd.getNextNumber() > 1;
		}

		gd.showDialog();
		if (stacksize > 1) {
			stacksize = (int) gd.getNextNumber();

		}
		return !gd.wasCanceled();
		
	}
	
	public void displaySliders() {

		final Frame frame = new Frame("Find MT's and Track");
		frame.setSize(550, 550);
		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		
		CheckboxGroup Finders = new CheckboxGroup();
		final Checkbox Normalize = new Checkbox("Normailze Image Intensity (recommended)", NormalizeImage);
		final Checkbox MedFiltercur = new Checkbox("Apply Median Filter to current Frame", Mediancurr);
		final Checkbox MedFilterAll = new Checkbox("Apply Median Filter to Stack", MedianAll);
		final Checkbox CannyDeriv = new Checkbox("Apply Edge detector", Canny);
		final Label MTText = new Label("Step 1) Determine end points of MT (start from seeds) ", Label.CENTER);
		final Label MTTextHF = new Label("Step 2) Track both end points of MT over time ", Label.CENTER);
		final Button MoveNext = new Button("Determine these parameters for higher frames");
		final Button JumptoFrame = new Button("Jump to frame:");
		final Button TrackEndPoints = new Button("Track EndPoints");
		final Button SkipframeandTrackEndPoints = new Button("Skip first few frames and Track EndPoints");
		
		final Checkbox mser = new Checkbox("MSER", Finders, FindLinesViaMSER);
		final Checkbox hough = new Checkbox("HOUGH", Finders, FindLinesViaHOUGH);
		final Checkbox mserwhough = new Checkbox("MSERwHOUGH", Finders, FindLinesViaMSERwHOUGH);
      
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
		c.insets = new Insets(10, 10, 0, 0);
		frame.add(CannyDeriv, c);
		
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
		c.insets = new Insets(10, 10, 0, 235);
		frame.add(MoveNext, c);

		++c.gridy;
		c.insets = new Insets(10, 10, 0, 355);
		frame.add(JumptoFrame, c);
		
		++c.gridy;
		c.insets = new Insets(10, 10, 0, 0);
		frame.add(MTTextHF, c);
		
		++c.gridy;
		c.insets = new Insets(10, 10, 0, 355);
		frame.add(TrackEndPoints, c);
		
		++c.gridy;
		c.insets = new Insets(10, 10, 0, 235);
		frame.add(SkipframeandTrackEndPoints, c);

		Normalize.addItemListener(new NormalizeListener());
		MedFiltercur.addItemListener(new MediancurrListener() );
		MedFilterAll.addItemListener(new MedianAllListener() );
		CannyDeriv.addItemListener(new CannyListener() );
		
		mser.addItemListener(new MserListener());
		hough.addItemListener(new HoughListener());
		mserwhough.addItemListener(new MserwHoughListener());
		MoveNext.addActionListener(new moveNextListener());
		JumptoFrame.addActionListener(new moveToFrameListener());
		TrackEndPoints.addActionListener(new TrackendsListener());
		SkipframeandTrackEndPoints.addActionListener(new SkipFramesandTrackendsListener());

		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);
		MTText.setFont(MTText.getFont().deriveFont(Font.BOLD));
		MTTextHF.setFont(MTTextHF.getFont().deriveFont(Font.BOLD));
	}

	protected class moveNextListener implements ActionListener {
		@Override
		public void actionPerformed(final ActionEvent arg0) {

			// add listener to the imageplus slice slider
			sliceObserver = new SliceObserver(preprocessedimp, new ImagePlusListener());

			preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), preprocessedimp.getFrame());
			imp.setPosition(channel, preprocessedimp.getSlice(), preprocessedimp.getFrame());
			if (preprocessedimp.getFrame() + 1 <= stacksize) {
				preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), preprocessedimp.getFrame() + 1);
				imp.setPosition(channel, preprocessedimp.getSlice(), preprocessedimp.getFrame());
				IJ.log(" Current Frame: " + (preprocessedimp.getFrame()) + " stacksize: " + stacksize);
			} else {
				IJ.log("Max frame number exceeded, moving to last frame instead" + "Stacksize: " + stacksize);
				preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), stacksize);
				imp.setPosition(channel, preprocessedimp.getSlice(), stacksize);
				currentframe = stacksize;
			}
			currentframe = preprocessedimp.getFrame();

			Roi roi = preprocessedimp.getRoi();
			if (roi == null) {
				// IJ.log( "A rectangular ROI is required to define the area..."
				// );
				preprocessedimp.setRoi(standardRectangle);
				roi = preprocessedimp.getRoi();
			}

			updatePreview(ValueChange.FRAME);
			isStarted = true;
			
			// check whenever roi is modified to update accordingly
			roiListener = new RoiListener();
			preprocessedimp.getCanvas().addMouseListener(roiListener);
			
			if (FindLinesViaMSER)
				UpdateMSER();
			
			if (FindLinesViaHOUGH)
				UpdateHough();
			
			if (FindLinesViaMSERwHOUGH)
				UpdateMSERwHough();

		}
	}

	private boolean moveDialogue() {
		GenericDialog gd = new GenericDialog("Choose Frame");

		if (stacksize > 1) {
			gd.addNumericField("Move to frame", currentframe, 0);

		}

		gd.showDialog();
		if (stacksize > 1) {
			currentframe = (int) gd.getNextNumber();

		}
		return !gd.wasCanceled();
	}
	
	
	

	protected class moveToFrameListener implements ActionListener {
		@Override
		public void actionPerformed(final ActionEvent arg0) {

			boolean dialog = moveDialogue();
			if (dialog) {
				// add listener to the imageplus slice slider
				sliceObserver = new SliceObserver(preprocessedimp, new ImagePlusListener());
				preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), preprocessedimp.getFrame());
				imp.setPosition(channel, preprocessedimp.getSlice(), currentframe);
				if (currentframe <= stacksize) {
					preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), currentframe);
					imp.setPosition(channel, preprocessedimp.getSlice(), currentframe);
				} else {
					IJ.log("Max frame number exceeded, moving to last frame instead");
					preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), stacksize);
					imp.setPosition(channel, preprocessedimp.getSlice(), stacksize);
					currentframe = stacksize;
				}

				if (preprocessedimp.getType() == ImagePlus.COLOR_RGB
						|| preprocessedimp.getType() == ImagePlus.COLOR_256) {
					IJ.log("Color images are not supported, please convert to 8, 16 or 32-bit grayscale");
					return;
				}

				Roi roi = preprocessedimp.getRoi();
				if (roi == null) {
					// IJ.log( "A rectangular ROI is required to define the
					// area..."
					// );
					preprocessedimp.setRoi(standardRectangle);
					roi = preprocessedimp.getRoi();
				}
			}
			updatePreview(ValueChange.FRAME);
			isStarted = true;
		
			// check whenever roi is modified to update accordingly
			roiListener = new RoiListener();
			preprocessedimp.getCanvas().addMouseListener(roiListener);

			if (FindLinesViaMSER)
				UpdateMSER();
			
			if (FindLinesViaHOUGH)
				UpdateHough();
			
			if (FindLinesViaMSERwHOUGH)
				UpdateMSERwHough();
		}
	}

	protected class FindLinesListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			// add listener to the imageplus slice slider
			sliceObserver = new SliceObserver(preprocessedimp, new ImagePlusListener());

			IJ.log("Starting Chosen Line finder from the seed image (first frame should be seeds)");
			preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), currentframe);
			imp.setPosition(channel, preprocessedimp.getSlice(), currentframe);

			updatePreview(ValueChange.FRAME);
			isStarted = true;

			Roi roi = preprocessedimp.getRoi();
			if (roi == null) {
				// IJ.log( "A rectangular ROI is required to define the area..."
				// );
				preprocessedimp.setRoi(standardRectangle);
				roi = preprocessedimp.getRoi();
			}
			IJ.log("Current frame: " + currentframe);
			updatePreview(ValueChange.ALL);
	
			
			if (ndims == 2) {
				if (FindLinesViaMSER) {
					boolean dialog = DialogueModelChoice();
					if (dialog) {

						updatePreview(ValueChange.SHOWMSER);
						LinefinderInteractiveMSER newlineMser = new LinefinderInteractiveMSER(currentimg,
								currentPreprocessedimg, newtree, minlength, currentframe);

						PrevFrameparam = FindlinesVia.LinefindingMethod(currentimg, currentPreprocessedimg, minlength,
								currentframe, psf, newlineMser, userChoiceModel);
					}

				}
				if (FindLinesViaHOUGH) {
					boolean dialog = DialogueModelChoice();
					if (dialog) {
						updatePreview(ValueChange.SHOWHOUGH);
						LinefinderInteractiveHough newlineHough = new LinefinderInteractiveHough(currentimg,
								currentPreprocessedimg, intimg, Maxlabel, thetaPerPixel, rhoPerPixel, currentframe);

						PrevFrameparam = FindlinesVia.LinefindingMethod(currentimg, currentPreprocessedimg, minlength,
								currentframe, psf, newlineHough, userChoiceModel);
					}
				}

				if (FindLinesViaMSERwHOUGH) {
					boolean dialog = DialogueModelChoice();
					if (dialog) {
						updatePreview(ValueChange.SHOWMSER);
						LinefinderInteractiveMSERwHough newlineMserwHough = new LinefinderInteractiveMSERwHough(
								currentimg, currentPreprocessedimg, newtree, minlength, currentframe, thetaPerPixel, rhoPerPixel);
						PrevFrameparam = FindlinesVia.LinefindingMethod(currentimg, currentPreprocessedimg, minlength,
								currentframe, psf, newlineMserwHough, userChoiceModel);
					}
				}
				// Draw the detected lines
				RandomAccessibleInterval<FloatType> gaussimg = new ArrayImgFactory<FloatType>().create(currentimg,
						new FloatType());
				PushCurves.DrawstartLine(gaussimg, PrevFrameparam.fst, PrevFrameparam.snd, psf);
				ImageJFunctions.show(gaussimg).setTitle("Exact-line-start");

			}

			if (ndims > 2) {

				RandomAccessibleInterval<FloatType> groundframe = currentimg;
				RandomAccessibleInterval<FloatType> groundframepre = currentPreprocessedimg;
				if (FindLinesViaMSER) {
					boolean dialog = DialogueModelChoice();
					if (dialog) {
						updatePreview(ValueChange.SHOWMSER);
						LinefinderInteractiveMSER newlineMser = new LinefinderInteractiveMSER(groundframe,
								groundframepre, newtree, minlength, currentframe);
						PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength,
								currentframe, psf, newlineMser, userChoiceModel);
					}

				}

				if (FindLinesViaHOUGH) {

					boolean dialog = DialogueModelChoice();
					if (dialog) {
						updatePreview(ValueChange.SHOWHOUGH);
						LinefinderInteractiveHough newlineHough = new LinefinderInteractiveHough(groundframe,
								groundframepre, intimg, Maxlabel, thetaPerPixel, rhoPerPixel, currentframe);

						PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength,
								currentframe, psf, newlineHough, userChoiceModel);
					}
				}

				if (FindLinesViaMSERwHOUGH) {
					boolean dialog = DialogueModelChoice();
					if (dialog) {
						updatePreview(ValueChange.SHOWMSER);
						LinefinderInteractiveMSERwHough newlineMserwHough = new LinefinderInteractiveMSERwHough(groundframe, groundframepre,
								newtree, minlength, currentframe, thetaPerPixel, rhoPerPixel);
						PrevFrameparam = FindlinesVia.LinefindingMethod(groundframe, groundframepre, minlength,
								currentframe, psf, newlineMserwHough, userChoiceModel);
					}

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
	            	IJ.log("Image Stack Intensity Normalized between: " 
	            	+ (int)minval.get() + " and " + (int)maxval.get());
	           		Normalize.normalize(Views.iterable(originalimg), minval, maxval);
	           		Normalize.normalize(Views.iterable(originalPreprocessedimg), minval, maxval);
	            	
	               }
			}
	    	
	    	
	    }
	    
	 
	 protected class CannyListener implements ItemListener{

			@Override
			public void itemStateChanged(ItemEvent arg0) {
	              
	               if (arg0.getStateChange() == ItemEvent.DESELECTED)
	            	   Canny = false;
	               else if (arg0.getStateChange() == ItemEvent.SELECTED){
	            	   Canny = true;
	            	   
	            	  
	            	
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
	    			IJ.log(" Median filter sucessfully applied to current Image" );
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
	              	
	              	final MedianFilterImg2D<FloatType> medfilter = new MedianFilterImg2D<FloatType>(originalPreprocessedimg, radius);
	    			medfilter.process();
	    			IJ.log(" Median filter sucessfully applied to the whole stack" );
	    			originalPreprocessedimg = medfilter.getResult();
	              	
	              	
	                 }
	  		}
	      	
	      	
	      }
	    
//	    final long Cannyradius = (long) (Math.ceil(Math.sqrt(psf[0] * psf[0] + psf[1] * psf[1])));
		
	//	newimg = copytoByteImage(Kernels.CannyEdgeandMean(currentPreprocessedimg, Cannyradius));
	protected class SkipFramesandTrackendsListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

		
			sliceObserver = new SliceObserver(preprocessedimp, new ImagePlusListener());
			
			boolean dialogmove = moveDialogue();
			if (dialogmove) {
				// add listener to the imageplus slice slider
				
				preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), preprocessedimp.getFrame());
				imp.setPosition(channel, preprocessedimp.getSlice(), preprocessedimp.getFrame());

				if (currentframe <= stacksize) {
					preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), currentframe);
					imp.setPosition(channel, imp.getSlice(), currentframe);
				} else {
					IJ.log("Max frame number exceeded, moving to last frame instead");
					preprocessedimp.setPosition(channel, imp.getSlice(), stacksize);
					imp.setPosition(channel, imp.getSlice(), stacksize);
					currentframe = stacksize;
				}

			
			}
			updatePreview(ValueChange.FRAME);
			isStarted = true;
			

			
			

			int next = preprocessedimp.getFrame();
		
			
			 maxStack();
			 
			
			 
			for (int index = next; index <= stacksize; ++index) {
			
			
				
				currentframe = index;
				preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), currentframe);
				imp.setPosition(channel, preprocessedimp.getSlice(), currentframe);
				
				
			updatePreview(ValueChange.FRAME);
			isStarted = true;

			Roi roi = preprocessedimp.getRoi();
			if (roi == null) {
				// IJ.log( "A rectangular ROI is required to define the area..."
				// );
				preprocessedimp.setRoi(standardRectangle);
				roi = preprocessedimp.getRoi();
			}
			IJ.log("Current frame: " + currentframe);
		
			//newimg = copytoByteImage(Kernels.CannyEdgeandMean(currentPreprocessedimg, Cannyradius));
			boolean dialog;
			boolean dialogupdate;
			
			if (ndims > 2) {

				RandomAccessibleInterval<FloatType> groundframe = currentimg;
				RandomAccessibleInterval<FloatType> groundframepre = currentPreprocessedimg;
				if (FindLinesViaMSER) {
					if (index == next)
						dialog = DialogueModelChoice();
						
						else dialog = false;
	
					
					
				
					
					
					updatePreview(ValueChange.SHOWMSER);

						LinefinderInteractiveHFMSER newlineMser = new LinefinderInteractiveHFMSER(groundframe,
								groundframepre, newtree, minlength, currentframe);

						returnVector = FindlinesVia.LinefindingMethodHF(groundframe, groundframepre, PrevFrameparam,
								minlength, currentframe, psf, newlineMser, userChoiceModel);
						

				}

				if (FindLinesViaHOUGH) {

					if (index == next)
						dialog = DialogueModelChoice();
						else dialog = false;
					
					
					
					updatePreview(ValueChange.SHOWHOUGH);
						LinefinderInteractiveHFHough newlineHough = new LinefinderInteractiveHFHough(groundframe,
								groundframepre, intimg, Maxlabel, thetaPerPixel, rhoPerPixel, currentframe);

						returnVector = FindlinesVia.LinefindingMethodHF(groundframe, groundframepre, PrevFrameparam,
								minlength, currentframe, psf, newlineHough, userChoiceModel);
					
				}

				if (FindLinesViaMSERwHOUGH) {
					if (index == next)
						dialog = DialogueModelChoice();
						else dialog = false;
					
					updatePreview(ValueChange.SHOWMSER);
						LinefinderInteractiveHFMSERwHough newlineMserwHough = new LinefinderInteractiveHFMSERwHough(
								groundframe, groundframepre, newtree, minlength, currentframe, thetaPerPixel, rhoPerPixel);
						returnVector = FindlinesVia.LinefindingMethodHF(groundframe, groundframepre, PrevFrameparam,
								minlength, currentframe, psf, newlineMserwHough, userChoiceModel);
						
						

				}

			}
			

			 NewFrameparam = returnVector.snd;

			ArrayList<Trackproperties> startStateVectors = returnVector.fst.fst;
			ArrayList<Trackproperties> endStateVectors = returnVector.fst.snd;

			PrevFrameparam = NewFrameparam;

			Allstart.add(startStateVectors);
			Allend.add(endStateVectors);
			
			}
			
		

			ImagePlus impstartsec = ImageJFunctions.show(originalimg);
			ImagePlus impendsec = ImageJFunctions.show(originalPreprocessedimg);

			final Trackstart trackerstart = new Trackstart(Allstart,stacksize - next);
			final Trackend trackerend = new Trackend(Allend, stacksize - next);
			trackerstart.process();
			SimpleWeightedGraph<double[], DefaultWeightedEdge> graphstart = trackerstart.getResult();
			DisplayGraph displaygraphtrackstart = new DisplayGraph(impstartsec, graphstart);
			displaygraphtrackstart.getImp();
			impstartsec.draw();

			trackerend.process();
			SimpleWeightedGraph<double[], DefaultWeightedEdge> graphend = trackerend.getResult();
			DisplayGraph displaygraphtrackend = new DisplayGraph(impendsec, graphend);
			displaygraphtrackend.getImp();
			impendsec.draw();
			
			
			 
		}
	}

	

	
	protected class TrackendsListener implements ActionListener {

		@Override
		public void actionPerformed(final ActionEvent arg0) {

			// add listener to the imageplus slice slider
			sliceObserver = new SliceObserver(preprocessedimp, new ImagePlusListener());

			preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), preprocessedimp.getFrame());
			imp.setPosition(channel, preprocessedimp.getSlice(), preprocessedimp.getFrame());

			int next = preprocessedimp.getFrame();
			
		
			
			
			 maxStack();
			for (int index = next; index <= stacksize; ++index) {
				
				currentframe = index;
				preprocessedimp.setPosition(channel, preprocessedimp.getSlice(), currentframe);
				imp.setPosition(channel, preprocessedimp.getSlice(), currentframe);
				
				
			updatePreview(ValueChange.FRAME);
			isStarted = true;

			Roi roi = preprocessedimp.getRoi();
			if (roi == null) {
				// IJ.log( "A rectangular ROI is required to define the area..."
				// );
				preprocessedimp.setRoi(standardRectangle);
				roi = preprocessedimp.getRoi();
			}
			IJ.log("Current frame: " + currentframe);
			
			boolean dialog;
			boolean dialogupdate;
			

			if (ndims > 2) {

				RandomAccessibleInterval<FloatType> groundframe = currentimg;
				RandomAccessibleInterval<FloatType> groundframepre = currentPreprocessedimg;
				if (FindLinesViaMSER) {
					if (index == next)
						dialog = DialogueModelChoice();
					
						else dialog = false;
					
					

					updatePreview(ValueChange.SHOWMSER);

						LinefinderInteractiveHFMSER newlineMser = new LinefinderInteractiveHFMSER(groundframe,
								groundframepre, newtree, minlength, currentframe);

						returnVector = FindlinesVia.LinefindingMethodHF(groundframe, groundframepre, PrevFrameparam,
								minlength, currentframe, psf, newlineMser, userChoiceModel);
						

				}

				if (FindLinesViaHOUGH) {

					if (index == next)
						dialog = DialogueModelChoice();
					  
						else dialog = false;
					
					
					
					updatePreview(ValueChange.SHOWHOUGH);
						LinefinderInteractiveHFHough newlineHough = new LinefinderInteractiveHFHough(groundframe,
								groundframepre, intimg, Maxlabel, thetaPerPixel, rhoPerPixel, currentframe);

						returnVector = FindlinesVia.LinefindingMethodHF(groundframe, groundframepre, PrevFrameparam,
								minlength, currentframe, psf, newlineHough, userChoiceModel);
					
				}

				if (FindLinesViaMSERwHOUGH) {
					if (index == next)
						dialog = DialogueModelChoice();
						else dialog = false;
					
					updatePreview(ValueChange.SHOWMSER);
						LinefinderInteractiveHFMSERwHough newlineMserwHough = new LinefinderInteractiveHFMSERwHough(
								groundframe, groundframepre, newtree, minlength, currentframe, thetaPerPixel, rhoPerPixel);
						returnVector = FindlinesVia.LinefindingMethodHF(groundframe, groundframepre, PrevFrameparam,
								minlength, currentframe, psf, newlineMserwHough, userChoiceModel);
						
						

				}

			}
			

			 NewFrameparam = returnVector.snd;

			ArrayList<Trackproperties> startStateVectors = returnVector.fst.fst;
			ArrayList<Trackproperties> endStateVectors = returnVector.fst.snd;

			PrevFrameparam = NewFrameparam;

			Allstart.add(startStateVectors);
			Allend.add(endStateVectors);
			
			}
			
		

			ImagePlus impstartsec = ImageJFunctions.show(originalimg);
			ImagePlus impendsec = ImageJFunctions.show(originalPreprocessedimg);

			final Trackstart trackerstart = new Trackstart(Allstart,stacksize - next);
			final Trackend trackerend = new Trackend(Allend, stacksize - next);
			trackerstart.process();
			SimpleWeightedGraph<double[], DefaultWeightedEdge> graphstart = trackerstart.getResult();
			DisplayGraph displaygraphtrackstart = new DisplayGraph(impstartsec, graphstart);
			displaygraphtrackstart.getImp();
			impstartsec.draw();

			trackerend.process();
			SimpleWeightedGraph<double[], DefaultWeightedEdge> graphend = trackerend.getResult();
			DisplayGraph displaygraphtrackend = new DisplayGraph(impendsec, graphend);
			displaygraphtrackend.getImp();
			impendsec.draw();
			
			
			
		}
	}

	

	

	protected class MserListener implements ItemListener {
		@Override
		public void itemStateChanged(final ItemEvent arg0) {
			boolean oldState = FindLinesViaMSER;

			if (arg0.getStateChange() == ItemEvent.DESELECTED)
				FindLinesViaMSER = false;
			else if (arg0.getStateChange() == ItemEvent.SELECTED) {

				FindLinesViaMSER = true;
				FindLinesViaHOUGH = false;
				FindLinesViaMSERwHOUGH = false;
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
				FindLinesViaHOUGH = false;
			else if (arg0.getStateChange() == ItemEvent.SELECTED) {

				FindLinesViaHOUGH = true;
				FindLinesViaMSER = false;
				FindLinesViaMSERwHOUGH = false;
				DisplayHough();
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
				FindLinesViaMSERwHOUGH = false;
			else if (arg0.getStateChange() == ItemEvent.SELECTED) {

				FindLinesViaMSERwHOUGH = true;
				FindLinesViaMSER = false;
				FindLinesViaHOUGH = false;
				DisplayMSERwHough();
				
			}

			if (FindLinesViaMSERwHOUGH != oldState) {
				while (isComputing)
					SimpleMultiThreading.threadWait(10);

				updatePreview(ValueChange.FindLinesVia);
			}
		}
	}

	private void DisplayHough() {

		// Create dialog
		final Frame frame = new Frame("Interactive Hough");
		frame.setSize(550, 550);
		frame.setLayout(new BorderLayout());
		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		final Label exthresholdText = new Label("threshold = threshold to create Bitimg for watershedding.",
				Label.CENTER);
		final Label exthetaText = new Label("thetaPerPixel = Pixel Size in theta direction for Hough space.",
				Label.CENTER);
		final Label exrhoText = new Label("rhoPerPixel = Pixel Size in rho direction for Hough space.",
				Label.CENTER);
		
		// IJ.log("Determining the initial threshold for the image");
		// thresholdHoughInit =
		// GlobalThresholding.AutomaticThresholding(currentPreprocessedimg);
		final Scrollbar threshold = new Scrollbar(Scrollbar.HORIZONTAL, (int) thresholdHoughInit, 10, 0,
				10 + scrollbarSize);
		this.thresholdHough = computeValueFromScrollbarPosition((int) thresholdHoughInit, thresholdHoughMin,
				thresholdHoughMax, scrollbarSize);

		final Scrollbar thetaSize = new Scrollbar(Scrollbar.HORIZONTAL, (int) thetaPerPixelInit, 10, 0,
				10 + scrollbarSize);
		this.thetaPerPixel = computeValueFromScrollbarPosition((int) thetaPerPixelInit, thetaPerPixelMin,
				thetaPerPixelMax, scrollbarSize);

		final Scrollbar rhoSize = new Scrollbar(Scrollbar.HORIZONTAL, (int) rhoPerPixelInit, 10, 0, 10 + scrollbarSize);
		this.rhoPerPixel = computeValueFromScrollbarPosition((int) rhoPerPixelInit, rhoPerPixelMin, rhoPerPixelMax,
				scrollbarSize);

		final Checkbox displayBit = new Checkbox("Display Bitimage ", displayBitimg);
		final Checkbox displayWatershed = new Checkbox("Display Watershedimage ", displayWatershedimg);
		final Label thresholdText = new Label("thresholdValue = ", Label.CENTER);
		final Label thetaText = new Label("Size of Hough Space in Theta = ", Label.CENTER);
		final Label rhoText = new Label("Size of Hough Space in Rho = ", Label.CENTER);
		final Button Dowatershed = new Button("Do watershedding");
		final Button FindLinesListener = new Button("Find endpoints");
		final Button button = new Button("Done");
		/* Location */
		frame.setLayout(layout);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 4;
		c.weighty = 1.5;

		frame.add(exthresholdText, c);
		++c.gridy;
		
		frame.add(exthetaText, c);
		++c.gridy;
		
		frame.add(exrhoText, c);
		++c.gridy;
		
		frame.add(thresholdText, c);
		++c.gridy;
		
		frame.add(threshold, c);
		++c.gridy;

		

		frame.add(thetaText, c);
		++c.gridy;
		frame.add(thetaSize, c);
		++c.gridy;

		frame.add(rhoText, c);

		++c.gridy;

		frame.add(rhoSize, c);

		++c.gridy;
		c.insets = new Insets(10, 175, 0, 175);
		frame.add(displayBit, c);

		++c.gridy;
		c.insets = new Insets(10, 175, 0, 175);
		frame.add(displayWatershed, c);
		++c.gridy;
		c.insets = new Insets(10, 175, 0, 175);
		frame.add(Dowatershed, c);
		++c.gridy;
		
		c.insets = new Insets(10, 175, 0, 175);
		frame.add(FindLinesListener, c);
		++c.gridy;
		c.insets = new Insets(10, 175, 0, 175);
		frame.add(button, c);

		button.addActionListener(new FinishedButtonListener(frame, false));
		threshold.addAdjustmentListener(new thresholdHoughListener(thresholdText, thresholdHoughMin, thresholdHoughMax,
				scrollbarSize, threshold));

		thetaSize.addAdjustmentListener(
				new thetaSizeHoughListener(thetaText, rhoText, thetaPerPixelMin, thetaPerPixelMax, scrollbarSize, thetaSize, rhoSize));

		rhoSize.addAdjustmentListener(
				new rhoSizeHoughListener(rhoText, rhoPerPixelMin, rhoPerPixelMax, scrollbarSize, rhoSize));

		displayBit.addItemListener(new ShowBitimgListener());
		displayWatershed.addItemListener(new ShowwatershedimgListener());
		Dowatershed.addActionListener(new DowatershedListener());
		FindLinesListener.addActionListener(new FindLinesListener());
		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);

		originalColor = threshold.getBackground();

	}

	private void DisplayMSER() {

		// Create dialog
		final Frame frame = new Frame("Interactive Mser");
		frame.setSize(550, 550);
		frame.setLayout(new BorderLayout());
		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		final Label exdeltaText = new Label("delta = stepsize of thresholds.", Label.CENTER);
		final Label exmaxVarText = new Label("maxVar = maximum instability score of the region. ", Label.CENTER);
		final Label exminDiversityText = new Label("minDiversity = minimum diversity of adjacent regions. ",
				Label.CENTER);
		final Label exminSizeText = new Label("MinSize = mimimum size of accepted region. ", Label.CENTER);
		final Label exmaxSizeText = new Label("MaxSize = maximum size of accepted region. ", Label.CENTER);

		final Scrollbar delta = new Scrollbar(Scrollbar.HORIZONTAL, deltaInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar maxVar = new Scrollbar(Scrollbar.HORIZONTAL, maxVarInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar minDiversity = new Scrollbar(Scrollbar.HORIZONTAL, minDiversityInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar minSize = new Scrollbar(Scrollbar.HORIZONTAL, minSizeInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar maxSize = new Scrollbar(Scrollbar.HORIZONTAL, maxSizeInit, 10, 0, 10 + scrollbarSize);
		final Button ComputeTree = new Button("Compute Tree and display");
		final Button FindLinesListener = new Button("Find endpoints");
		this.maxVar = computeValueFromScrollbarPosition(maxVarInit, maxVarMin, maxVarMax, scrollbarSize);
		this.delta = computeValueFromScrollbarPosition(deltaInit, deltaMin, deltaMax, scrollbarSize);
		this.minDiversity = computeValueFromScrollbarPosition(minDiversityInit, minDiversityMin, minDiversityMax,
				scrollbarSize);
		this.minSize = (int) computeValueFromScrollbarPosition(minSizeInit, minSizemin, minSizemax, scrollbarSize);
		this.maxSize = (int) computeValueFromScrollbarPosition(maxSizeInit, maxSizemin, maxSizemax, scrollbarSize);

		final Checkbox min = new Checkbox("Look for Minima ", darktobright);

		final Label deltaText = new Label("delta = ", Label.CENTER);
		final Label maxVarText = new Label("maxVar = ", Label.CENTER);
		final Label minDiversityText = new Label("minDiversity = ", Label.CENTER);
		final Label minSizeText = new Label("MinSize = ", Label.CENTER);
		final Label maxSizeText = new Label("MaxSize = ", Label.CENTER);
		final Button button = new Button("Done");
		/* Location */
		frame.setLayout(layout);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 4;
		c.weighty = 1.5;

		frame.add(exdeltaText, c);
		++c.gridy;

		frame.add(exmaxVarText, c);
		++c.gridy;

		frame.add(exminDiversityText, c);
		++c.gridy;

		frame.add(exminSizeText, c);
		++c.gridy;

		frame.add(exmaxSizeText, c);
		++c.gridy;

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

		++c.gridy;
		c.insets = new Insets(10, 180, 0, 180);
		frame.add(FindLinesListener, c);
		
		++c.gridy;
		c.insets = new Insets(10, 180, 0, 180);
		frame.add(button, c);

		delta.addAdjustmentListener(new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta));

		maxVar.addAdjustmentListener(new maxVarListener(maxVarText, maxVarMin, maxVarMax, scrollbarSize, maxVar));

		minDiversity.addAdjustmentListener(new minDiversityListener(minDiversityText, minDiversityMin, minDiversityMax,
				scrollbarSize, minDiversity));

		minSize.addAdjustmentListener(new minSizeListener(minSizeText, minSizemin, minSizemax, scrollbarSize, minSize));

		maxSize.addAdjustmentListener(new maxSizeListener(maxSizeText, maxSizemin, maxSizemax, scrollbarSize, maxSize));
		button.addActionListener(new FinishedButtonListener(frame, false));

		delta.addAdjustmentListener(new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta));
		maxVar.addAdjustmentListener(new maxVarListener(maxVarText, maxVarMin, maxVarMax, scrollbarSize, maxVar));
		minDiversity.addAdjustmentListener(new minDiversityListener(minDiversityText, minDiversityMin, minDiversityMax,
				scrollbarSize, minDiversity));
		minSize.addAdjustmentListener(new minSizeListener(minSizeText, minSizemin, minSizemax, scrollbarSize, minSize));
		maxSize.addAdjustmentListener(new maxSizeListener(maxSizeText, maxSizemin, maxSizemax, scrollbarSize, maxSize));
		min.addItemListener(new DarktobrightListener());
		ComputeTree.addActionListener(new ComputeTreeListener());
		FindLinesListener.addActionListener(new FindLinesListener());
		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);

		originalColor = delta.getBackground();

	}

	
	private void DisplayMSERwHough() {

		// Create dialog
		final Frame frame = new Frame("Interactive Mser with Hough");
		frame.setSize(750, 750);
		frame.setLayout(new BorderLayout());
		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		
		final Label exthetaText = new Label("thetaPerPixel = Pixel Size in theta direction for Hough space.",
				Label.CENTER);
		final Label exrhoText = new Label("rhoPerPixel = Pixel Size in rho direction for Hough space.",
				Label.CENTER);
		
	    final Checkbox rhoEnable = new Checkbox( "Enable Manual Adjustment of rhoPerPixel", enablerhoPerPixel );


		final Scrollbar thetaSize = new Scrollbar(Scrollbar.HORIZONTAL, (int) thetaPerPixelInit, 10, 0,
				10 + scrollbarSize);
		this.thetaPerPixel = computeValueFromScrollbarPosition((int) thetaPerPixelInit, thetaPerPixelMin,
				thetaPerPixelMax, scrollbarSize);

		final Scrollbar rhoSize = new Scrollbar(Scrollbar.HORIZONTAL, (int) rhoPerPixelInit, 10, 0, 10 + scrollbarSize);
		this.rhoPerPixel = computeValueFromScrollbarPosition((int) rhoPerPixelInit, rhoPerPixelMin, rhoPerPixelMax,
				scrollbarSize);

		
		final Label thetaText = new Label("Size of Hough Space in Theta = ", Label.CENTER);
		final Label rhoText = new Label("Size of Hough Space in Rho = ", Label.CENTER);
		final Button FindLinesListener = new Button("Find endpoints");
		final Button button = new Button("Done");
	
		
		
		final Label exdeltaText = new Label("delta = stepsize of thresholds.", Label.CENTER);
		
		final Label exmaxVarText = new Label("maxVar = maximum instability score of the region. ", Label.CENTER);
		final Label exminDiversityText = new Label("minDiversity = minimum diversity of adjacent regions. ",
				Label.CENTER);
		final Label exminSizeText = new Label("MinSize = mimimum size of accepted region. ", Label.CENTER);
		final Label exmaxSizeText = new Label("MaxSize = maximum size of accepted region. ", Label.CENTER);

		final Scrollbar delta = new Scrollbar(Scrollbar.HORIZONTAL, deltaInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar maxVar = new Scrollbar(Scrollbar.HORIZONTAL, maxVarInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar minDiversity = new Scrollbar(Scrollbar.HORIZONTAL, minDiversityInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar minSize = new Scrollbar(Scrollbar.HORIZONTAL, minSizeInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar maxSize = new Scrollbar(Scrollbar.HORIZONTAL, maxSizeInit, 10, 0, 10 + scrollbarSize);
		final Button ComputeTree = new Button("Compute Tree and display");

		final Label HoughText = new Label("Now determine the Hough space parameters.", Label.CENTER);
		
		this.maxVar = computeValueFromScrollbarPosition(maxVarInit, maxVarMin, maxVarMax, scrollbarSize);
		this.delta = computeValueFromScrollbarPosition(deltaInit, deltaMin, deltaMax, scrollbarSize);
		this.minDiversity = computeValueFromScrollbarPosition(minDiversityInit, minDiversityMin, minDiversityMax,
				scrollbarSize);
		this.minSize = (int) computeValueFromScrollbarPosition(minSizeInit, minSizemin, minSizemax, scrollbarSize);
		this.maxSize = (int) computeValueFromScrollbarPosition(maxSizeInit, maxSizemin, maxSizemax, scrollbarSize);

		final Checkbox min = new Checkbox("Look for Minima ", darktobright);

		final Label deltaText = new Label("delta = ", Label.CENTER);
		final Label maxVarText = new Label("maxVar = ", Label.CENTER);
		final Label minDiversityText = new Label("minDiversity = ", Label.CENTER);
		final Label minSizeText = new Label("MinSize = ", Label.CENTER);
		final Label maxSizeText = new Label("MaxSize = ", Label.CENTER);
		/* Location */
		frame.setLayout(layout);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 4;
		c.weighty = 1.5;

		frame.add(exdeltaText, c);
		++c.gridy;

		frame.add(exmaxVarText, c);
		++c.gridy;

		frame.add(exminDiversityText, c);
		++c.gridy;

		frame.add(exminSizeText, c);
		++c.gridy;

		frame.add(exmaxSizeText, c);
		++c.gridy;

		
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
		++c.gridy;
		
		frame.add(HoughText, c);
		++c.gridy;
		
		
		frame.add(exthetaText, c);
		++c.gridy;
		
		frame.add(exrhoText, c);
		
		
		++c.gridy;
		frame.add(thetaText, c);
		++c.gridy;
		frame.add(thetaSize, c);
		++c.gridy;

		frame.add(rhoText, c);

		++c.gridy;

		frame.add(rhoSize, c);
		
		++c.gridy;
		 c.insets = new Insets(0,175,0,175);
		frame.add(rhoEnable, c);

		++c.gridy;
		c.insets = new Insets(10, 175, 0, 175);
		frame.add(FindLinesListener, c);
	
		
		++c.gridy;
		c.insets = new Insets(10, 175, 0, 175);
		frame.add(button, c);

		delta.addAdjustmentListener(new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta));

		maxVar.addAdjustmentListener(new maxVarListener(maxVarText, maxVarMin, maxVarMax, scrollbarSize, maxVar));

		minDiversity.addAdjustmentListener(new minDiversityListener(minDiversityText, minDiversityMin, minDiversityMax,
				scrollbarSize, minDiversity));

		minSize.addAdjustmentListener(new minSizeListener(minSizeText, minSizemin, minSizemax, scrollbarSize, minSize));

		maxSize.addAdjustmentListener(new maxSizeListener(maxSizeText, maxSizemin, maxSizemax, scrollbarSize, maxSize));
		button.addActionListener(new FinishedButtonListener(frame, false));

		delta.addAdjustmentListener(new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta));
		maxVar.addAdjustmentListener(new maxVarListener(maxVarText, maxVarMin, maxVarMax, scrollbarSize, maxVar));
		minDiversity.addAdjustmentListener(new minDiversityListener(minDiversityText, minDiversityMin, minDiversityMax,
				scrollbarSize, minDiversity));
		minSize.addAdjustmentListener(new minSizeListener(minSizeText, minSizemin, minSizemax, scrollbarSize, minSize));
		maxSize.addAdjustmentListener(new maxSizeListener(maxSizeText, maxSizemin, maxSizemax, scrollbarSize, maxSize));
		min.addItemListener(new DarktobrightListener());

		
		
		FindLinesListener.addActionListener(new FindLinesListener());

		thetaSize.addAdjustmentListener(
				new thetaSizeHoughListener(thetaText, rhoText, thetaPerPixelMin, thetaPerPixelMax, scrollbarSize, thetaSize, rhoSize));

		rhoSize.addAdjustmentListener(
				new rhoSizeHoughListener(rhoText, rhoPerPixelMin, rhoPerPixelMax, scrollbarSize, rhoSize));

		ComputeTree.addActionListener(new ComputeTreeListener());
		
		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);

		originalColor = delta.getBackground();

	}

	
	
	
	
	
	private void UpdateHough() {

		// Create dialog
				final Frame frame = new Frame("update Hough");
				frame.setSize(550, 550);
				frame.setLayout(new BorderLayout());
				/* Instantiation */
				final GridBagLayout layout = new GridBagLayout();
				final GridBagConstraints c = new GridBagConstraints();
				final Label exthresholdText = new Label("threshold = threshold to create Bitimg for watershedding.",
						Label.CENTER);
				final Label exthetaText = new Label("thetaPerPixel = Pixel Size in theta direction for Hough space.",
						Label.CENTER);
				final Label exrhoText = new Label("rhoPerPixel = Pixel Size in rho direction for Hough space.",
						Label.CENTER);
				
				// IJ.log("Determining the initial threshold for the image");
				// thresholdHoughInit =
				// GlobalThresholding.AutomaticThresholding(currentPreprocessedimg);
				final Scrollbar threshold = new Scrollbar(Scrollbar.HORIZONTAL, (int) thresholdHoughInit, 10, 0,
						10 + scrollbarSize);
				this.thresholdHough = computeValueFromScrollbarPosition((int) thresholdHoughInit, thresholdHoughMin,
						thresholdHoughMax, scrollbarSize);

				final Scrollbar thetaSize = new Scrollbar(Scrollbar.HORIZONTAL, (int) thetaPerPixelInit, 10, 0,
						10 + scrollbarSize);
				this.thetaPerPixel = computeValueFromScrollbarPosition((int) thetaPerPixelInit, thetaPerPixelMin,
						thetaPerPixelMax, scrollbarSize);

				final Scrollbar rhoSize = new Scrollbar(Scrollbar.HORIZONTAL, (int) rhoPerPixelInit, 10, 0, 10 + scrollbarSize);
				this.rhoPerPixel = computeValueFromScrollbarPosition((int) rhoPerPixelInit, rhoPerPixelMin, rhoPerPixelMax,
						scrollbarSize);

				final Checkbox displayBit = new Checkbox("Display Bitimage ", displayBitimg);
				final Checkbox displayWatershed = new Checkbox("Display Watershedimage ", displayWatershedimg);
				final Label thresholdText = new Label("thresholdValue = ", Label.CENTER);
				final Label thetaText = new Label("Size of Hough Space in Theta = ", Label.CENTER);
				final Label rhoText = new Label("Size of Hough Space in Rho = ", Label.CENTER);
				final Button Dowatershed = new Button("Do watershedding");
				final Button button = new Button("Done");
				/* Location */
				frame.setLayout(layout);

				c.fill = GridBagConstraints.HORIZONTAL;
				c.gridx = 0;
				c.gridy = 0;
				c.weightx = 4;
				c.weighty = 1.5;

				frame.add(exthresholdText, c);
				++c.gridy;
				
				frame.add(exthetaText, c);
				++c.gridy;
				
				frame.add(exrhoText, c);
				++c.gridy;
				
				frame.add(thresholdText, c);
				++c.gridy;
				
				frame.add(threshold, c);
				++c.gridy;

				

				frame.add(thetaText, c);
				++c.gridy;
				frame.add(thetaSize, c);
				++c.gridy;

				frame.add(rhoText, c);

				++c.gridy;

				frame.add(rhoSize, c);

				++c.gridy;
				c.insets = new Insets(10, 175, 0, 175);
				frame.add(displayBit, c);

				++c.gridy;
				c.insets = new Insets(10, 175, 0, 175);
				frame.add(displayWatershed, c);
				++c.gridy;
				c.insets = new Insets(10, 175, 0, 175);
				frame.add(Dowatershed, c);
				
				++c.gridy;
				c.insets = new Insets(10, 175, 0, 175);
				frame.add(button, c);

				button.addActionListener(new DoneandmovebackButtonListener(frame, false));

				threshold.addAdjustmentListener(new thresholdHoughListener(thresholdText, thresholdHoughMin, thresholdHoughMax,
						scrollbarSize, threshold));

				thetaSize.addAdjustmentListener(
						new thetaSizeHoughListener(thetaText, rhoText, thetaPerPixelMin, thetaPerPixelMax, scrollbarSize, thetaSize, rhoSize));

				rhoSize.addAdjustmentListener(
						new rhoSizeHoughListener(rhoText, rhoPerPixelMin, rhoPerPixelMax, scrollbarSize, rhoSize));

				displayBit.addItemListener(new ShowBitimgListener());
				displayWatershed.addItemListener(new ShowwatershedimgListener());
				Dowatershed.addActionListener(new DowatershedListener());
				frame.addWindowListener(new FrameListener(frame));

				frame.setVisible(true);

				originalColor = threshold.getBackground();
	}

	private void UpdateMSER() {

		// Create dialog
		
				
	
				
				Frame frame = new Frame("Update MSER Parameters");
				frame.setSize(550, 550);
				frame.setLayout(new BorderLayout());
				final Scrollbar delta = new Scrollbar(Scrollbar.HORIZONTAL, deltaInit, 10, 0, 10 + scrollbarSize);
				final Scrollbar maxVar = new Scrollbar(Scrollbar.HORIZONTAL, maxVarInit, 10, 0, 10 + scrollbarSize);
				final Scrollbar minDiversity = new Scrollbar(Scrollbar.HORIZONTAL, minDiversityInit, 10, 0, 10 + scrollbarSize);
				final Scrollbar minSize = new Scrollbar(Scrollbar.HORIZONTAL, minSizeInit, 10, 0, 10 + scrollbarSize);
				final Scrollbar maxSize = new Scrollbar(Scrollbar.HORIZONTAL, maxSizeInit, 10, 0, 10 + scrollbarSize);
				this.maxVar = computeValueFromScrollbarPosition(maxVarInit, maxVarMin, maxVarMax, scrollbarSize);
				this.delta = computeValueFromScrollbarPosition(deltaInit, deltaMin, deltaMax, scrollbarSize);
				this.minDiversity = computeValueFromScrollbarPosition(minDiversityInit, minDiversityMin, minDiversityMax,
						scrollbarSize);
				this.minSize = (int) computeValueFromScrollbarPosition(minSizeInit, minSizemin, minSizemax, scrollbarSize);
				this.maxSize = (int) computeValueFromScrollbarPosition(maxSizeInit, maxSizemin, maxSizemax, scrollbarSize);

				final Checkbox min = new Checkbox("Look for Minima ", darktobright);
				/* Instantiation */
				final GridBagLayout layout = new GridBagLayout();
				final GridBagConstraints c = new GridBagConstraints();
				final Button ComputeTree = new Button("Compute Tree and display");
				final Button button = new Button("Done");
				/* Location */
				final Label deltaText = new Label("delta = ", Label.CENTER);
				final Label maxVarText = new Label("maxVar = ", Label.CENTER);
				final Label minDiversityText = new Label("minDiversity = ", Label.CENTER);
				final Label minSizeText = new Label("MinSize = ", Label.CENTER);
				final Label maxSizeText = new Label("MaxSize = ", Label.CENTER);
				frame.setLayout(layout);

				c.fill = GridBagConstraints.HORIZONTAL;
				c.gridx = 0;
				c.gridy = 0;
				c.weightx = 4;
				c.weighty = 1.5;
				++c.gridy;

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

			
				
				++c.gridy;
				c.insets = new Insets(10, 180, 0, 180);
				frame.add(button, c);

				delta.addAdjustmentListener(new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta));

				maxVar.addAdjustmentListener(new maxVarListener(maxVarText, maxVarMin, maxVarMax, scrollbarSize, maxVar));

				minDiversity.addAdjustmentListener(new minDiversityListener(minDiversityText, minDiversityMin, minDiversityMax,
						scrollbarSize, minDiversity));

				minSize.addAdjustmentListener(new minSizeListener(minSizeText, minSizemin, minSizemax, scrollbarSize, minSize));

				maxSize.addAdjustmentListener(new maxSizeListener(maxSizeText, maxSizemin, maxSizemax, scrollbarSize, maxSize));
				button.addActionListener(new DoneandmovebackButtonListener(frame, false));

				delta.addAdjustmentListener(new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta));
				maxVar.addAdjustmentListener(new maxVarListener(maxVarText, maxVarMin, maxVarMax, scrollbarSize, maxVar));
				minDiversity.addAdjustmentListener(new minDiversityListener(minDiversityText, minDiversityMin, minDiversityMax,
						scrollbarSize, minDiversity));
				minSize.addAdjustmentListener(new minSizeListener(minSizeText, minSizemin, minSizemax, scrollbarSize, minSize));
				maxSize.addAdjustmentListener(new maxSizeListener(maxSizeText, maxSizemin, maxSizemax, scrollbarSize, maxSize));
				min.addItemListener(new DarktobrightListener());
				ComputeTree.addActionListener(new ComputeTreeListener());
				frame.addWindowListener(new FrameListener(frame));

				frame.setVisible(true);

				originalColor = delta.getBackground();
				
				
	}

	
	private void UpdateMSERwHough() {

		GenericDialog gd = new GenericDialog("Update MSERwHough Parameters");
		

		
		Frame frame = new Frame("Update MSER Parameters");
		frame.setSize(550, 550);
		frame.setLayout(new BorderLayout());
		final Scrollbar delta = new Scrollbar(Scrollbar.HORIZONTAL, deltaInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar maxVar = new Scrollbar(Scrollbar.HORIZONTAL, maxVarInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar minDiversity = new Scrollbar(Scrollbar.HORIZONTAL, minDiversityInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar minSize = new Scrollbar(Scrollbar.HORIZONTAL, minSizeInit, 10, 0, 10 + scrollbarSize);
		final Scrollbar maxSize = new Scrollbar(Scrollbar.HORIZONTAL, maxSizeInit, 10, 0, 10 + scrollbarSize);
		this.maxVar = computeValueFromScrollbarPosition(maxVarInit, maxVarMin, maxVarMax, scrollbarSize);
		this.delta = computeValueFromScrollbarPosition(deltaInit, deltaMin, deltaMax, scrollbarSize);
		this.minDiversity = computeValueFromScrollbarPosition(minDiversityInit, minDiversityMin, minDiversityMax,
				scrollbarSize);
		this.minSize = (int) computeValueFromScrollbarPosition(minSizeInit, minSizemin, minSizemax, scrollbarSize);
		this.maxSize = (int) computeValueFromScrollbarPosition(maxSizeInit, maxSizemin, maxSizemax, scrollbarSize);

		final Checkbox min = new Checkbox("Look for Minima ", darktobright);
		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		final Button ComputeTree = new Button("Compute Tree and display");
		final Button button = new Button("Done");
		/* Location */
		final Label deltaText = new Label("delta = ", Label.CENTER);
		final Label maxVarText = new Label("maxVar = ", Label.CENTER);
		final Label minDiversityText = new Label("minDiversity = ", Label.CENTER);
		final Label minSizeText = new Label("MinSize = ", Label.CENTER);
		final Label maxSizeText = new Label("MaxSize = ", Label.CENTER);
		frame.setLayout(layout);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 4;
		c.weighty = 1.5;
		++c.gridy;

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

	
		
		++c.gridy;
		c.insets = new Insets(10, 180, 0, 180);
		frame.add(button, c);

		delta.addAdjustmentListener(new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta));

		maxVar.addAdjustmentListener(new maxVarListener(maxVarText, maxVarMin, maxVarMax, scrollbarSize, maxVar));

		minDiversity.addAdjustmentListener(new minDiversityListener(minDiversityText, minDiversityMin, minDiversityMax,
				scrollbarSize, minDiversity));

		minSize.addAdjustmentListener(new minSizeListener(minSizeText, minSizemin, minSizemax, scrollbarSize, minSize));

		maxSize.addAdjustmentListener(new maxSizeListener(maxSizeText, maxSizemin, maxSizemax, scrollbarSize, maxSize));
		button.addActionListener(new DoneandmovebackButtonListener(frame, false));

		delta.addAdjustmentListener(new DeltaListener(deltaText, deltaMin, deltaMax, scrollbarSize, delta));
		maxVar.addAdjustmentListener(new maxVarListener(maxVarText, maxVarMin, maxVarMax, scrollbarSize, maxVar));
		minDiversity.addAdjustmentListener(new minDiversityListener(minDiversityText, minDiversityMin, minDiversityMax,
				scrollbarSize, minDiversity));
		minSize.addAdjustmentListener(new minSizeListener(minSizeText, minSizemin, minSizemax, scrollbarSize, minSize));
		maxSize.addAdjustmentListener(new maxSizeListener(maxSizeText, maxSizemin, maxSizemax, scrollbarSize, maxSize));
		min.addItemListener(new DarktobrightListener());
		ComputeTree.addActionListener(new ComputeTreeListener());
		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);

		originalColor = delta.getBackground();
		
		

	}

	
	protected class FinishedButtonListener implements ActionListener {
		final Frame parent;
		final boolean cancel;

		public FinishedButtonListener(Frame parent, final boolean cancel) {
			this.parent = parent;
			this.cancel = cancel;
		}

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			wasCanceled = cancel;
			close(parent, sliceObserver, roiListener);
		}
	}

	
	protected class DoneandmovebackButtonListener implements ActionListener {
		final Frame parent;
		final boolean cancel;

		public DoneandmovebackButtonListener(Frame parent, final boolean cancel) {
			this.parent = parent;
			this.cancel = cancel;
		}

		@Override
		public void actionPerformed(final ActionEvent arg0) {
			wasCanceled = cancel;
			close(parent, sliceObserver, roiListener);
			preprocessedimp.setPosition(channel, 0, 1);
			imp.setPosition(channel, 0, 1);
			
		}
	}
	
	protected class ComputeTreeListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			ShowMser = true;
			updatePreview(ValueChange.SHOWMSER);

		}
	}

	protected class DowatershedListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			ShowHough = true;
			updatePreview(ValueChange.SHOWHOUGH);

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

	protected class ShowBitimgListener implements ItemListener {
		@Override
		public void itemStateChanged(final ItemEvent arg0) {
			boolean oldState = displayBitimg;

			if (arg0.getStateChange() == ItemEvent.DESELECTED)
				displayBitimg = false;
			else if (arg0.getStateChange() == ItemEvent.SELECTED)
				displayBitimg = true;

			if (displayBitimg != oldState) {
				while (isComputing)
					SimpleMultiThreading.threadWait(10);

				updatePreview(ValueChange.DISPLAYBITIMG);
			}
		}
	}

	protected class ShowwatershedimgListener implements ItemListener {
		@Override
		public void itemStateChanged(final ItemEvent arg0) {
			boolean oldState = displayWatershedimg;

			if (arg0.getStateChange() == ItemEvent.DESELECTED)
				displayWatershedimg = false;
			else if (arg0.getStateChange() == ItemEvent.SELECTED)
				displayWatershedimg = true;

			if (displayWatershedimg != oldState) {
				while (isComputing)
					SimpleMultiThreading.threadWait(10);

				updatePreview(ValueChange.DISPLAYWATERSHEDIMG);
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

	protected class thetaSizeHoughListener implements AdjustmentListener {
		final Label label;
		final Label rholabel;
		final float min, max;
		final int scrollbarSize;

		final Scrollbar thetaScrollbar;
		final Scrollbar rhoScrollbar;

		public thetaSizeHoughListener(final Label label, final Label rholabel, final float min, final float max, final int scrollbarSize,
				final Scrollbar thetaScrollbar, final Scrollbar rhoScrollbar) {
			this.label = label;
			this.rholabel = rholabel;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;
			this.thetaScrollbar = thetaScrollbar;
			this.rhoScrollbar = rhoScrollbar;

		}

		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			thetaPerPixel = computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize);

			
			if (!enablerhoPerPixel)
			{
				rhoPerPixel = thetaPerPixel;
				rholabel.setText("rhoPerPixel = " + rhoPerPixel);
		        rhoScrollbar.setValue(computeScrollbarPositionFromValue(rhoPerPixel, min, max, scrollbarSize));
				
			}
			
			
			thetaScrollbar.setValue(computeScrollbarPositionFromValue(thetaPerPixel, min, max, scrollbarSize));

			label.setText("thetaPerPixel = " + thetaPerPixel);

			// if ( !event.getValueIsAdjusting() )
			{
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.thetaPerPixel);
			}
		}
	}

	protected class rhoSizeHoughListener implements AdjustmentListener {
		final Label label;
		final float min, max;
		final int scrollbarSize;

		final Scrollbar rhoScrollbar;

		public rhoSizeHoughListener(final Label label, final float min, final float max, final int scrollbarSize,
				final Scrollbar rhoScrollbar) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;

			this.rhoScrollbar = rhoScrollbar;

		}

		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			
			
			
			rhoPerPixel = computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize);

			rhoScrollbar.setValue(computeScrollbarPositionFromValue(rhoPerPixel, min, max, scrollbarSize));

			label.setText("rhoPerPixel = " + rhoPerPixel);

			// if ( !event.getValueIsAdjusting() )
			{
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.rhoPerPixel);
			}
		
	
		
		
		}
	}

	protected class thresholdHoughListener implements AdjustmentListener {
		final Label label;
		final float min, max;
		final int scrollbarSize;

		final Scrollbar thresholdScrollbar;

		public thresholdHoughListener(final Label label, final float min, final float max, final int scrollbarSize,
				final Scrollbar thresholdScrollbar) {
			this.label = label;
			this.min = min;
			this.max = max;
			this.scrollbarSize = scrollbarSize;

			this.thresholdScrollbar = thresholdScrollbar;

		}

		@Override
		public void adjustmentValueChanged(final AdjustmentEvent event) {
			thresholdHough = computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize);

			thresholdScrollbar.setValue(computeScrollbarPositionFromValue(thresholdHough, min, max, scrollbarSize));

			label.setText("thresholdBitimg = " + thresholdHough);

			// if ( !event.getValueIsAdjusting() )
			{
				while (isComputing) {
					SimpleMultiThreading.threadWait(10);
				}
				updatePreview(ValueChange.thresholdHough);
			}
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

			maxVarScrollbar.setValue(computeScrollbarPositionFromValue((float) maxVar, min, max, scrollbarSize));

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

			minDiversityScrollbar
					.setValue(computeScrollbarPositionFromValue((float) minDiversity, min, max, scrollbarSize));

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

	public ArrayList<EllipseRoi> getcurrentRois(MserTree<UnsignedByteType> newtree) {

		final HashSet<Mser<UnsignedByteType>> rootset = newtree.roots();

		ArrayList<EllipseRoi> Allrois = new ArrayList<EllipseRoi>();
		final Iterator<Mser<UnsignedByteType>> rootsetiterator = rootset.iterator();

		AllmeanCovar = new ArrayList<double[]>();

		while (rootsetiterator.hasNext()) {

			Mser<UnsignedByteType> rootmser = rootsetiterator.next();

			if (rootmser.size() > 0) {

				final double[] meanandcov = { rootmser.mean()[0], rootmser.mean()[1], rootmser.cov()[0],
						rootmser.cov()[1], rootmser.cov()[2] };
				AllmeanCovar.add(meanandcov);

			}
		}

		// We do this so the ROI remains attached the the same label and is not
		// changed if the program is run again
		SortListbyproperty.sortpointList(AllmeanCovar);
		for (int index = 0; index < AllmeanCovar.size(); ++index) {

			final double[] mean = { AllmeanCovar.get(index)[0], AllmeanCovar.get(index)[1] };
			final double[] covar = { AllmeanCovar.get(index)[2], AllmeanCovar.get(index)[3],
					AllmeanCovar.get(index)[4] };

			EllipseRoi roi = createEllipse(mean, covar, 3);
			Allrois.add(roi);

		}

		return Allrois;

	}

	public boolean DialogueNormalize() {
		// Create dialog
		GenericDialog gd = new GenericDialog("Image Intensity Normalization");
		gd.addNumericField(" Minimum Value for Intensity Normalization: ", minval.get(), 0);
		gd.addNumericField(" Maximum Value for Intensity Normalization: ", maxval.get(), 0);
		gd.showDialog();
		minval = new FloatType((int) gd.getNextNumber());
		maxval = new FloatType((int) gd.getNextNumber());

		return !gd.wasCanceled();
	}

	public boolean DialogueModelChoice() {

		GenericDialog gd = new GenericDialog("Model Choice for sub-pixel Localization");
		String[] LineModel = { "GaussianLines", "SecondOrderSpline", "ThridOrderSpline" };
		int indexmodel = 0;

		gd.addChoice("Choose your model: ", LineModel, LineModel[indexmodel]);
		
		gd.showDialog();
		indexmodel = gd.getNextChoiceIndex();
		
            if (indexmodel == 0)
			userChoiceModel = UserChoiceModel.Line;
            if (indexmodel == 1)
			userChoiceModel = UserChoiceModel.Splineordersec;
            if (indexmodel == 2)
			userChoiceModel = UserChoiceModel.Splineordersec;

          
	

		return !gd.wasCanceled();  
	}

	public boolean DialogueMedian() {
		// Create dialog
		GenericDialog gd = new GenericDialog("Choose the radius of the filter");
		gd.addNumericField("Radius:", radius, 0);
		gd.showDialog();
		radius = ((int) gd.getNextNumber());

		return !gd.wasCanceled();
	}

	protected RandomAccessibleInterval<FloatType> extractImage(final RandomAccessibleInterval<FloatType> intervalView) {

		final FloatType type = intervalView.randomAccess().get().createVariable();
		final ImgFactory<FloatType> factory = net.imglib2.util.Util.getArrayOrCellImgFactory(intervalView, type);
		RandomAccessibleInterval<FloatType> totalimg = factory.create(intervalView, type);

		final long[] location = new long[intervalView.numDimensions()];

		if (location.length > 2)
			location[2] = (preprocessedimp.getCurrentSlice() - 1) / preprocessedimp.getNChannels();
		
		long[] min = {(long) standardRectangle.getMinX() , (long) standardRectangle.getMinY()};
		
		long[] max = {(long) standardRectangle.getMaxX(), (long) standardRectangle.getMaxY()};
		
		FinalInterval interval =Intervals.createMinMax(min[0], min[1], max[0], max[1]);
		
		final RandomAccessibleInterval<FloatType> img = Views.interval( intervalView, interval );
		
		double maxint = GetLocalmaxmin.computeMaxIntensity(img);
		double minint = GetLocalmaxmin.computeMinIntensity(img);
		
		totalimg = Views.interval(Views.extendRandom(img, -0.02, -0.01), intervalView);
		
		return totalimg;
	}

	protected final void close(final Frame parent, final SliceObserver sliceObserver, final ImagePlus imp,
			RoiListener roiListener) {
		if (parent != null)
			parent.dispose();

		if (sliceObserver != null)
			sliceObserver.unregister();

		if (imp != null) {
			if (roiListener != null)
				imp.getCanvas().removeMouseListener(roiListener);
			if (imp.getOverlay()!=null){
			imp.getOverlay().clear();
			imp.updateAndDraw();
			}
		}

		isFinished = true;
	}

	protected final void close(final Frame parent, final SliceObserver sliceObserver, RoiListener roiListener) {
		if (parent != null)
			parent.dispose();

		if (sliceObserver != null)
			sliceObserver.unregister();
		if (roiListener != null)
			imp.getCanvas().removeMouseListener(roiListener);
			//imp.getOverlay().clear();

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
				updatePreview(ValueChange.FRAME);

			}
		}
	}

	/**
	 * Generic, type-agnostic method to create an identical copy of an Img
	 *
	 * @param input
	 *            - the Img to copy
	 * @return - the copy of the Img
	 */
	public <T extends Type<T>> Img<T> copyImage(final Img<T> input) {
		// create a new Image with the same properties
		// note that the input provides the size for the new image as it
		// implements
		// the Interval interface
		Img<T> output = input.factory().create(input, input.firstElement());

		// create a cursor for both images
		Cursor<T> cursorInput = input.cursor();
		Cursor<T> cursorOutput = output.cursor();

		// iterate over the input
		while (cursorInput.hasNext()) {
			// move both cursors forward by one pixel
			cursorInput.fwd();
			cursorOutput.fwd();

			// set the value of this pixel of the output image to the same as
			// the input,
			// every Type supports T.set( T type )
			cursorOutput.get().set(cursorInput.get());
		}

		// return the copy
		return output;
	}

	/**
	 * Generic, type-agnostic method to create an identical copy of an Img
	 *
	 * @param currentPreprocessedimg2
	 *            - the Img to copy
	 * @return - the copy of the Img
	 */
	public Img<UnsignedByteType> copytoByteImage(final RandomAccessibleInterval<FloatType> input) {
		// create a new Image with the same properties
		// note that the input provides the size for the new image as it
		// implements
		// the Interval interface

		Normalize.normalize(Views.iterable(input), new FloatType(0), new FloatType(255));
		final UnsignedByteType type = new UnsignedByteType();
		final ImgFactory<UnsignedByteType> factory = net.imglib2.util.Util.getArrayOrCellImgFactory(input, type);
		final Img<UnsignedByteType> output = factory.create(input, type);
		// create a cursor for both images
		Cursor<FloatType> cursorInput = Views.iterable(input).cursor();
		Cursor<UnsignedByteType> cursorOutput = output.cursor();

		// iterate over the input
		while (cursorInput.hasNext()) {
			// move both cursors forward by one pixel
			cursorInput.fwd();
			cursorOutput.fwd();

			// set the value of this pixel of the output image to the same as
			// the input,
			// every Type supports T.set( T type )
			cursorOutput.get().set((int) cursorInput.get().get());
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
		final double x = mean[0];
		final double y = mean[1];
		final double dx = scale1 * Math.cos(theta);
		final double dy = scale1 * Math.sin(theta);
		final EllipseRoi ellipse = new EllipseRoi(x - dx, y - dy, x + dx, y + dy, scale2 / scale1);
		return ellipse;
	}

	public static void main(String[] args) {
		new ImageJ();

		ImagePlus imp = new Opener().openImage("/Users/varunkapoor/res/super_bent_small.tif");
		ImagePlus Preprocessedimp = new Opener().openImage("/Users/varunkapoor/res/super_bent_small.tif");
		imp.show();
		Preprocessedimp.show();
		final double[] psf = { 1.65, 1.47 };
		final long radius = (long) (Math.ceil(Math.sqrt(psf[0] * psf[0] + psf[1] * psf[1])));

		// minimum length of the lines to be detected, the smallest possible
		// number is 2.
		final int minlength = (int) (radius);


		new InteractiveMT(imp, Preprocessedimp, psf, minlength).run(null);

	}
}
