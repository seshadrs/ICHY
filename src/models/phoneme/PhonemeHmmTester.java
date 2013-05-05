package models.phoneme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import comirva.audio.util.math.Matrix;

public class PhonemeHmmTester {

	private int K = 3;
	protected HashMap<String, HMM> phHmms;
	private HashMap<String, ArrayList<String>> nsegs;
	private HashMap<String, HashMap<Integer,Integer>> ntrans;
	
	
	//****** Constructor *********//
	public PhonemeHmmTester(ArrayList<String> phDic){
		phHmms = new HashMap<String, HMM>(phDic.size());
		// create an HMM with 3 states for each phoneme
		for (String ph:phDic){
			phHmms.put(ph,  new HMM(K));
		}
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
	private void score(ArrayList<String>words, Matrix features, int numFeats){
		System.out.println("In score..");
		nsegs = new HashMap<String, ArrayList<String>>();
		ntrans = new HashMap<String, HashMap<Integer,Integer>>();
		System.out.println("Current Word Scores.....");
		for(int i=0; i<words.size(); i++){
			String [] phonemes = words.get(i).split("\\s");
			HMM cwhmm = new HMM(phonemes.length*K, numFeats);
			double [][] transitions = new double[phonemes.length*K][phonemes.length*K];
			Gaussian [] states = new Gaussian[phonemes.length*K];
			HashMap<Integer,String>map = new HashMap<Integer, String>(phonemes.length*K);
			int stateId = 0;
			int sit = 0;
			for(String phoneme : phonemes){
				if (stateId != 0){
					transitions[stateId-1][stateId] = 1.0;
				}
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
			}
			cwhmm.setTransitions(transitions);
			cwhmm.setStates(states);
			
			// get score
			double score = cwhmm.Score(features);
			System.out.println(words.get(i) + " : " + score);
		}
	}
	
	public void test(ArrayList<String> words, Matrix feats, int numFeats) throws IOException{
		String path = "an4/models/ph_cont/";
		initialize(numFeats, path);
		score(words, feats, numFeats);
	}
	

}
