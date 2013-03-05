package speech.scripts;
  
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import speech.audio.feat.MFCC;
import speech.audio.utils.IO;

public class FeatureExtraction {
 
	
	 public static void main(String[] args) throws IllegalArgumentException, IOException
	 {
		 double[] audioData = IO.read(args[0]); // wav file
		 
		 
	   
	   int[] nFiltersArr = {40,30,25};
	   
	   for(int nFilters : nFiltersArr)
	   {
		   
		   FileWriter outFilelog = new FileWriter(args[1]+"_"+ Integer.toString(nFilters)+".mat"); // log spectrum file
			 PrintWriter outflog = new PrintWriter(outFilelog);
		   FileWriter outFile = new FileWriter(args[2]+"_"+ Integer.toString(nFilters)+".dat"); // mfcc file
		   PrintWriter outf = new PrintWriter(outFile);
		 
		 //MFCC(float sampleRate, double minFreq, double maxFreq, int numFilters)
		 MFCC featExtractor = new MFCC(16000, 50, 7000, nFilters);
		 
		 double[][] featVectorsLogMel = featExtractor.extractFeaturesLogMel(audioData);
		 
		 double[][] featVectors = featExtractor.extractFeatures(audioData);
		 
		 //System.out.println(audioData.length);

		 System.out.println("Log Mel...");
		 System.out.println(featVectorsLogMel.length);
     System.out.println(featVectorsLogMel[0].length);
     System.out.println(featVectorsLogMel[3][5]);    
     
     System.out.println("Mel Cepstra...");
		 System.out.println(featVectors.length);
		 System.out.println(featVectors[0].length);
		 System.out.println(featVectors[3][5]);		 
		
		 
		 for (int i=0; i < featVectorsLogMel.length; i++){
		   for (int j=0; j < featVectorsLogMel[0].length; j++){
				try{
					 double x = featVectorsLogMel[i][j];
					 outflog.print(x+"\t");
				}catch(Exception e)
				 {
					   
				 }
			 }
			 outflog.println();
		 }
		 outflog.close();
	 
   for (int j=0; j < featVectors[0].length;j++){
     for (int i=0; i < featVectors.length; i++){
      try{
         double x = featVectors[i][j];
         outf.print(x+"\t");
      }catch(Exception e)
       {
           
       }
     }
     outf.println();
   }
   int y = 0;
   String s = "";
   for (int k=0; k < featVectors.length; k++){
     s = s + y + "\t";
   }
   for (int buf=0; buf < 48; buf++){    
     outf.println(s);
   }
   outf.close();
   
	   }
 }
	
	
}
