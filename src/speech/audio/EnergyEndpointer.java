package speech.audio;

import java.util.ArrayList;
import java.util.Arrays;

import speech.audio.utils.SignalData;


public class EnergyEndpointer {
  
  /* 
   * frame classifier parameters
   */
  private double level = Double.NEGATIVE_INFINITY;
  private double forgetfactor = 1.5;
  private double adjustment = 0.05;
  private double threshold = 0.1;
  private double background = 100;
  private int backgroundCounter;
  private int lookback = 10;
  
  /* 
   * Frame tracker for endpointing 
   */
  private ArrayList<Boolean> tracker; 
  private static boolean SILENCE = false;
  private static boolean VOICE = true;
  
  /*
   * Constructor 
   */
  public EnergyEndpointer(){
    tracker = new ArrayList<Boolean>();
    backgroundCounter = 0;
    background = 0;
  }
  
  /*
   * Main function for endpoint detection.
   * Returns true of last "lookback" number of 
   * frames were silence.
   */
  public boolean isEndpoint(){
    if (tracker.contains(VOICE)){
      return false;
    }
    
    System.out.println("WOHOO");
    
    return true;
  }
  
  /*
   * Record the background noise level
   */
  public void getBackground(byte buffer[]){
    int frame [] = SignalData.toIntArr(buffer);
    //double currEnergy;
    background += getFrameEnergy(frame);
    backgroundCounter += 1;
  }
  
  
  /*
   * Adds a frame to endpointer's lookback memory
   * Also keeps track of how many frames have been added 
   */
  public void addFrame(byte buffer[]){
    int frame [] = SignalData.toIntArr(buffer);
    double currEnergy;
    
    // if first frame, set level to energy of first frame 
    // and set background to average of background frames
    if (level == Double.NEGATIVE_INFINITY){
      level = getFrameEnergy(frame);
      background = background / backgroundCounter;
    }
    
    // after certain number of frames, remove old frame
    if (tracker.size() == lookback){
      tracker.remove(0);
    }
    
    currEnergy = getFrameEnergy(frame);
    tracker.add(classifyFrame(currEnergy));
  }
  
  
  
  /*
   * Calculate the frame energy in decibels
   */
  private double getFrameEnergy(int frame[]){
    double energy = 0;
    for (int cur : frame){
      energy = energy + Math.pow(cur, 2);
    }
    energy = 10 * Math.log10(energy);
    return energy;
  }
  
  
  /*
   * Frame classification algorithm implemented as per lecture2 slides.
   */
  private boolean classifyFrame(double frameEnergy){
    double current = frameEnergy;
    boolean isSpeech = false;
    level = ((level * forgetfactor) + current) / (forgetfactor + 1);
    if (current < background){
    background = current;
    }
    else{
    background += (current - background) * adjustment;
    }
    if (level < background) level = background;
    if (level - background > threshold) isSpeech = true;
    return isSpeech;
 } 
}
