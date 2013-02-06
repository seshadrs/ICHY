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

}
