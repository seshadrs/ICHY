package decoder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import speech.audio.feat.MFCC;
import speech.audio.utils.IO;

import comirva.audio.util.math.Matrix;

import models.LM;
import models.phoneme.Gaussian;
import models.phoneme.HMM;

public class ApproximateDecoder_AuroraWord {
	
	
	public class State
	{
		int index;						//unique index of the state
		boolean isFirstStateInword;
		
		Gaussian g;						//gaussian representing the state
		double selfProb;				//nextStateProb is '1-selfProb'
		
		String word;					//the word that the state belongs to
		String prevWord;				//the word that preceeded the state during decoding
		String history;					//the history that preceeded the state during decoding
		
		ArrayList<State> ancestors;		//acts as backpointers. the states that can lead to this state
		
		State(int index, Gaussian g, double selfProb, String word)
		{
			this.index = index;
			this.g =g;
			this.selfProb = selfProb;
			
			this.word = word;
			this.prevWord = "";
			this.history = "";
			
			this.ancestors = new ArrayList<ApproximateDecoder_AuroraWord.State>();
		}
		
		private void reset()
		{
			this.prevWord = "";
			this.history = "";
		}
	}
	
	
	public static HashMap<String,String> wordToPhones;				//word -> phoneme
	public static ArrayList<String> phones;							//phones
	public static HashMap<String,HMM> phoneToModel ;				//phone -> HMM
	
	
	
	public static void main(String[] args) throws IOException
	{
		/* CONTROL FILES */
		String wordDictionaryPath = "aurora/aurora.wdic";
		String phoneListPath = "aurora/aurora.wo";
		String acousticModelPath = "aurora/wo_cont/";
		String languageModelPath = "an4/etc/an4.trigramlm";
		String transcriptionFilePath = "aurora/TEST.transcripts";
		String testFilesListPath = "aurora/TEST.filelist";
		
		/* PARAMETERS */
		double lmLProbWeight = 1.0;
		double insertionPenaltyLProbPerWord = 0.0; 
		
		/* LOAD DICTIONARIES, MODELS */
		wordToPhones = Utils.loadDict(wordDictionaryPath);				//word -> phoneme
		phones = Utils.loadPhones(phoneListPath);						//phones list
		phoneToModel = Utils.loadModels(phones,acousticModelPath);		//phone -> HMM
		LM.loadLM(languageModelPath);									//Language Model
		
		
		ApproximateDecoder_AuroraWord decoder = new ApproximateDecoder_AuroraWord();			//decoder obj
		
		ArrayList<State> states= new ArrayList<ApproximateDecoder_AuroraWord.State>();		//list of all decoder states
		
		/* ADD STATES FOR WORDS : BEGIN_SIL, VOCAB WORDS, END_SIL */
		State newState;
		State prevStateInWord;
		HMM hmm;
		Gaussian[] gaussians;
		double[][] transitions;
		Integer begin_silFirstStateIndex = 0;
		Integer begin_silLastStateIndex;
		ArrayList<Integer> wordsFirstStateIndices = new ArrayList<Integer>();
		ArrayList<Integer> wordsLastStateIndices = new ArrayList<Integer>();
		Integer end_silFirstStateIndex;
		Integer end_silLastStateIndex;
		
		hmm = phoneToModel.get("SIL");
		prevStateInWord = null;
		gaussians = hmm.getStates();
		transitions = hmm.getTransitions();
		for(int i=0; i<gaussians.length; i++)						//for every gaussian in the silence hmm
			{
				newState = decoder.new State(states.size(), gaussians[i], transitions[i][i], "<s>");
				newState.ancestors.add(newState);
				states.add(newState);
				if(prevStateInWord!=null)
					newState.ancestors.add(prevStateInWord);
				prevStateInWord = newState;
			}
		begin_silLastStateIndex = states.size()-1;
		states.get(begin_silFirstStateIndex).isFirstStateInword = true;
		
		Integer wordFirstStateIndex;
		for (String word : wordToPhones.keySet())						//for every word in vocab
		{
			wordFirstStateIndex = states.size();
			wordsFirstStateIndices.add(wordFirstStateIndex);
			prevStateInWord=null;
			for(String phone: wordToPhones.get(word).split(" "))		//for every phone in the word
			{
				hmm = phoneToModel.get(phone);
				gaussians = hmm.getStates();
				transitions = hmm.getTransitions();
				for(int i=0; i<gaussians.length; i++)						//for every gaussian in the hmm
					{
						newState = decoder.new State(states.size(), gaussians[i], transitions[i][i], word);
						newState.ancestors.add(newState);
						states.add(newState);
						if(prevStateInWord!=null)
							newState.ancestors.add(prevStateInWord);
						prevStateInWord = newState;
					}
			}
			wordsLastStateIndices.add(states.size()-1);
			states.get(wordFirstStateIndex).isFirstStateInword = true;
		}
		
		end_silFirstStateIndex = states.size();
		hmm = phoneToModel.get("SIL");
		prevStateInWord = null;
		gaussians = hmm.getStates();
		transitions = hmm.getTransitions();
		for(int i=0; i<gaussians.length; i++)						//for every gaussian in the silence hmm
		{
			newState = decoder.new State(states.size(), gaussians[i], transitions[i][i], "</s>");
			newState.ancestors.add(newState);
			states.add(newState);
			if(prevStateInWord!=null)
				newState.ancestors.add(prevStateInWord);
			prevStateInWord = newState;
		}
		end_silLastStateIndex = states.size()-1;
		states.get(end_silFirstStateIndex).isFirstStateInword = true;
		
		
		/* DEFINE ANCESTORS (BACK-POINTERS) FOR THE GRAPH */
		State wordFirstState, wordLastState; 
		State beg_silLastState = states.get(begin_silLastStateIndex);
		State end_silFirstState = states.get(end_silFirstStateIndex);
		
		for(Integer i : wordsFirstStateIndices)						//wire words in vocab to beginning silence
		{
			wordFirstState = states.get(i);
			wordFirstState.ancestors.add(beg_silLastState);
		}
		
		for(Integer i : wordsFirstStateIndices)						//wire words in vocab to each other
		{
			wordFirstState = states.get(i);
			for(Integer j : wordsLastStateIndices)
			{
				wordLastState = states.get(j);
				wordFirstState.ancestors.add(wordLastState);
			}
		}
		
		for(Integer i : wordsLastStateIndices)						//wire ending silence to words in vocab
		{
			wordLastState = states.get(i);
			end_silFirstState.ancestors.add(wordLastState);
		}
		
		
		/* READ TRANSCRIPTIONS*/
		HashMap<String, String> transcriptions = new HashMap<String, String>();
		ArrayList<String> idsSorted = new ArrayList<String>();
		BufferedReader dtr = new BufferedReader(new FileReader(transcriptionFilePath));
		String line;
		while ((line = dtr.readLine()) != null) 
		{
			line= line.trim();
			String id = line.substring(line.indexOf('(')+1).replace(")","");
			idsSorted.add(id);
			String transcription = line.substring(0,line.indexOf('(')).trim();
			transcriptions.put(id, transcription);
		}
		
		HashMap<String, String> idToFile = new HashMap<String, String>();
		dtr = new BufferedReader(new FileReader(testFilesListPath));
		String filename;
		while ((line = dtr.readLine()) != null)
		{
			filename = line.trim();
			String id = line.trim()+".wav";
			idToFile.put(id, filename);
		}
		
		
		/*for(State s : states.get(0).ancestors)
			System.out.println(s.index);
		for(State s : states.get(end_silFirstStateIndex).ancestors)
			System.out.println(s.index);*/
		
		
		double[][] trellis;
		MFCC featExtractor;
		double[][] featureVectors;
		Matrix featureMatrix;
		double [] audioData;
		int cols,rows,uttNumber=1;
		for(String id : idsSorted) 						//for every audio file
		{
			filename= id+".wav";
			
			/* EXTRACT FEATURES */
			featExtractor = new MFCC(16000, 50, 7000, 40);
			try
			{
				audioData = IO.read("/Users/sesha7/Downloads/hwdata/test/"+filename); // wav file
			}
			catch(Exception e)
			{
				System.out.println("Error reading audio file "+filename+". "+e.toString());
				continue;
			}
			featureVectors = featExtractor.extractAll39Features(audioData);
			featureMatrix = new Matrix(featureVectors);
			
			
			/* RESET STATES IF NECESSARY */
			if(uttNumber>1)
				for (State x : states)
					x.reset();
			
			/* DECODING */
			rows = states.size();
			cols = featureVectors.length+1;
			trellis = new double[rows][cols];					//allocate the search trellis
			State curState, bestAncestor=null;
			String trigramContext;
			double ancestorLProb, candidateLProb, bestLProb, emissionLProb;
			
			trellis[0][0] = 0.0;								//very first state (has to be silence)
			for(int i=1;i<rows;i++)
				trellis[i][0] = Double.NEGATIVE_INFINITY;
			
			for(int i=1; i< cols; i++)						//for every column in the trellis (starting with 2nd one) 
			{
				for(int j=0;j<rows;j++)
				{
					curState = states.get(j);
					emissionLProb = Math.log10(HMM.getEmissionProb(featureMatrix, i-1, curState.g));		//TODO: take log10(prob)
					
					bestLProb = Double.NEGATIVE_INFINITY;
					candidateLProb = Double.NEGATIVE_INFINITY;
					for(State ancestor : curState.ancestors)
					{
						ancestorLProb = trellis[ancestor.index][i-1];
						if(ancestorLProb != Double.NEGATIVE_INFINITY)	//if path through ancestor is possible
							if(curState.isFirstStateInword && curState.index != ancestor.index)
							{
								trigramContext = ancestor.prevWord+" "+ancestor.word;
								candidateLProb = ancestorLProb + lmLProbWeight*LM.probability(trigramContext, curState.word) - insertionPenaltyLProbPerWord + Math.log10(1.0-ancestor.selfProb) + emissionLProb;
							}
							else if(curState.index == ancestor.index)
							{
								candidateLProb = ancestorLProb + Math.log10(ancestor.selfProb) + emissionLProb;
							}
							else
							{
								candidateLProb = ancestorLProb + Math.log10(1.0-ancestor.selfProb) + emissionLProb;
							}
						if(candidateLProb > bestLProb)
						{
							bestLProb = candidateLProb;
							bestAncestor = ancestor;
						}
					}
					
					if (bestAncestor != null)			//if the best ancestor was found
					{
						trellis[j][i] = bestLProb;
						if(curState.isFirstStateInword && curState.index != bestAncestor.index)
						{
							curState.prevWord = bestAncestor.word;
							curState.history = bestAncestor.history + " " + bestAncestor.word;
							
						}
						else
						{
							curState.prevWord = bestAncestor.prevWord;
							curState.history = bestAncestor.history;
						}
					}
						
					else
						trellis[j][i] = Double.NEGATIVE_INFINITY;
				}
			}
			
			/*for(int i=0; i<rows;i++)
				System.out.print(trellis[i][cols-1]+" ");
			System.out.println();*/
			
			String result = states.get(end_silLastStateIndex).history+" </s>";
			String transcription = transcriptions.get(id);
			System.out.println("UTT-"+uttNumber+"\t"+transcription+"\t->\t"+result);
			
			/*for(int i=0; i<rows;i++)
			{
				result = states.get(i).history+" </s>";
				transcription = transcriptions.get(id);
				System.out.println(transcription+"\t->\t"+result);
			}*/
			
			uttNumber+=1;
		}
		
		
		
		
	}
	

}
