package util;

import java.util.ArrayList;

import com.sun.tools.javac.util.Pair;

import ij.gui.EllipseRoi;
import labeledObjects.CommonOutput;
import labeledObjects.LabelledImg;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.AllConnectedComponents;
import net.imglib2.algorithm.labeling.Watershed;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.labeling.DefaultROIStrategyFactory;
import net.imglib2.labeling.Labeling;
import net.imglib2.labeling.LabelingROIStrategy;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

@SuppressWarnings("deprecation")
public class Boundingboxes {

	
	
	public static long[] GetMaxcorners(RandomAccessibleInterval<IntType> inputimg, int label) {

		Cursor<IntType> intCursor = Views.iterable(inputimg).localizingCursor();
		int n = inputimg.numDimensions();
		long[] maxVal = { inputimg.min(0), inputimg.min(1) };

		while (intCursor.hasNext()) {
			intCursor.fwd();
			int i = intCursor.get().get();
			if (i == label) {

				for (int d = 0; d < n; ++d) {

					final long p = intCursor.getLongPosition(d);
					if (p > maxVal[d])
						maxVal[d] = p;

				}

			}
		}

		return maxVal;

	}

	public static long[] GetMincorners(RandomAccessibleInterval<IntType> inputimg, int label) {

		Cursor<IntType> intCursor = Views.iterable(inputimg).localizingCursor();
		int n = inputimg.numDimensions();
		long[] minVal = { inputimg.max(0), inputimg.max(1) };
		while (intCursor.hasNext()) {
			intCursor.fwd();
			int i = intCursor.get().get();
			if (i == label) {

				for (int d = 0; d < n; ++d) {

					final long p = intCursor.getLongPosition(d);
					if (p < minVal[d])
						minVal[d] = p;
				}

			}
		}

		return minVal;

	}

	public static double GetBoundingbox(RandomAccessibleInterval<IntType> inputimg, int label) {

		Cursor<IntType> intCursor = Views.iterable(inputimg).localizingCursor();
		int n = inputimg.numDimensions();
		long[] position = new long[n];
		long[] minVal = { inputimg.max(0), inputimg.max(1) };
		long[] maxVal = { inputimg.min(0), inputimg.min(1) };

		while (intCursor.hasNext()) {
			intCursor.fwd();
			int i = intCursor.get().get();
			if (i == label) {

				intCursor.localize(position);
				for (int d = 0; d < n; ++d) {
					if (position[d] < minVal[d]) {
						minVal[d] = position[d];
					}
					if (position[d] > maxVal[d]) {
						maxVal[d] = position[d];
					}

				}

			}
		}

		double boxsize = Distance(minVal, maxVal);

		Pair<long[], long[]> boundingBox = new Pair<long[], long[]>(minVal, maxVal);
		return boxsize;
	}

	public static int GetMaxlabelsseeded(RandomAccessibleInterval<IntType> intimg) {

		// To get maximum Labels on the image
		Cursor<IntType> intCursor = Views.iterable(intimg).cursor();
		int currentLabel = 1;
		boolean anythingFound = true;
		while (anythingFound) {
			anythingFound = false;
			intCursor.reset();
			while (intCursor.hasNext()) {
				intCursor.fwd();
				int i = intCursor.get().get();
				if (i == currentLabel) {

					anythingFound = true;

				}
			}
			currentLabel++;
		}

		return currentLabel;

	}

	public static NativeImgLabeling<Integer, IntType> GetlabeledImage(RandomAccessibleInterval<FloatType> inputimg,
			NativeImgLabeling<Integer, IntType> seedLabeling) {

		int n = inputimg.numDimensions();
		long[] dimensions = new long[n];

		for (int d = 0; d < n; ++d)
			dimensions[d] = inputimg.dimension(d);
		final NativeImgLabeling<Integer, IntType> outputLabeling = new NativeImgLabeling<Integer, IntType>(
				new ArrayImgFactory<IntType>().create(inputimg, new IntType()));

		final Watershed<FloatType, Integer> watershed = new Watershed<FloatType, Integer>();

		watershed.setSeeds(seedLabeling);
		watershed.setIntensityImage(inputimg);
		watershed.setStructuringElement(AllConnectedComponents.getStructuringElement(2));
		watershed.setOutputLabeling(outputLabeling);
		watershed.process();
		DefaultROIStrategyFactory<Integer> deffactory = new DefaultROIStrategyFactory<Integer>();
		LabelingROIStrategy<Integer, Labeling<Integer>> factory = deffactory
				.createLabelingROIStrategy(watershed.getResult());
		outputLabeling.setLabelingCursorStrategy(factory);

		return outputLabeling;

	}

	public static Pair<RandomAccessibleInterval<FloatType>, FinalInterval>  CurrentLabelImage(RandomAccessibleInterval<FloatType> img, EllipseRoi roi){
		
		int n = img.numDimensions();
		long[] position = new long[n];
		long[] minVal = { img.max(0), img.max(1) };
		long[] maxVal = { img.min(0), img.min(1) };
		
		Cursor<FloatType> localcursor = Views.iterable(img).localizingCursor();
		
		while (localcursor.hasNext()) {
			localcursor.fwd();
			int x = localcursor.getIntPosition(0);
			int y = localcursor.getIntPosition(1);
			if (roi.contains(x, y)){

				localcursor.localize(position);
				for (int d = 0; d < n; ++d) {
					if (position[d] < minVal[d]) {
						minVal[d] = position[d];
					}
					if (position[d] > maxVal[d]) {
						maxVal[d] = position[d];
					}

				}
				
			}
		}
		FinalInterval interval = new FinalInterval(minVal, maxVal);
		RandomAccessibleInterval<FloatType> currentimgsmall = Views.interval(img, interval);
		
		Pair<RandomAccessibleInterval<FloatType>, FinalInterval> pair = new Pair<RandomAccessibleInterval<FloatType>, FinalInterval>(currentimgsmall, interval);
		
		return pair;
	}
	
	public static RandomAccessibleInterval<FloatType> CurrentLabelImage(ArrayList<LabelledImg> imgs,int label) {
		
		RandomAccessibleInterval<FloatType> currentimg = imgs.get(label).Actualroiimg;
		int n = currentimg.numDimensions();
		long[] position = new long[n];
		long[] minVal = { currentimg.max(0), currentimg.max(1) };
		long[] maxVal = { currentimg.min(0), currentimg.min(1) };
		EllipseRoi roi = imgs.get(label).roi;
		
		Cursor<FloatType> localcursor = Views.iterable(currentimg).localizingCursor();

		while (localcursor.hasNext()) {
			localcursor.fwd();
			int x = localcursor.getIntPosition(0);
			int y = localcursor.getIntPosition(1);
			if (roi.contains(x, y)){

				localcursor.localize(position);
				for (int d = 0; d < n; ++d) {
					if (position[d] < minVal[d]) {
						minVal[d] = position[d];
					}
					if (position[d] > maxVal[d]) {
						maxVal[d] = position[d];
					}

				}
				
			}
		}
		
		FinalInterval interval = new FinalInterval(minVal, maxVal) ;
		RandomAccessibleInterval<FloatType> currentimgsmall = Views.interval(currentimg, interval);
		return currentimgsmall;
		
	}
	
	
	
public static RandomAccessibleInterval<FloatType> CurrentLabeloffsetImage(ArrayList<CommonOutput> imgs, final EllipseRoi roi,int label) {
		
		RandomAccessibleInterval<FloatType> currentimg = imgs.get(label).Actualroi;
		int n = currentimg.numDimensions();
		long[] position = new long[n];
		long[] minVal = { currentimg.max(0), currentimg.max(1) };
		long[] maxVal = { currentimg.min(0), currentimg.min(1) };
		
		Cursor<FloatType> localcursor = Views.iterable(currentimg).localizingCursor();

		while (localcursor.hasNext()) {
			localcursor.fwd();
			int x = localcursor.getIntPosition(0);
			int y = localcursor.getIntPosition(1);
			if (roi.contains(x, y)){

				localcursor.localize(position);
				for (int d = 0; d < n; ++d) {
					if (position[d] < minVal[d]) {
						minVal[d] = position[d];
					}
					if (position[d] > maxVal[d]) {
						maxVal[d] = position[d];
					}

				}
				
			}
		}
		
		FinalInterval interval = new FinalInterval(minVal, maxVal) ;
		RandomAccessibleInterval<FloatType> currentimgsmall = Views.offsetInterval(currentimg, interval);
		return currentimgsmall;
		
	}


public static FinalInterval  CurrentroiInterval(RandomAccessibleInterval<FloatType> currentimg, final  EllipseRoi roi){
	
	int n = currentimg.numDimensions();
	long[] position = new long[n];
	long[] minVal = { currentimg.max(0), currentimg.max(1) };
	long[] maxVal = { currentimg.min(0), currentimg.min(1) };
	
	Cursor<FloatType> localcursor = Views.iterable(currentimg).localizingCursor();

	while (localcursor.hasNext()) {
		localcursor.fwd();
		int x = localcursor.getIntPosition(0);
		int y = localcursor.getIntPosition(1);
		if (roi.contains(x, y)){

			localcursor.localize(position);
			for (int d = 0; d < n; ++d) {
				if (position[d] < minVal[d]) {
					minVal[d] = position[d];
				}
				if (position[d] > maxVal[d]) {
					maxVal[d] = position[d];
				}

			}
			
		}
	}
	
	FinalInterval interval = new FinalInterval(minVal, maxVal) ;
	
	return interval;
	
}
	public static Pair<RandomAccessibleInterval<FloatType>, FinalInterval> CurrentLabeloffsetImagepair(RandomAccessibleInterval<IntType> Intimg,
			RandomAccessibleInterval<FloatType> originalimg, int currentLabel) {

		RandomAccess<FloatType> inputRA = originalimg.randomAccess();

		Cursor<IntType> intCursor = Views.iterable(Intimg).cursor();
		final FloatType type = originalimg.randomAccess().get().createVariable();
		final ImgFactory<FloatType> factory = Util.getArrayOrCellImgFactory(originalimg, type);
		RandomAccessibleInterval<FloatType> outimg = factory.create(originalimg, type);
		RandomAccess<FloatType> imageRA = outimg.randomAccess();

		// Go through the whole image and add every pixel, that belongs to
		// the currently processed label

		while (intCursor.hasNext()) {
			intCursor.fwd();
			inputRA.setPosition(intCursor);
			imageRA.setPosition(inputRA);
			int i = intCursor.get().get();
			if (i == currentLabel) {

				imageRA.get().set(inputRA.get());

			}

		}
		long[] minCorner = Boundingboxes.GetMincorners(Intimg, currentLabel);
		long[] maxCorner = Boundingboxes.GetMaxcorners(Intimg, currentLabel);

		FinalInterval intervalsmall = new FinalInterval(minCorner, maxCorner);
		RandomAccessibleInterval<FloatType> outimgsmall = Views.offsetInterval(outimg, intervalsmall);

		Pair<RandomAccessibleInterval<FloatType>, FinalInterval> pair = new Pair<RandomAccessibleInterval<FloatType>, FinalInterval>(outimgsmall, intervalsmall);
		return pair;

	}

	
	public static Pair<RandomAccessibleInterval<FloatType>, FinalInterval> CurrentLabelImagepair(RandomAccessibleInterval<IntType> Intimg,
			RandomAccessibleInterval<FloatType> originalimg, int currentLabel) {

		RandomAccess<FloatType> inputRA = originalimg.randomAccess();

		Cursor<IntType> intCursor = Views.iterable(Intimg).cursor();
		final FloatType type = originalimg.randomAccess().get().createVariable();
		final ImgFactory<FloatType> factory = Util.getArrayOrCellImgFactory(originalimg, type);
		RandomAccessibleInterval<FloatType> outimg = factory.create(originalimg, type);
		RandomAccess<FloatType> imageRA = outimg.randomAccess();

		// Go through the whole image and add every pixel, that belongs to
		// the currently processed label

		while (intCursor.hasNext()) {
			intCursor.fwd();
			inputRA.setPosition(intCursor);
			imageRA.setPosition(inputRA);
			int i = intCursor.get().get();
			if (i == currentLabel) {

				imageRA.get().set(inputRA.get());

			}

		}
		long[] minCorner = Boundingboxes.GetMincorners(Intimg, currentLabel);
		long[] maxCorner = Boundingboxes.GetMaxcorners(Intimg, currentLabel);

		FinalInterval intervalsmall = new FinalInterval(minCorner, maxCorner);

		Pair<RandomAccessibleInterval<FloatType>, FinalInterval> pair = new Pair<RandomAccessibleInterval<FloatType>, FinalInterval>(outimg, intervalsmall);
		return pair;

	}
	
	public static RandomAccessibleInterval<FloatType> CurrentLabelImage(RandomAccessibleInterval<IntType> Intimg,
			RandomAccessibleInterval<FloatType> originalimg, int currentLabel) {

		RandomAccess<FloatType> inputRA = originalimg.randomAccess();

		Cursor<IntType> intCursor = Views.iterable(Intimg).cursor();
		final FloatType type = originalimg.randomAccess().get().createVariable();
		final ImgFactory<FloatType> factory = Util.getArrayOrCellImgFactory(originalimg, type);
		RandomAccessibleInterval<FloatType> outimg = factory.create(originalimg, type);
		RandomAccess<FloatType> imageRA = outimg.randomAccess();

		// Go through the whole image and add every pixel, that belongs to
		// the currently processed label

		while (intCursor.hasNext()) {
			intCursor.fwd();
			inputRA.setPosition(intCursor);
			imageRA.setPosition(inputRA);
			int i = intCursor.get().get();
			if (i == currentLabel) {

				imageRA.get().set(inputRA.get());

			}

		}
		long[] minCorner = Boundingboxes.GetMincorners(Intimg, currentLabel);
		long[] maxCorner = Boundingboxes.GetMaxcorners(Intimg, currentLabel);

		FinalInterval intervalsmall = new FinalInterval(minCorner, maxCorner);
		RandomAccessibleInterval<FloatType> outimgsmall = Views.interval(outimg, intervalsmall);

		return outimgsmall;

	}
	
	public static RandomAccessibleInterval<FloatType> CurrentLabeloffsetImage(RandomAccessibleInterval<IntType> Intimg,
			RandomAccessibleInterval<FloatType> originalimg, int currentLabel) {

		RandomAccess<FloatType> inputRA = originalimg.randomAccess();

		Cursor<IntType> intCursor = Views.iterable(Intimg).cursor();
		final FloatType type = originalimg.randomAccess().get().createVariable();
		final ImgFactory<FloatType> factory = Util.getArrayOrCellImgFactory(originalimg, type);
		RandomAccessibleInterval<FloatType> outimg = factory.create(originalimg, type);
		RandomAccess<FloatType> imageRA = outimg.randomAccess();

		// Go through the whole image and add every pixel, that belongs to
		// the currently processed label

		while (intCursor.hasNext()) {
			intCursor.fwd();
			inputRA.setPosition(intCursor);
			imageRA.setPosition(inputRA);
			int i = intCursor.get().get();
			if (i == currentLabel) {

				imageRA.get().set(inputRA.get());

			}

		}
		long[] minCorner = Boundingboxes.GetMincorners(Intimg, currentLabel);
		long[] maxCorner = Boundingboxes.GetMaxcorners(Intimg, currentLabel);

		FinalInterval intervalsmall = new FinalInterval(minCorner, maxCorner);
		RandomAccessibleInterval<FloatType> outimgsmall = Views.offsetInterval(outimg, intervalsmall);

		return outimgsmall;

	}
	
	
	public static double Distance(final long[] minCorner, final long[] maxCorner) {

		double distance = 0;

		for (int d = 0; d < minCorner.length; ++d) {

			distance += Math.pow((minCorner[d] - maxCorner[d]), 2);

		}
		return Math.sqrt(distance);
	}
}