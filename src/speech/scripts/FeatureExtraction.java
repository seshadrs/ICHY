package speech.scripts;

import java.io.IOException;

import speech.audio.feat.MFCC;
import speech.audio.utils.IO;

public class FeatureExtraction {

	
	 public static void main(String[] args) throws IllegalArgumentException, IOException
	 {
		 double[] audioData = IO.read("./rec.wav");
		 
		 MFCC featExtractor = new MFCC();
		 
		 double[][] featVectors = featExtractor.extractFeatures(audioData);
		 
		 System.out.println(audioData.length);
		 System.out.println(featVectors.length);
		 System.out.println(featVectors[0].length);
		 
		 System.out.println(featVectors[3][5]);		 
		
		 
		 for (int i=0; i< featVectors.length; i++)
			 {for (int j=0; j< 13; j++)
			 {
				 try{
					 double x = featVectors[i][j];
				 System.out.print(x+" ");}
				 catch(Exception e)
				 {}
			 }
			 System.out.println();
			 }
	 }
	
	
}
