package speech.audio.feat;

import java.io.IOException;

import comirva.audio.feature.*;

/*
 * Class that converts audio samples to the MFCC feature vectors
 * 
 * */
public class MFCC {
	
	private static comirva.audio.util.MFCC extractor;
	
	public MFCC()
	{
	  //MFCC(float sampleRate, int windowSize, int numberCoefficients, boolean useFirstCoefficient, 
	  //double minFreq, double maxFreq, int numberFilters)
		extractor = new comirva.audio.util.MFCC(16000);
	}
	
	public MFCC(float sampleRate, double minFreq, double maxFreq, int numFilters)
  {
    //MFCC(float sampleRate, int windowSize, int numberCoefficients, boolean useFirstCoefficient, 
    //double minFreq, double maxFreq, int numberFilters)
    extractor = new comirva.audio.util.MFCC(sampleRate, 512, 13, true, minFreq, maxFreq, numFilters);
  }
	
	public double[][] extractFeaturesLogMel(double[] data) throws IllegalArgumentException, IOException
	{
		return extractor.processLogMel(data);
	}
	
	 public double[][] extractFeatures(double[] data) throws IllegalArgumentException, IOException
	  {
	    return extractor.process(data);
	  }
	 
	 
	 //Returns the 13 LogMel features, the velocity and the acceleration of the features.
	 public double[][] extractAll39Features(double[] data) throws IllegalArgumentException, IOException
	 {
		 double[][] logMelVectors = extractor.processLogMel(data);
		 double[][] allFeats = new double[logMelVectors.length][39];
		 
		 double[] velocityVector= new double[13];
		 double[] prevVelocityVector= new double[13];
		 double[] accelerationVector= new double[13];
		 double[] prevLogMelVector = new double[13];
		
		 int pos=0;
		 double velI,accI;
		 
		 for(double[] logMelVector : logMelVectors)
		 {
			 //enter values into new feat-arr
			 for(int i=0; i<13;i++)
				 allFeats[pos][i] = logMelVector[i];
			 
			 if(pos>0)	//calculate velocity
			 {
				 for(int i=0; i<13;i++)
					 {
					 	velI = logMelVector[i]-prevLogMelVector[i];
					 	allFeats[pos][13+i] = velI;
					 	velocityVector[i] = velI; 
					 }
			 }
			 
			 if(pos>1)	//calculate acceleration
			 {
				 for(int i=0; i<13;i++)
					 {
					 	accI = velocityVector[i]-prevVelocityVector[i];
					 	allFeats[pos][13*2+i] = accI;
					 	accelerationVector[i] = accI; 
					 }
			 }
			 
			 pos+=1;
			 prevLogMelVector = logMelVector;
			 prevVelocityVector = velocityVector;
				 
			 
		 }
		 
		return allFeats; 
	 }

}
