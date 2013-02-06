package speech.audio.utils;

public class SignalData {
	
	
	/*
	   * Extract samples for the frame from the byte buffer
	   * TODO: put this in utils since we need this for ASR too.
	   */
	  public static int[] toIntArr(byte buffer[]){
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
	   * Extract samples for the frame from the byte buffer
	   * TODO: put this in utils since we need this for ASR too.
	   */
	  public static float[] toFloatArr(byte buffer[]){
	    int cur = 0;
	    int idx = 0;
	    int fval = 0;
	    float frame [] = new float[buffer.length/2];
	    while (cur < buffer.length-1){
	      fval = 0;
	      //System.out.println("Fval: \t is "+ String.format("%x", fval));
	      fval  = (buffer[cur+1] << 8) & 0x0000ff00;
	      fval  = fval | buffer[cur];
	      frame[idx] = (float) (fval & 0x0000ffff);
	      idx += 1;
	      cur += 2;
	    }
	    return frame;
	  }
	  
	  
	  /*
	   * Extracts a frame out of the audio data of the specified start and end locations 
	   * */
	  public static byte[] extractFrame(byte[] data, int start, int end)
	  {
		  byte[] frame = new byte[end-start];
		  for(int i=start; i<end; i++)
		  {
			  frame[i-start]=data[i];
		  }
		  
		  return frame;
		  
	  }
	  
	  

}
