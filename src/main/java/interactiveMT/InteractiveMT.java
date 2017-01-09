package interactiveMT;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
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
import fiji.tool.SliceObserver;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import labeledObjects.CommonOutputHF;
import labeledObjects.Indexedlength;
import lineFinder.FindlinesVia;
import lineFinder.FindlinesVia.LinefindingMethod;
import lineFinder.LinefinderHough;
import lineFinder.LinefinderMSER;
import lineFinder.LinefinderMSERwHough;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianReal1;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import mpicbg.imglib.util.Util;
import mpicbg.spim.registration.detection.DetectionSegmentation;
import mpicbg.spim.segmentation.InteractiveActiveContour.ValueChange;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import spim.process.fusion.FusionHelper;

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
	Color colorDraw = null;

	SliceObserver sliceObserver;
	RoiListener roiListener;
	boolean isComputing = false;
	boolean isStarted = false;
	boolean FindLinesViaMSER = false;
	boolean FindLinesViaHOUGH = false;
	boolean FindLinesViaMSERwHOUGH = false;
	int channel = 0;
	Img<FloatType> img;
	Img<FloatType> Preprocessedimg;
	FloatImagePlus<net.imglib2.type.numeric.real.FloatType> source;
	FloatImagePlus<net.imglib2.type.numeric.real.FloatType> Preprocessedsource;
	ImagePlus imp;
	ImagePlus preprocessedimp;
	final double[] psf;
	final int minlength;
	int stacksize;
	private final int ndims;
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

	// for the case that it is needed again, we can save one conversion
	public FloatImagePlus<net.imglib2.type.numeric.real.FloatType> getConvertedImage() {
		return source;
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

		currentslice = imp.getFrame();
		imp.setPosition(imp.getChannel(), imp.getSlice(), imp.getFrame());

		int z = currentslice;

		// copy the ImagePlus into an ArrayImage<FloatType> for faster access
		source = convertToFloat(imp, channel, z - 1);
		displaySliders();

		updatePreview(ValueChange.ALL);
		isStarted = true;

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
			img = ImageJFunctions.wrapFloat(imp);
			Preprocessedimg = ImageJFunctions.wrapFloat(preprocessedimp);

			roiChanged = true;
		}

		// if we got some mouse click but the ROI did not change we can return
		if (!roiChanged && change == ValueChange.ROI) {
			isComputing = false;
			return;
		}
		
	
		
		if (ndims == 2) {
			Pair<ArrayList<Indexedlength>, ArrayList<Indexedlength>> PrevFrameparam = null;
			if (roiChanged || FindLinesViaMSER) {

				LinefinderMSER newlineMser = new LinefinderMSER(img, Preprocessedimg, minlength, 0);
				newlineMser.setMaxlines(5);

				Overlay overlay = newlineMser.getOverlay();
				ImagePlus impcurr = IJ.getImage();
				impcurr.setOverlay(overlay);
				PrevFrameparam = FindlinesVia.LinefindingMethod(img, Preprocessedimg, minlength, 0, psf, newlineMser,
						UserChoiceModel.Line);

			}
			if (roiChanged || FindLinesViaHOUGH) {

				LinefinderHough newlineHough = new LinefinderHough(img, Preprocessedimg, minlength, 0);

				PrevFrameparam = FindlinesVia.LinefindingMethod(img, Preprocessedimg, minlength, 0, psf, newlineHough,
						UserChoiceModel.Line);

			}

			if (roiChanged || FindLinesViaMSERwHOUGH) {

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

		} //

	
	
		imp.updateAndDraw();

		isComputing = false;
	}

	public void displaySliders() {

		final Frame frame = new Frame("Find MT's and Track");
		frame.setSize(550, 550);
		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();
		final Label MTText = new Label("Find Lines Via ", Label.CENTER);
		CheckboxGroup Finders = new CheckboxGroup();
		
		final Checkbox mser = new Checkbox("MSER",Finders, FindLinesViaMSER );
		final Checkbox hough = new Checkbox("HOUGH", Finders, FindLinesViaHOUGH );
		final Checkbox mserwhough = new Checkbox("MSERwHOUGH",Finders, FindLinesViaMSERwHOUGH );
      
		
		/* Location */
		frame.setLayout(layout);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;

		frame.add(MTText, c);
		
		++c.gridy;
		c.insets = new Insets(0, 170, 0, 75);
		frame.add(mser, c);
	
		++c.gridy;
		c.insets = new Insets(0, 170, 0, 75);
		frame.add(hough, c);
		
		++c.gridy;
		c.insets = new Insets(0, 170, 0, 75);
		frame.add(mserwhough, c);
       
		
		
		
		
		
		
		mser.addItemListener(new MserListener());
		hough.addItemListener(new HoughListener());
		mserwhough.addItemListener(new MserwHoughListener());
		
		
		frame.addWindowListener(new FrameListener(frame));

		frame.setVisible(true);

		MTText.setFont(MTText.getFont().deriveFont(Font.BOLD));
	}
	

	protected class MserListener implements ItemListener {
		@Override
		public void itemStateChanged(final ItemEvent arg0) {
			boolean oldState = FindLinesViaMSER;

			if (arg0.getStateChange() == ItemEvent.DESELECTED)
				FindLinesViaMSER= false;
			else if (arg0.getStateChange() == ItemEvent.SELECTED){
			
				FindLinesViaMSER = true;
				
				
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
			}

			if (FindLinesViaMSERwHOUGH != oldState) {
				while (isComputing)
					SimpleMultiThreading.threadWait(10);

				updatePreview(ValueChange.FindLinesVia);
			}
		}
	}
	
	
	private boolean DialogueGeneral(){
		
		
		String[] Linefinder = { "MSER (recommended)", "HOUGH", "MSERwHOUGH" };
		int findertype = 0;
		// Create dialog
				GenericDialog gd = new GenericDialog("Find MT and track ends");
				gd.addChoice("Choose your tracker :", Linefinder, Linefinder[findertype]);
				gd.showDialog();
				
				if (findertype == 0){
					FindLinesViaMSER = true;
					FindLinesViaHOUGH = false;
					FindLinesViaMSERwHOUGH = false;
				}
				if (findertype == 1){
					FindLinesViaHOUGH = true;
					FindLinesViaMSER = false;
					FindLinesViaMSERwHOUGH = false;
				}
				if (findertype == 2){
					FindLinesViaMSERwHOUGH = true;
					FindLinesViaMSER = false;
					FindLinesViaHOUGH = false;
				}
				
				return !gd.wasCanceled();
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

				gd.showDialog();
				
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

	/**
	 * Normalize and make a copy of the {@link ImagePlus} into an {@link Image}
	 * &gt;FloatType&lt; for faster access when copying the slices
	 * 
	 * @param imp
	 *            - the {@link ImagePlus} input image
	 * @return - the normalized copy [0...1]
	 */
	public static FloatImagePlus<net.imglib2.type.numeric.real.FloatType> convertToFloat(final ImagePlus imp,
			int channel, int timepoint) {
		return convertToFloat(imp, channel, timepoint, Double.NaN, Double.NaN);
	}

	public static FloatImagePlus<net.imglib2.type.numeric.real.FloatType> convertToFloat(final ImagePlus imp,
			int channel, int timepoint, final double min, final double max) {
		// stupid 1-offset of imagej
		channel++;
		timepoint++;

		final int h = imp.getHeight();
		final int w = imp.getWidth();

		final ArrayList<float[]> img = new ArrayList<float[]>();

		if (imp.getProcessor() instanceof FloatProcessor) {
			for (int z = 0; z < imp.getNSlices(); ++z)
				img.add(((float[]) imp.getStack().getProcessor(imp.getStackIndex(channel, z + 1, timepoint))
						.getPixels()).clone());
		} else if (imp.getProcessor() instanceof ByteProcessor) {
			for (int z = 0; z < imp.getNSlices(); ++z) {
				final byte[] pixels = (byte[]) imp.getStack().getProcessor(imp.getStackIndex(channel, z + 1, timepoint))
						.getPixels();
				final float[] pixelsF = new float[pixels.length];

				for (int i = 0; i < pixels.length; ++i)
					pixelsF[i] = pixels[i] & 0xff;

				img.add(pixelsF);
			}
		} else if (imp.getProcessor() instanceof ShortProcessor) {
			for (int z = 0; z < imp.getNSlices(); ++z) {
				final short[] pixels = (short[]) imp.getStack()
						.getProcessor(imp.getStackIndex(channel, z + 1, timepoint)).getPixels();
				final float[] pixelsF = new float[pixels.length];

				for (int i = 0; i < pixels.length; ++i)
					pixelsF[i] = pixels[i] & 0xffff;

				img.add(pixelsF);
			}
		} else // some color stuff or so
		{
			for (int z = 0; z < imp.getNSlices(); ++z) {
				final ImageProcessor ip = imp.getStack().getProcessor(imp.getStackIndex(channel, z + 1, timepoint));
				final float[] pixelsF = new float[w * h];

				int i = 0;

				for (int y = 0; y < h; ++y)
					for (int x = 0; x < w; ++x)
						pixelsF[i++] = ip.getPixelValue(x, y);

				img.add(pixelsF);
			}
		}

		final FloatImagePlus<net.imglib2.type.numeric.real.FloatType> i = createImgLib2(img, w, h);

		if (Double.isNaN(min) || Double.isNaN(max) || Double.isInfinite(min) || Double.isInfinite(max) || min == max)
			FusionHelper.normalizeImage(i);
		else
			FusionHelper.normalizeImage(i, (float) min, (float) max);

		return i;
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
