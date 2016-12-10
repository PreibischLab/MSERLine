package fakeMTgenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import com.sun.tools.javac.util.Pair;

import ij.ImageJ;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import poissonSimulator.Poissonprocess;
import preProcessing.Kernels;

public class MovingLines {

	
	
	public static void main (String[] args) throws IncompatibleTypeException, IOException{
        new ImageJ();
		
		final FinalInterval range = new FinalInterval(1212, 1212);
		final FinalInterval smallrange = new FinalInterval(912, 912);
		
		
		
		final int ndims = range.numDimensions();
		final double [] sigma = {1.65,1.47};
		final double [] Ci = new double[ndims];
		
		for (int d = 0; d < ndims; ++d)
			Ci[d] = 1.0 / Math.pow(sigma[d],2);
		
		final int numframes = 50;
		final int numlines = 4;

		
		
			
		RandomAccessibleInterval<FloatType> lineimage = new ArrayImgFactory<FloatType>().create(range, new FloatType());
		RandomAccessibleInterval<FloatType> noisylines = new ArrayImgFactory<FloatType>().create(range, new FloatType());
		
		
		ArrayList<double[]> startseeds = new ArrayList<double[]>();
		ArrayList<double[]> endseeds = new ArrayList<double[]>();
		Dummylines.GetSeeds(lineimage, startseeds, endseeds, smallrange, numlines, sigma);
		
		
		

		FloatType minval = new FloatType(0);
		FloatType maxval = new FloatType(1);
		Normalize.normalize(Views.iterable(lineimage), minval, maxval);
		Kernels.addBackground(Views.iterable(lineimage), 0.2);
		noisylines = Poissonprocess.poissonProcess(lineimage, 10);
		ImageJFunctions.show(noisylines);
		ArrayList<double[]> startseedscopy =  new ArrayList<double[]>();
		ArrayList<double[]> endseedscopy = new ArrayList<double[]>();
		for (int index = 0; index < startseeds.size(); ++index){
			
			startseedscopy.add(index, startseeds.get(index));
			
		}
       for (int index = 0; index < endseeds.size(); ++index){
			
			endseedscopy.add(index, endseeds.get(index));
			
		}
		ArrayList<Indexofline> linestlist = new ArrayList<Indexofline>();
		ArrayList<Indexofline> lineendlist = new ArrayList<Indexofline>();
		for (int frame = 1; frame < numframes; ++frame){
			
			RandomAccessibleInterval<FloatType> noisylinesframe = new ArrayImgFactory<FloatType>().create(range, new FloatType());
			RandomAccessibleInterval<FloatType> lineimageframe = new ArrayImgFactory<FloatType>().create(range, new FloatType());
			
			Pair<Pair<ArrayList<Dummyprops>, ArrayList<Dummyprops>>, Pair<ArrayList<double[]>, ArrayList<double[]>>>  pair	 = 
					Dummylines.Growseeds(lineimageframe, startseeds, endseeds, frame, sigma);
		
			
			
			
			
			for (int index = 0; index < pair.fst.fst.size(); ++index){
				double[] prevst = new double[ndims];
				double[] nextst = new double[ndims];
				 
				for (int d = 0; d < ndims; ++d){
					
					prevst[d] = pair.fst.fst.get(index).originalpoint[d];
					nextst[d] = pair.fst.fst.get(index).newpoint[d];
					
				}
				Indexofline linest = new Indexofline(index, frame, nextst);
				linestlist.add(linest);	

		
			}
			 
			for (int index = 0; index < pair.fst.fst.size(); ++index){
				
				double[] preven = new double[ndims];
				double[] nexten = new double[ndims];
				
				  
				for (int d = 0; d < ndims; ++d){
					
					preven[d] = pair.fst.snd.get(index).originalpoint[d];
					nexten[d] = pair.fst.snd.get(index).newpoint[d];
				}
				
				Indexofline lineend = new Indexofline(index, frame, nexten);
				lineendlist.add(lineend);	
             	
				
				

		
			}
	    

	   
		
		Normalize.normalize(Views.iterable(lineimageframe), minval, maxval);
		Kernels.addBackground(Views.iterable(lineimageframe), 0.2);
		noisylinesframe = Poissonprocess.poissonProcess(lineimageframe, 10);
		
	
		ImageJFunctions.show(noisylinesframe);
		
		
		
		
		
		
		
		
	
		}
		 FileWriter writerend = new FileWriter("../res/HHNActuallength-movingend.txt", true);
		for (int i = 0; i < lineendlist.size() ; ++i){
			for (int j = 0; j < lineendlist.size() ; ++j){
			
				
			
			
			if (lineendlist.get(i).index == lineendlist.get(j).index && lineendlist.get(i).frame - lineendlist.get(j).frame == 1){
				double length = Distance(lineendlist.get(i).position, lineendlist.get(j).position);
				writerend.write( lineendlist.get(i).frame + " " + lineendlist.get(j).position[0] + " " + lineendlist.get(j).position[1]
						+ " " + lineendlist.get(i).position[0] + " " + lineendlist.get(i).position[1] + " " +  length );
				writerend.write("\r\n");
				
			}
		}
		}
		
		 FileWriter writerstart = new FileWriter("../res/HHNActuallength-movingstartHN.txt", true);
			for (int i = 0; i < linestlist.size() ; ++i){
				for (int j = 0; j < linestlist.size() ; ++j){
				
				
				
				
				if (linestlist.get(i).index == linestlist.get(j).index && linestlist.get(i).frame - linestlist.get(j).frame == 1){
					double length = Distance(linestlist.get(i).position, linestlist.get(j).position);
					writerstart.write( linestlist.get(i).frame + " " + linestlist.get(j).position[0] + " " + linestlist.get(j).position[1]
							+ " " + linestlist.get(i).position[0] + " " + linestlist.get(i).position[1] + " " + length );
					writerstart.write("\r\n");
					
				}
			}
			}
		
		System.out.println("done");
		
		writerend.close();
		writerstart.close();
		
	}
	public static double Distance(final double[] cordone, final double[] cordtwo) {

		double distance = 0;

		for (int d = 0; d < cordone.length; ++d) {

			distance += Math.pow((cordone[d] - cordtwo[d]), 2);

		}
		return Math.sqrt(distance);
	}
}

