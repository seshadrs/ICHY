package Speech.Audio;

import java.util.ArrayList;

public class EnergyEndpointer {
  
  /* 
   * frame classifier parameters
   */
  private double level = Double.NEGATIVE_INFINITY;
  private double forgetfactor = 1.5;
  private double adjustment = 0.05;
  private double threshold = 20;
  private double background;
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
    return true;
  }
  
  /*
   * Record the background noise level
   */
  public void getBackground(byte buffer[]){
    int frame [] = extractFrame(buffer);
    //double currEnergy;
    background = getFrameEnergy(frame);
    System.out.println(frame);
    System.out.println(background);
  }
  
  
  /*
   * Adds a frame to endpointer's lookback memory
   * Also keeps track of how many frames have been added 
   */
  public void addFrame(byte buffer[]){
    int frame [] = extractFrame(buffer);
    double currEnergy;
    
    // if first frame, set level to energy of first frame and return
    if (level == Double.NEGATIVE_INFINITY){
      level = getFrameEnergy(frame);
    }
    
    // after certain number of frames, remove old frame
    if (tracker.size() == lookback){
      tracker.remove(0);
    }
    
    currEnergy = getFrameEnergy(frame);
    tracker.add(classifyFrame(currEnergy));
  }
  
  /*
   * Extract samples for the frame from the byte buffer
   * TODO: put this in utils since we need this for ASR too.
   */
  private int [] extractFrame(byte buffer[]){
    int cur = 0;
    int idx = 0;
    int fval = 0;
    int frame [] = new int[buffer.length/2];
    while (cur < buffer.length-1){
      fval = 0;
      //System.out.println("Fval: \t is "+ String.format("%x", fval));
      fval  = (buffer[cur+1] << 8) & 0x0000ff00;
      fval  = fval | buffer[cur];
      frame[idx] = fval & 0x0000ffff;
      idx += 1;
      cur += 2;
    }
    return frame;
  }
  
  /*
   * Calculate the frame energy in decibels
   */
  private double getFrameEnergy(int frame[]){
    double energy = 0;
    for (int cur : frame){
      energy = energy + Math.pow(cur, 2);
    }
    energy = 10 * Math.log(energy);
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
