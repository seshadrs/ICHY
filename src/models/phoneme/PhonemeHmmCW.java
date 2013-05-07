package models.phoneme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import comirva.audio.util.math.Matrix;

public class PhonemeHmmCW {

	private int K = 3;
	protected HashMap<String, HMM> phHmms;
	private HashMap<String, ArrayList<String>> nsegs;
	private HashMap<String, HashMap<Integer,Integer>> ntrans;
	
	
	//****** Constructor *********//
	public PhonemeHmmCW(ArrayList<String> phDic){
		phHmms = new HashMap<String, HMM>(phDic.size());
		// create an HMM with 3 states for each phoneme
		for (String ph:phDic){
			phHmms.put(ph,  new HMM(K));
		}
	}
	
	
	//****** Train phoneme models from continuous words *********//
	
	private void initialize(int numFeats, String path) throws IOException{
		System.out.println("In initialize..");
		
		// initialization of HMM Gaussians with phoneme models trained on IW
		for (String mm: phHmms.keySet()){
			phHmms.get(mm).setFeats(numFeats);
			phHmms.get(mm).loadModel(path+mm+".model");
		}
		
	}
	
	// compose an HMM from the transcript
	// do Viterbi throught the HMM to get state sequence
	// use the state sequence to update the appropriate phonemes and states
	private void segment(ArrayList<String>words, ArrayList<Matrix>features, int numFeats){
		System.out.println("In segment..");
		nsegs = new HashMap<String, ArrayList<String>>();
		ntrans = new HashMap<String, HashMap<Integer,Integer>>();
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
					transitions[stateId-1][stateId] = 0.3;
				}*/
				double [][] trans = phHmms.get(phoneme).getTransitions();
				for(int t1=0;t1<trans.length;t1++){
					for(int t2=0;t2<trans.length;t2++){
						transitions[stateId+t1][stateId+t2] = trans[t1][t2];
					}	
				}
				sit = 0;
				Gaussian [] curr_st = phHmms.get(phoneme).getStates();
				for(int curr_i=0; curr_i<curr_st.length;curr_i++){
					states[stateId] = curr_st[curr_i];
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
			
			// store new segments
			ArrayList<Integer> nseq = cwhmm.Viterbi(features.get(i));
			String sta;
			String psta = map.get(nseq.get(0));
			nsegs.put(psta, new ArrayList<String>());
			nsegs.get(psta).add(i+";"+0);
			for(int is=1; is<nseq.size(); is++){
				sta = map.get(nseq.get(is));
				if (!nsegs.containsKey(sta)){
					nsegs.put(sta, new ArrayList<String>());
				}
				nsegs.get(sta).add(i+";"+is);
				if(!ntrans.containsKey(psta)){
					ntrans.put(psta, new HashMap<Integer,Integer>());
					ntrans.get(psta).put(0, 0);
					ntrans.get(psta).put(1, 0);
					ntrans.get(psta).put(2, 0);
					ntrans.get(psta).put(3, 0);
				}
				if (psta.split(";")[0].equals(sta.split(";")[0]))
					ntrans.get(psta).put(Integer.parseInt(sta.split(";")[1]), ntrans.get(psta).get(Integer.parseInt(sta.split(";")[1]))+1);
				else
					ntrans.get(psta).put(3, ntrans.get(psta).get(3)+1);
				psta = sta;
			}
		}
	}
	
	
	private void update(ArrayList<Matrix>features){
		
		System.out.println("In update..");
		// update
		for (String ph : nsegs.keySet()){
			int st = Integer.parseInt(ph.split(";")[1]); 
			phHmms.get(ph.split(";")[0]).updateParams(st, nsegs.get(ph), features, ntrans.get(ph));
		}
	}
	
	public void train(ArrayList<String>words, ArrayList<Matrix>features, int numFeats) throws IOException{
		String path = "an4/models/ph_isow/";
		initialize(numFeats, path);
		int numits = 0;
		while(numits < 50){
			switch(numits){
				case 10:
					storeModels("an4/models/ph_cont10/");
					System.out.println("Done with 10");
					break;
				case 30:
					storeModels("an4/models/ph_cont30/");
					System.out.println("Done with 30");
					break;
				case 40:
					storeModels("an4/models/ph_cont40/");
					System.out.println("Done with 40");
					break;
				default:
					break;	
			}
			
			segment(words, features, numFeats);
			update(features);
			numits++;
		}
		storeModels("an4/models/ph_cont50/");
	}
	
	private void storeModels(String path) throws IOException{
		for (String ph:phHmms.keySet()){
			phHmms.get(ph).prettyPrint(path+ph+".model");
		}
	}
}
