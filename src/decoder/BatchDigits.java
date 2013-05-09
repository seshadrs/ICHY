package decoder;


/* 
 * Does batch decoding of digits data with Phone models (aurora/an4)
 * 
 * */


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.media.jai.operator.MaxDescriptor;

import models.LM;
import models.phoneme.Gaussian;
import models.phoneme.HMM;

import speech.audio.feat.MFCC;
import speech.audio.utils.IO;

import comirva.audio.util.math.Matrix;

public class BatchDigits {
	
	public static HashMap<String,String> wordToPhones;					//word -> phoneme
	public static ArrayList<String> phones;		//phones
	public static HashMap<String,HMM> phoneToModel ;			//phone -> HMM

	public class WordUtt
	{
		String val;
		String context;
		Double lmProb;
		int statePtr;
		int depth;
		
		WordUtt(String val, String context, Double lmProb, int statePtr, int depth)
		{
			this.val = val;
			this.context = context;
			this.lmProb = lmProb;
			this.statePtr = statePtr;
			this.depth = depth;
		}
	}
	
	public class State 
	{
		Gaussian g;
		Double probSelf;
		Double probNext;
		
		State(Gaussian g, Double probSelf,Double probNext)
		{
			this.g=g;
			this.probNext=probNext;
			this.probSelf=probSelf;
		}
	}
	
	
	
	public static HashMap<String,String> loadDict(String filename) throws IOException
	{
		Map dic = new HashMap<String,String>();// dictionary
		
		BufferedReader dicr = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = dicr.readLine()) != null) {
			String [] ll = line.split("\\s", 2);
			if (!ll[0].trim().contains("("))
				dic.put(ll[0].trim(), ll[1].trim());
		}
		dicr.close();
		return (HashMap<String, String>) dic;
	}
	
	public static HashMap<String,HMM> loadModels(ArrayList<String> phones, String path) throws IOException
	{
		 	HashMap<String, HMM> phHmms;
		 	int K=3, numFeats=39;
			phHmms = new HashMap<String, HMM>(phones.size());
			// create an HMM with 3 states for each phoneme
			for (String ph:phones){
				phHmms.put(ph,  new HMM(K,39));
			}
		
			System.out.println("In initialize..");
			
			// initialization of HMM Gaussians with phoneme models trained on IW
			for (String mm: phHmms.keySet()){
				phHmms.get(mm).setFeats(numFeats);
				phHmms.get(mm).loadModel(path+mm+".model");
			}
			
			return phHmms;
	}
	
	public static ArrayList<String> loadPhones(String filename) throws IOException
	{
		ArrayList<String> phones = new ArrayList<String>();
		BufferedReader dicr = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = dicr.readLine()) != null) {
			 phones.add(line.trim());
		}
		dicr.close();
		
		return phones;
	}
	
	
	
	public static void main(String[] args) throws IllegalArgumentException, IOException
	{
		/*Params
		 * */
		String modelFolder = "aurora/ph_cont/";		// "an4/models/ph_cont/"
		int stateCountPenaltyFactor = 1;
		boolean verbose = false;
		
		
		BatchDigits b = new BatchDigits();
		
		wordToPhones = loadDict("aurora/aurora.dic");					//word -> phoneme
		phones = loadPhones("aurora/aurora.ph");		//phones

		phoneToModel = loadModels(phones,modelFolder);			//phone -> HMM
		
		
		LM.loadLM("an4/etc/an4.trigramlm");
		
		
		HashMap<String, String> transcriptions = new HashMap<String, String>();
		ArrayList<String> idsSorted = new ArrayList<String>();
		BufferedReader dtr = new BufferedReader(new FileReader("aurora/TEST.transcripts"));
		String line;
		while ((line = dtr.readLine()) != null) 
		{
			line= line.trim();
			String id = line.substring(line.indexOf('(')+1).replace(")","");
			idsSorted.add(id);
			String transcription = line.substring(0,line.indexOf('(')).trim();
			System.out.println(id+"\t"+transcription);
			transcriptions.put(id, transcription);
			//System.out.println(id+"\t"+transcription);
		}
		
		MFCC featExtractor = new MFCC(16000, 50, 7000, 40);
		
		HashMap<String, String> idToFile = new HashMap<String, String>();
		dtr = new BufferedReader(new FileReader("aurora/TEST.filelist"));
		String filename;
		while ((line = dtr.readLine()) != null)
		{
			filename = line.trim()+".wav";
			String id = line.trim();
			//System.out.println(id+"\t--"+filename);
			idToFile.put(id, filename);
		}
		
		
		int uttNumber=1;
		
		for(String id : idsSorted) //"an391-mjwl-b an404-mdms2-b an422-menk-b an440-mjgm-b cen1-mjwl-b 	cen3-fvap-b 	cen4-mjgm-b 	cen6-fjlp-b 	cen7-miry-b an392-mjwl-b an405-mdms2-b an423-menk-b an441-mmxg-b cen1-mmxg-b 	cen3-mdms2-b cen4-mjwl-b 	cen6-fvap-b 	cen7-mjgm-b an393-mjwl-b an406-fcaw-b an424-menk-b an442-mmxg-b cen2-fcaw-b 	cen3-menk-b 	cen4-mmxg-b 	cen6-mdms2-b cen7-mjwl-b an394-mjwl-b an407-fcaw-b an425-menk-b an443-mmxg-b cen2-fjlp-b 	cen3-miry-b 	cen5-fcaw-b 	cen6-menk-b 	cen7-mmxg-b an395-mjwl-b an408-fcaw-b an426-fvap-b an444-mmxg-b cen2-fvap-b 	cen3-mjgm-b 	cen5-fjlp-b 	cen6-miry-b 	cen8-fcaw-b an396-miry-b an409-fcaw-b an427-fvap-b an445-mmxg-b cen2-mdms2-b cen3-mjwl-b 	cen5-fvap-b 	cen6-mjgm-b 	cen8-fjlp-b an397-miry-b an410-fcaw-b an428-fvap-b cen1-fcaw-b 	cen2-menk-b 	cen3-mmxg-b 	cen5-mdms2-b cen6-mjwl-b 	cen8-fvap-b an398-miry-b an416-fjlp-b an429-fvap-b cen1-fjlp-b 	cen2-miry-b 	cen4-fcaw-b 	cen5-menk-b 	cen6-mmxg-b 	cen8-mdms2-b an399-miry-b an417-fjlp-b an430-fvap-b cen1-fvap-b 	cen2-mjgm-b 	cen4-fjlp-b 	cen5-miry-b 	cen7-fcaw-b 	cen8-menk-b an400-miry-b an418-fjlp-b an436-mjgm-b cen1-mdms2-b cen2-mjwl-b 	cen4-fvap-b 	cen5-mjgm-b 	cen7-fjlp-b 	cen8-miry-b an401-mdms2-b an419-fjlp-b an437-mjgm-b cen1-menk-b 	cen2-mmxg-b 	cen4-mdms2-b cen5-mjwl-b 	cen7-fvap-b 	cen8-mjgm-b an402-mdms2-b an420-fjlp-b an438-mjgm-b cen1-miry-b 	cen3-fcaw-b 	cen4-menk-b 	cen5-mmxg-b 	cen7-mdms2-b cen8-mjwl-b an403-mdms2-b an421-menk-b an439-mjgm-b cen1-mjgm-b 	cen3-fjlp-b 	cen4-miry-b 	cen6-fcaw-b 	cen7-menk-b 	cen8-mmxg-b".replace("\t","").split(" "))
		{
			filename= id+".wav";
			System.out.println(id+"\t"+filename);
			
			//if(!idToFile.containsKey(id))
			//	continue;
			
			double [] audioData;
			
//			try{
			audioData = IO.read("/Users/sesha7/Downloads/hwdata/test/"+filename); // wav file
//			}
//			catch(Exception e)
//			{
//				continue;
//			}
			
			double[][] featVectors = featExtractor.extractAll39Features(audioData);
			Matrix mat = new Matrix(featVectors);
			
			//!!! int maxDepth=1; //3 words
			int maxDepth = transcriptions.get(id).split(" ").length+1;	//limit the depth it traverses
			
			int rows = maxDepth*1000*3 +1, cols= featVectors.length +1;
			double[][] trellis = new double[rows][cols];
			
			//initialize trellis values
			for(int i=0; i< rows; i++)
				for(int j=0; j< cols; j++)
					trellis[i][j]=Double.NEGATIVE_INFINITY;
			trellis[0][0]=0.0;
			
			//imp datastructures
			double bestProb=Double.NEGATIVE_INFINITY;
			double actualProb=Double.NEGATIVE_INFINITY;
			String bestMatch="";
			Stack<WordUtt> uttStack = new Stack<BatchDigits.WordUtt>();	//words to process
			ArrayList<State> states = new ArrayList<BatchDigits.State>();
			
			//load the vocab
			for (String word : wordToPhones.keySet())
			{
				String context = "<s>";
				WordUtt firstWord = b.new WordUtt(word,context,LM.getDummyProbability(context, word),0,1); 
				uttStack.add(firstWord);
			}
			
			WordUtt curWord;
			HMM hmm;
			double[][] transitions;
			double emissionProb, emissionProbRaw, transitionProb=0.0;
			double totalPrSelf, totalPrNext;
			while(uttStack.size()>0)
			{
				curWord = uttStack.pop();
				
				//add states for cur word
				
				//beginning silence
				HMM silHmm = phoneToModel.get("SIL");
				double[][] silTransitions= silHmm.getTransitions();
				for (int i=0; i<3;i++)
					states.add(b.new State(silHmm.getState(i), 
							Math.log10(silTransitions[i][i]),		 
							(i<2)? Math.log10(silTransitions[i][i+1]):Math.log10(1.0-silTransitions[i][i]))
					);
				
				for(String ph : wordToPhones.get(curWord.val).split(" "))
					{
						hmm= phoneToModel.get(ph);
						transitions=hmm.getTransitions();
						
	//					for(int i=0; i< 3;i++)
	//						{
	//							for(int j=0; j< 3;j++)
	//								System.out.print(transitions[i][j]+"->"+Math.log10(transitions[i][j])+" , ");
	//							System.out.println();
	//						}
						
						for (int i=0; i<3;i++)
							states.add(b.new State(hmm.getState(i), 
									Math.log10(transitions[i][i]),		 
									(i<2)? Math.log10(transitions[i][i+1]):Math.log10(1.0-transitions[i][i]))
							);
					
					}
				//end-silence
				for (int i=0; i<3;i++)
					states.add(b.new State(silHmm.getState(i), 
							Math.log10(silTransitions[i][i]),		 
							(i<2)? Math.log10(silTransitions[i][i+1]):Math.log10(1.0-silTransitions[i][i]))
					);
				
	//			if (curWord.depth>1)
	//			{
	//				for(int l=0; l<cols;l++)
	//					System.out.print(trellis[curWord.statePtr-1][l]+" _ ");
	//				System.out.println();
	//			}
				
				boolean exit = false;
				for (int i=curWord.statePtr+1; i<states.size()+1;i++)	//for every state	//!+1
				{	
					
					for (int j=1; j< cols; j++)		//for every cell in Column
					{
						totalPrSelf=Double.NEGATIVE_INFINITY;
						totalPrNext=Double.NEGATIVE_INFINITY;
						emissionProbRaw = HMM.getEmissionProb(mat,j-1,states.get(i-1).g);
						emissionProb = (emissionProbRaw==0.0)? 0.0 : Math.log10(emissionProbRaw);
						
						if (trellis[i][j-1]!=Double.NEGATIVE_INFINITY)	//self
						{
							transitionProb = states.get(i-1).probSelf;
							totalPrSelf = trellis[i][j-1]+emissionProb + transitionProb;
						}
						
						if (trellis[i-1][j-1]!=Double.NEGATIVE_INFINITY)	//next
						{
							transitionProb = (i==1)? 0.0 : states.get(i-2).probNext;
							totalPrSelf = trellis[i-1][j-1]+emissionProb + transitionProb;
						}
						
						  trellis[i][j] = Math.max(totalPrNext, totalPrSelf);		//best-path
						
						  /*//!!!
						//sum-of-paths
						if (Math.max(totalPrNext, totalPrSelf)!=Double.NEGATIVE_INFINITY)
							trellis[i][j] = ((totalPrNext!=Double.NEGATIVE_INFINITY)? totalPrNext: 0.0) + ((totalPrSelf!=Double.NEGATIVE_INFINITY)? totalPrSelf: 0.0);
						else
							trellis[i][j] = Math.max(totalPrNext, totalPrSelf);
							*/
						
						//System.out.println(curWord.context+" "+curWord.val+" "+trellis[i][j]);
						//System.out.println(curWord.context+" "+curWord.val+"\t ("+states.size()+") \t"+i+","+j+"="+trellis[i][j]+"\t"+transitionProb+" "+emissionProb+" "+trellis[i][j-1]+" "+trellis[i-1][j-1]);
							
					}
					
					if((trellis[i][cols-1]==Double.NEGATIVE_INFINITY || (cols%5==0 && trellis[i][cols-1] 
							//+ curWord.lmProb 
							- stateCountPenaltyFactor * states.size() 
							<  1.1 * bestProb)) && bestProb != Double.NEGATIVE_INFINITY)
					{
						//System.out.println("breaking");
						break;
					}
				}
				
	//			if (curWord.depth>1)
	//			{
	//				for(int l=0; l<cols;l++)
	//					System.out.print(trellis[states.size()-1][l]+" , ");
	//				System.out.println();
	//			}
				
				//observe final Prob, remember if best
				
				Double uttProb = trellis[states.size()][cols-1]	//AM-score 
								  // + curWord.lmProb				//LM-score
								 - stateCountPenaltyFactor * states.size();
				if( uttProb  > bestProb)
					{
						bestProb = uttProb;
						bestMatch = curWord.context+" "+curWord.val;
						//System.out.println(bestMatch+" at "+bestProb);
					}
				
				if (verbose)
					System.out.println("Utt "+uttNumber+" "+id+":\t"+"EOW : "+curWord.context+" "+curWord.val+" -> "+ uttProb);	
				
				if ((curWord.context+" "+curWord.val).replace("<s>", "").trim().compareTo(transcriptions.get(id).trim())==0)
					actualProb = uttProb;
						
				
				//if cur depth is maxdepth, rollback
				if (curWord.depth==maxDepth)
				{
					//System.out.println("rolling-back "+curWord.context+" "+curWord.val+" -> "+trellis[states.size()-1][cols-1]+"\t ("+states.size()+")");
					
					//clear trellis
					for (int i=curWord.statePtr+1; i<states.size()+1;i++)	
						for(int j=1; j<cols;j++)
							trellis[i][j]=Double.NEGATIVE_INFINITY;
						
					//clear states
					if(uttStack.size()!=0 && uttStack.peek().depth==curWord.depth-1)
						states.clear();
					else
					{	int prevStateSize=states.size();
						for (int i=curWord.statePtr; i< prevStateSize;i++)
							states.remove(states.size()-1);
					}
					
	//				for(int l=0; l<cols;l++)
	//					System.out.print(trellis[states.size()-1][l]+" ");
	//				System.out.println();
					
					//System.out.println("New state count\t ("+states.size()+")");
					
				}
				else
				{
					//add children to stack
					for (String word : wordToPhones.keySet())
					{
						String context = curWord.context+" "+curWord.val;
						WordUtt nextWord = b.new WordUtt(word,context,curWord.lmProb+LM.getDummyProbability(context, word), states.size(),curWord.depth+1); 
						uttStack.add(nextWord);
					}
				}
			}
		
			System.out.println("Utt "+uttNumber+" "+id+":\tPRED = "+bestMatch+" "+bestProb+".\tACTUAL="+transcriptions.get(id)+" "+actualProb);
			uttNumber+=1;
	}
		
		System.out.println(new Date().toString());
	}
	
}
