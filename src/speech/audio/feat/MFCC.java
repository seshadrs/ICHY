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
		extractor = new comirva.audio.util.MFCC(16000);
	}
	
	public double[][] extractFeatures(double[] data) throws IllegalArgumentException, IOException
	{
		return extractor.process(data);
	}

}
