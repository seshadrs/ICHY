package models.phoneme;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import comirva.audio.util.math.Matrix;

public class PhonemeHmmTester {

	private int K = 3;
	protected HashMap<String, HMM> phHmms;
	private HashMap<String, ArrayList<String>> nsegs;
	private HashMap<String, HashMap<Integer,Integer>> ntrans;
	private HashMap<String, String> gold;
	
	
	//****** Constructor *********//
	public PhonemeHmmTester(ArrayList<String> phDic){
		phHmms = new HashMap<String, HMM>(phDic.size());
		// create an HMM with 3 states for each phoneme
		for (String ph:phDic){
			phHmms.put(ph,  new HMM(K));
		}
		//this.dic = dic;
	}
	
	
	//****** Test continuous words against phoneme candidates *********//
	
	private void initialize(int numFeats, String path) throws IOException{
		System.out.println("In initialize..");
		
		// initialization of HMM Gaussians with phoneme models trained on IW
		for (String mm: phHmms.keySet()){
			phHmms.get(mm).setFeats(numFeats);
			phHmms.get(mm).loadModel(path+mm+".model");
		}
		
	}
	
	// compose an HMM from the transcript
	// do a forward algo through Hmm to get score
	private ArrayList<Double> score(ArrayList<String>words, Matrix features, int numFeats){
		//System.out.println("In score..");
		//System.out.println("Calculating Word Scores.....");
		ArrayList<Double> scores = new ArrayList<Double>(words.size());
		for(int i=0; i<words.size(); i++){
			String [] phonemes = words.get(i).split("\\s");
			HMM cwhmm = new HMM(phonemes.length*K, numFeats);
			double [][] transitions = new double[phonemes.length*K][phonemes.length*K];
			for (int tii=0; tii<transitions.length;tii++){
				for (int tjj=0;tjj<transitions[0].length;tjj++){
					transitions[tii][tjj] = 0.0;
				}
			}
			Gaussian [] states = new Gaussian[phonemes.length*K];
			HashMap<Integer,String>map = new HashMap<Integer, String>(phonemes.length*K);
			int stateId = 0;
			int sit = 0;
			for(String phoneme : phonemes){
				/*if (stateId != 0){
					transitions[stateId-1][stateId] = 1.0;
				}*/
				double [][] trans = phHmms.get(phoneme).getTransitions();
				for(int t1=0;t1<trans.length;t1++){
					for(int t2=0;t2<trans.length;t2++){
						transitions[stateId+t1][stateId+t2] = trans[t1][t2];
					}	
				}
				sit = 0;
				for(Gaussian s: phHmms.get(phoneme).getStates()){
					states[stateId] = s;
					map.put(stateId, phoneme+";"+sit);
					stateId ++;
					sit ++;
				}
				if (stateId<transitions.length){
					//transitions[stateId-1][stateId] = phHmms.get(phoneme).getEndProb();
					transitions[stateId-1][stateId] = 1 - trans[2][2];
				}
			}
			cwhmm.setTransitions(transitions);
			cwhmm.setStates(states);
			
			//cwhmm.Viterbi(features);
			// get score
			double score = cwhmm.Score(features);//-phonemes.length;//(phonemes.length+1);
			scores.add(score);
			//System.out.println(words.get(i) + " : " + score);
		}
		return scores;
	}
	
	public int testOne(ArrayList<String> words, Matrix feats, int numFeats) throws IOException{
		String path = "an4/models/ph_cont50/";
		initialize(numFeats, path);
		ArrayList<Double>scores = score(words, feats, numFeats);
		double maxsc = scores.get(0);
		int maxk = 0;
		for (int k=1;k<scores.size();k++){
			if (scores.get(k) > maxsc){
				maxsc = scores.get(k);
				maxk = k;
			}
		}
		return maxk;
	}
	
	public void test(ArrayList<String> words, ArrayList<Matrix> feats, ArrayList<String> ids, int numFeats, ArrayList<String> fullwords ) throws IOException{
		String path = "aurora/models/ph_cont/";
		BufferedWriter ofi = new BufferedWriter(new FileWriter("aurora/etc/aurora_test_8000.scores"));
		initialize(numFeats, path);
		double correct = 0;
		System.out.println("ID, Gold, Recognized");
		for (int idx=0; idx<feats.size();idx++){
			Matrix m = feats.get(idx);
			ArrayList<Double>scores = score(words, m, numFeats);
			ofi.write(ids.get(idx)+"\n");
			double maxsc = scores.get(0);
			int maxk = 0;
			ofi.write(maxsc+"\t");
			for (int k=1;k<scores.size();k++){
				if (scores.get(k) > maxsc){
					maxsc = scores.get(k);
					maxk = k;
				}
				ofi.write(scores.get(k)+"\t");
			}
			ofi.write("\n");
			if (maxk == idx){
				correct++;
			}
			System.out.println(ids.get(idx)+", \""+fullwords.get(idx)+"\", \""+fullwords.get(maxk)+"\"");
			System.out.println("Scores"+", "+scores.get(idx)+", "+scores.get(maxk));
			System.out.println();
		}
		ofi.close();
		double acc = correct/feats.size();
		System.out.println("Correctly recognized: " + correct);
		System.out.println("Total samples: " + feats.size());
		System.out.println("Accuracy: " + acc);
	}
	

}
