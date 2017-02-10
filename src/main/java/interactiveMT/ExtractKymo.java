package interactiveMT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ij.IJ;
import ij.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class ExtractKymo {

	
	
	public static void ReadFromKymo(final RandomAccessibleInterval<FloatType> Kymo, File fichier) throws IOException{
		
		
		
			FileWriter fw = new FileWriter(fichier);
			BufferedWriter bw = new BufferedWriter(fw);

			bw.write(
					"\tFramenumber\tLength\n");

			
			Cursor<FloatType> cursor = Views.iterable(Kymo).localizingCursor();
			
			while(cursor.hasNext()){
				
				cursor.fwd();
				
				if (cursor.get().get() > 0){
					
					bw.write("\t" + cursor.getDoublePosition(1) + "\t" + cursor.getDoublePosition(0) + "\n");
					
					
				}
				
				
			}
			
			
			
		
		
		bw.close();
		fw.close();
		
		
	
		
	}
	
	
	
	public static void main(String[] args) throws IOException{
		new ImageJ();
		String usefolder = IJ.getDirectory("imagej");
		String addToName = "Kymo2Will";
		
		RandomAccessibleInterval<FloatType> img = util.ImgLib2Util
				.openAs32Bit(new File("/Users/varunkapoor/res/MaskKymo2.tif"), new ArrayImgFactory<FloatType>());
		Normalize.normalize(Views.iterable(img), new FloatType(0), new FloatType(1));
		
		
		File fichier = new File(usefolder + "//" + addToName + "ID" + 0 + ".txt");
		
		 ReadFromKymo(img, fichier);
		
		ImageJFunctions.show(img);
		
	}
	
	
}
