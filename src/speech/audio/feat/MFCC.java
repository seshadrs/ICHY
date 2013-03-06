package speech.audio.feat;

import java.io.IOException;

import sun.awt.SunToolkit.InfiniteLoop;

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
		 double[][] featureVectors = extractor.process(data);
		 double[][] allFeats = new double[featureVectors.length][39];
		 
		 double[] velocityVector= new double[13];
		 double[] prevVelocityVector= new double[13];
		 double[] accelerationVector= new double[13];
		 double[] prevFeatVector = new double[13];
		 
		  int pos=0;
		 double velI,accI;
		 
		 for(double[] featVector : featureVectors)
		 {
			 //enter values into new feat-arr
			 for(int i=0; i<13;i++)
				 allFeats[pos][i] = featVector[i];
			 
			 if(pos>0)	//calculate velocity
			 {
				 for(int i=0; i<13;i++)
					 {
					 	velI = featVector[i]-prevFeatVector[i];
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
			 prevFeatVector = featVector;
			 prevVelocityVector = velocityVector.clone();
				 
			 
		 }
		 
		 //System.out.println("before "+allFeats.length);
		 allFeats = filterPrefixInfinityFrames(allFeats);
		 //System.out.println("after "+allFeats.length);
		 
		return allFeats; 
	 }

	private double[][] filterPrefixInfinityFrames(double[][] allFeats) {
		
		//scan first to find out the number of prefix rows with -Infinity
		int prefixRowsToRemove=0;
		for(int i=0; i<allFeats.length; i++)
		{
			boolean removeRow = false;
			
			for(int j=0; j<allFeats[i].length; j++)
			{
				if (allFeats[i][j] == Double.NEGATIVE_INFINITY || allFeats[i][j] == Double.POSITIVE_INFINITY || allFeats[i][j] == Double.NaN)
					{
						removeRow = true;
					}
			}
			
			if (removeRow)
				prefixRowsToRemove +=1;
			else
				break;
		}
		
		//now remove hte prefix rows and return rest of 2D arr
		double[][] filteredFeatures = new double[allFeats.length-prefixRowsToRemove][39];
		for(int i=0; i< filteredFeatures.length; i++)
		{
			for(int j=0; j<39;j++)
				filteredFeatures[i][j] = allFeats[i+prefixRowsToRemove][j];
		}
		
		return filteredFeatures;
		
	}

}
