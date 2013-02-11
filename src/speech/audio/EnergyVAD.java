package speech.audio;

import java.util.ArrayList;

import speech.audio.utils.SignalData;
import speech.audio.utils.SignalMath;



/*
 * Dynamic Energy based VAD
 * 
 * TODO: model the background energy level before the audio is recorded, to replace the default value.
 * */
public class EnergyVAD {
	
	/* 
	 * Default parameters from Sphinx-4 (http://cmusphinx.sourceforge.net/sphinx4/) class SpeechClassifier
	 * 
	 * */
	public static int frameLengthMs = 10;	//10 ms per frame
	private static double minSignal = 0;
	private static double level = 0;
	private static double averageNumber =1;
	private static double adjustment = 0.0025;
	private static double threshold = 3.5;
	
	private static double background = 0;
	private static int backgroundFrames = 0;
	
	/*
	 * Default parameter to detect endpoint in speech (a heuristic)  
	 * */
	private static int endpointThreshold = 10;
	
	
	public enum SpeechState 
	{
		//TODO: need to characterize background noise
		SPEECH, SILENCE;   
	}
	
	private static ArrayList<SpeechState> signalHistory =  new ArrayList<SpeechState>();
	
	
	private static void initSignalHistory()
	{
		background -= 3;		//to account for all the surface noise on the laptop 
		for(int i=0; i<endpointThreshold-1; i++)
			signalHistory.add(0,SpeechState.SPEECH);
	}
	
	/*
	 * If speech-endpoint occurs, returns boolean<true>
	 * Else, returns boolean<falase>
	 * */
	private static boolean speechEnd(SpeechState s)
	{
		if (signalHistory.size()==0)
			initSignalHistory();
		
		System.out.println(signalHistory.toString());
		
		boolean isSpeechEndpoint = true;
		
		if (s==SpeechState.SPEECH)
		{
			signalHistory.remove(0);
			signalHistory.add(s);
			isSpeechEndpoint = false;
		}
		else
		{
			boolean update = false;
			//check if all items in history are SpeechState.SILENCE
			for(SpeechState x : signalHistory)
			{
				if (x==SpeechState.SPEECH)
					{
						isSpeechEndpoint = false;
						update = true;
						break;
					}
			}
			
			if (update)
			{
				signalHistory.remove(0);
				signalHistory.add(s);
			}
		}
		
		return isSpeechEndpoint;
		
	}
	
	/**
     * Classifies the given audio frame as speech or not, and updates the endpointing parameters.
     *
     * @param audio the audio frame of length <= frame size
     */
    private static SpeechState classify(byte[] audio) 
    {
    	float[] data = SignalData.toFloatArr(audio);
        double current = SignalMath.logRootMeanSquare(data);

        boolean isSpeech = false;
        if (current >= minSignal) {
            level = ((level * averageNumber) + current) / (averageNumber + 1);
            if (current < background) {
                background = current;
            } else {
                background += (current - background) * adjustment;
            }
            if (level < background) {
                level = background;
            }
            isSpeech = ((level - background) > threshold);
        }

	System.out.println(isSpeech + " " + "Bkg: " + background + ", level: " + level +", current: " + current);
	
	if (isSpeech)
	{
		
		return SpeechState.SPEECH;
	}
	else
		return SpeechState.SILENCE;
                    
    }
    
    /*
	 * If speech-endpoint occurs, returns boolean<true>
	 * Else, returns boolean<false>
	 * Takes as input the audio frame
	 * */
    public static boolean speechEndpoint(byte[] audio)
    {
    	SpeechState curState = classify(audio);		//gets the speech state for the audio frame
    	 return speechEnd(curState);				//checks whether this state indicates the endpoint of speech input
    }
    
    
    /*
	 * Frames previous to key-hit are used to model the background energy level
	 * */
    public static void modelBackground(byte[] audio)
    {
    	float[] data = SignalData.toFloatArr(audio);
        double currentBGEnergy = SignalMath.logRootMeanSquare(data);
        background = (background*(double)backgroundFrames + currentBGEnergy)/(double)(backgroundFrames+1);
        backgroundFrames+=1;
        System.out.println(background);
        
        
    }
    
    
    

}

