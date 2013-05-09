package decoder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import models.LM;
import models.phoneme.Gaussian;
import models.phoneme.HMM;

import speech.audio.feat.MFCC;
import speech.audio.utils.IO;

import comirva.audio.util.math.Matrix;

public class Match {
	
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
		while ((line = dicr.readLine()) != null && dic.size()<10) {
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
		Match m = new Match();
		
		wordToPhones = loadDict("an4/etc/an4.dic");					//word -> phoneme
		phones = loadPhones("an4/etc/an4.phone");		//phones
		phoneToModel = loadModels(phones,"an4/models/ph_cont/");			//phone -> HMM
		LM.loadLM("data/an4.trigramlm");
		
		
		
		MFCC featExtractor = new MFCC(16000, 50, 7000, 40);
		//double [] audioData = IO.read("/Users/sesha7/Downloads/an4-1/wav/test/mmxg/cen3-mmxg-b.wav"); // wav file
		double [] audioData = IO.read("/Users/sesha7/Downloads/an4-1/wav/test/mmxg/an442-mmxg-b.wav"); // wav file
		//double [] audioData = IO.read("/Users/sesha7/Downloads/an4-1/wav/train/mtxj/an378-mtxj-b.wav"); // wav file
		
		double[][] featVectors = featExtractor.extractAll39Features(audioData);
		Matrix mat = new Matrix(featVectors);
		
		int maxDepth=2; //3 words
		int rows = maxDepth*1000*3 +1, cols= featVectors.length +1;
		double[][] trellis = new double[rows][cols];
		
		//initialize trellis values
		for(int i=0; i< rows; i++)
			for(int j=0; j< cols; j++)
				trellis[i][j]=Double.NEGATIVE_INFINITY;
		trellis[0][0]=0.0;
		
		//imp datastructures
		double bestProb=Double.NEGATIVE_INFINITY;
		String bestMatch="";
		Stack<WordUtt> uttStack = new Stack<Match.WordUtt>();	//words to process
		ArrayList<State> states = new ArrayList<Match.State>();
		
		//load the vocab
		for (String word : wordToPhones.keySet())
		{
			String context = "<s>";
			WordUtt firstWord = m.new WordUtt(word,context,LM.getProbability(context, word),0,1); 
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
						states.add(m.new State(hmm.getState(i), 
								Math.log10(transitions[i][i]),		 
								(i<2)? Math.log10(transitions[i][i+1]):Math.log10(1.0-transitions[i][i]))
						);
				
				}
			
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
				
				if(trellis[i][cols-1]==Double.NEGATIVE_INFINITY)
				{
					System.out.println("breaking");
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
							 + curWord.lmProb				//LM-score
							 + 1.5 * states.size();
			if( uttProb  > bestProb)
				{
					bestProb = uttProb;
					bestMatch = curWord.context+" "+curWord.val;
					//System.out.println(bestMatch+" at "+bestProb);
				}
			
			System.out.println("EOW : "+curWord.context+" "+curWord.val+" -> "+ uttProb);	
			
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
					WordUtt nextWord = m.new WordUtt(word,context,curWord.lmProb+LM.getProbability(context, word), states.size(),curWord.depth+1); 
					uttStack.add(nextWord);
				}
			}
		}
	
		System.out.println("BEST PRED = "+bestMatch+" @ "+bestProb);
	}
	
}
