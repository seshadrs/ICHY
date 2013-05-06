package models.phoneme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import comirva.audio.util.math.Matrix;

public class PhonemeHmmIW {

	private int K = 3;
	protected HashMap<String, HMM> phHmms;
	private HashMap<String, ArrayList<String>> nsegs;
	private HashMap<String, HashMap<Integer,Integer>> ntrans;
	
	
	//****** Constructor *********//
	public PhonemeHmmIW(ArrayList<String> phDic){
		phHmms = new HashMap<String, HMM>(phDic.size());
		// create an HMM with 3 states for each phoneme
		for (String ph:phDic){
			phHmms.put(ph,  new HMM(K));
		}
	}
	
	
	//****** Train phoneme models from isolated words *********//
	
	private void initialize(ArrayList<String>words, ArrayList<Matrix>features, int numFeats){
		System.out.println("In initialize..");
		int seglen = 0;
		//String [][] segments = new String[words.size()][];
		//HashMap<String, HashMap<Integer, ArrayList<Integer>>> segments = new HashMap<String, HashMap<Integer, ArrayList<Integer>>>();
		HashMap<String, ArrayList<String>> segments = new HashMap<String, ArrayList<String>>();
		HashMap<String, HashMap<Integer,Integer>> transitions = new HashMap<String, HashMap<Integer,Integer>>();
		
		// initialization of HMM Gaussians
		for (String mm: phHmms.keySet()){
			phHmms.get(mm).setFeats(numFeats);
		}
		
		//uniform segmentation
		int wid = 0;
		for (String w : words){
			//System.out.println("Doing word " + w);
			int iid = 0;
			int oid = 0;
			int pid = 0;
			int psid = 0;
			String cstate = "";
			String pstate = "";
			int ppsid = -1;
			int ppid = -1;
			String [] phonemes = w.split("\\s");
			//for (int ii=0;ii<phonemes.length;ii++)
			//	System.out.println(phonemes[ii]);
			//segments[wid] = new String [features.get(wid).getRowDimension()];
			//seglen = (segments[wid].length)/(phonemes.length * K);
			int nf = features.get(wid).getRowDimension();
			//System.out.println(nf);
			seglen = nf/(phonemes.length * K);
			while (oid < nf){
				if (iid > seglen){
					if(psid == 2){
						psid = 0;
						pid ++;
						//ppid = -1;
					}
					else{
						psid ++;
					}
					iid = 0;
				}
				cstate = phonemes[pid]+";"+psid;
				if (!segments.containsKey(cstate)){
					segments.put(cstate, new ArrayList<String>());
				}
				if (ppid != -1){
					pstate = phonemes[ppid] + ";" + ppsid;
					if(!transitions.containsKey(pstate)){
						transitions.put(pstate, new HashMap<Integer,Integer>());
						transitions.get(pstate).put(0, 0);
						transitions.get(pstate).put(1, 0);
						transitions.get(pstate).put(2, 0);
						transitions.get(pstate).put(3, 0);
					}
					if (ppid == pid)
						transitions.get(pstate).put(psid, (transitions.get(pstate).get(psid))+1);
					else
						transitions.get(pstate).put(3, (transitions.get(pstate).get(3))+1);
			    }
				ppid = pid;
				ppsid = psid;
				/*if (segments.get(cstate).containsKey(wid)){
					segments.get(cstate).get(wid).add(oid);
				}
				else{
					segments.get(cstate).put(wid, new ArrayList<Integer>());
					segments.get(cstate).get(wid).add(oid);
				}*/
				segments.get(cstate).add(wid+";"+oid);
				iid ++;
				oid ++;
			}
			wid ++;
		}
		
		// initialize
		for (String ph : segments.keySet()){
			//System.out.println("initializing phone " + ph);
			int st = Integer.parseInt(ph.split(";")[1]); 
			phHmms.get(ph.split(";")[0]).updateParams(st, segments.get(ph), features, transitions.get(ph));
		}
			
	}
	
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
		initialize(words, features, numFeats);
		int numits = 0;
		while(numits < 25){
			segment(words, features, numFeats);
			update(features);
			numits++;
		}
		storeModels();
	}
	
	private void storeModels() throws IOException{
		String path = "an4/models/ph_isow/";
		for (String ph:phHmms.keySet()){
			phHmms.get(ph).prettyPrint(path+ph+".model");
		}
	}
	
	
}
