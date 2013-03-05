/* 
 * AUTHOR: seshadrs
 * Feb 2 2013
 * 
 * */

package speech.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Scanner;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import speech.audio.utils.SignalData;




public class Recorder {
	
	private boolean keepListening = false;	//flag for listening state
	private boolean keepRecording = false;	//flag for recording state
	
	ByteArrayOutputStream data;				//audio data stream
	
	EnergyEndpointer eep = new EnergyEndpointer();
	
	/*
	 * Starts listening to audio on initialization
	 * */
	public Recorder()
	{
		listenToAudio();
	}
	
	/*
	 * Gets the DataLine, listens to audio continuously until recording.and or listening is stopped.
	 * */
	 private void listenToAudio() {
		    try 
		    {
		      final AudioFormat format = getFormat();
		      DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		      final TargetDataLine line = (TargetDataLine)  AudioSystem.getLine(info);
		      
		      line.open(format);
		      line.start();
		      
		      Runnable runner = new Runnable() {
		        //int bufferSize = (int)format.getSampleRate() * format.getFrameSize();
		        byte buffer[] = new byte[4800];
		        int frameSampleSize = EnergyVAD.frameLengthMs * 16;
		 
		        public void run() {
		          data = new ByteArrayOutputStream();
		          keepListening = true;
		          try {
		        	  
		        	  boolean begunRecording = false;
		        	  boolean isEndpoint = false;
		        	  
		            while (keepListening && !isEndpoint) {
		              int count = line.read(buffer, 0, buffer.length);
		              
		              if(!keepRecording)	//for VAD to learn background model
		              {
		            	  for(int i=0; i<count; i++)
      		  		  	{
      	  					int j = Math.max(i + frameSampleSize, count);	
      	  					byte[] frame = SignalData.extractFrame(buffer, i, j);
      	  					EnergyVAD.modelBackground(frame);
      	  					i=j;
      	  					
      		  		  	}
		              }
		              
		              if (keepRecording && count>0)
		            	  {
	            	  		if (begunRecording)
	            	  			{
	            		  		  	/*eep.addFrame(buffer);
	            		  		  	eep.isEndpoint();*/
	            	  				
	            	  				for(int i=0; i<count; i++)
	            		  		  	{
	            	  					int j = Math.max(i + frameSampleSize, count);	
	            	  					byte[] frame = SignalData.extractFrame(buffer, i, j);
	            	  					isEndpoint = EnergyVAD.speechEndpoint(frame);
	            	  					if (isEndpoint)
	            	  						{
	            	  							System.out.println("SPEECH ENDPOINT");
	            	  							break;
	            	  						}
	            	  					data.write(frame, 0, j-i);
	            	  					i=j;
	            	  					
	            		  		  	}
	            		  		  	
	            		  		  }
	            		  	  else
	            		  		  begunRecording = true;
		            	  }
		              else
		              {//listening to background audio
		            	eep.getBackground(buffer);
		            	  
		              }
		            }
		            data.close();
		            stopRecording();
		          } catch (IOException e) {
		            System.err.println("I/O problems: " + e);
		            System.exit(-1);
		          }
		        }
		      };
		      
		      Thread captureThread = new Thread(runner);
		      captureThread.start();
		    } 
		    
		    catch (LineUnavailableException e) 
		    {
		      System.err.println("Line unavailable: " + e);
		      System.exit(-2);
		    }
		  }
	 					
	 /*
	  * Starts recording audio
	  * */
	 public void startRecording()
	 {
		 keepRecording = true;
	 }
	 
	 /*
	  * Stops audio recording. File is saved in the specified location
	  * */
	 public void stopRecording()
	 {
		 keepRecording = false;
		 keepListening = false;	//exits the audio listening thread
	 }
	 
	 /*
	  * Plays back audio recorded
	  * */
	 private void playRecording() {
		    try {
		      byte audio[] = data.toByteArray();
		      InputStream input = 
		        new ByteArrayInputStream(audio);
		      final AudioFormat format = getFormat();
		      final AudioInputStream ais = 
		        new AudioInputStream(input, format, 
		        audio.length / format.getFrameSize());
		      DataLine.Info info = new DataLine.Info(
		        SourceDataLine.class, format);
		      final SourceDataLine line = (SourceDataLine)
		        AudioSystem.getLine(info);
		      line.open(format);
		      line.start();

		      Runnable runner = new Runnable() {
		        int bufferSize = (int) format.getSampleRate() 
		          * format.getFrameSize();
		        byte buffer[] = new byte[bufferSize];
		 
		        public void run() {
		          try {
		            int count;
		            while ((count = ais.read(
		                buffer, 0, buffer.length)) != -1) {
		              if (count > 0) {
		                line.write(buffer, 0, count);
		              }
		            }
		            line.drain();
		            line.close();
		          } catch (IOException e) {
		            System.err.println("I/O problems: " + e);
		            System.exit(-3);
		          }
		        }
		      };
		      Thread playThread = new Thread(runner);
		      playThread.start();
		    } catch (LineUnavailableException e) {
		      System.err.println("Line unavailable: " + e);
		      System.exit(-4);
		    } 
		  }
	 
	 /*
	  * Saves audio to disk as WAV file
	  * */
	 private boolean saveRecording() throws IOException
	 {
		 
		 byte[] audioData = data.toByteArray();
		 ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
         AudioInputStream outputAIS = new AudioInputStream( bais, getFormat(), (long) audioData.length/2);

         AudioSystem.write(outputAIS, Type.WAVE, new File("./output/rec.wav"));
		 return true;
		 
	 }
	 
	 /*
	  * Returns the audio format object
	  * */
	 private AudioFormat getFormat() 
	 {
		    float sampleRate = 16000;
		    int sampleSizeInBits = 16;
		    int channels = 1;
		    boolean signed = true;
		    boolean bigEndian = true;
		    return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	 }
	 
	 /*
	  * Returns when waits Enter key is pressed
	  * */
	 private void waitOnEnter()
	 {

		 Scanner sc = new Scanner(System.in);
	     while(!sc.nextLine().equals(""));
	     startRecording();
	     return;
		 
	 }
	 
	 public static void main(String[] args)
	 {
		 final Recorder r = new Recorder();
		 
		 System.out.println("Press the enter key to start recording.");
		 r.waitOnEnter();		//wait for the enter key press
		 r.startRecording();
		 
		 final long MAX_RECORD_TIME = 10*1000;	//in milliseconds
		 final long SLEEP_TIME = 500;
		 long timeSlept = 0;
		 
		 try {
			
			 while(r.keepListening && timeSlept<= MAX_RECORD_TIME)
				 {
				 	Thread.sleep(SLEEP_TIME);
				 	timeSlept += SLEEP_TIME;
				 }
		
		 } catch (InterruptedException e) {
			e.printStackTrace();
		}
	     
		 r.stopRecording();	//to set the bool vars when max-time out occurs
		 r.playRecording();
		 try {
			r.saveRecording();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	     
		 
	 }
	 
}
