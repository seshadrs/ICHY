package models.phoneme;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import comirva.audio.util.math.Matrix;

public class HMM {
	
	protected Gaussian [] states;
	protected double [][] transitions;
	protected int numStates;
	protected int numFeats;
	protected double endProb;
	//protected int totalPoints;
	
	public HMM(int numStates, int numFeats){
		this.numStates = numStates;
		transitions = new double [numStates][numStates];
		for (int i = 0; i<numStates; i++){
			for (int j=0; j<numStates; j++){
				transitions[i][j] = 0;
			}
		}
		//totalPoints = 0;
		this.numFeats = numFeats;
		states = new Gaussian [this.numStates];
		for (int i=0; i<this.numStates; i++){
			states[i] = new Gaussian(numFeats);
		}
		endProb = 0.0;
	}
	
	public HMM(int numStates){
		this.numStates = numStates;
		transitions = new double [numStates][numStates];
		for (int i = 0; i<numStates; i++){
			for (int j=0; j<numStates; j++){
				transitions[i][j] = 0;
			}
		}
		//totalPoints = 0;
		numFeats = -1;
	}
	
	public void setFeats(int numFeats){
		this.numFeats = numFeats;
		states = new Gaussian [this.numStates];
		for (int i=0; i<this.numStates; i++){
			states[i] = new Gaussian(numFeats);
		}
	}
	
	public Gaussian getState(int state){
		return states[state];
	}
	
	public Gaussian[] getStates(){
		return states;
	}
	
	public double[][] getTransitions(){
		return transitions;
	}
	
	public double getEndProb(){
		return endProb;
	}
	
	public void setTransitions(double [][] trans){
		transitions = trans;
	}
	
	public void setStates(Gaussian [] states){
		this.states = states;
	}
	
/*	public void updatePart(int state, int next, int wid, int fid){
		if (numFeats == -1){
			System.out.println("Not initialized Gaussians!!!");
			return;
		}
		transitions[state][next] += 1;
		totalPoints ++;
	}*/
	
	public void updateParams(int state, ArrayList<String>segs, ArrayList<Matrix> feats, HashMap<Integer, Integer>trans){
		// Gaussian Params
		double[]means = new double[numFeats];
		double[]covars = new double[numFeats];
		int wid = 0;
		int fid = 0;
		int totp = 0;
		for (String s: segs){
			wid = Integer.parseInt(s.split(";")[0]);
			fid = Integer.parseInt(s.split(";")[1]);
			for(int idx=0;idx<numFeats;idx++){
				means[idx] = feats.get(wid).get(fid, idx);
				
			}
			totp++;
		}
		for (int i=0;i<numFeats;i++){
			means[i] = means[i]/totp;
		}
		for (String s: segs){
			wid = Integer.parseInt(s.split(";")[0]);
			fid = Integer.parseInt(s.split(";")[1]);
			for(int idx=0;idx<numFeats;idx++){
				covars[idx] += Math.pow(((feats.get(wid).get(fid, idx))-means[idx]),2);
			}
		}
		for(int i=0;i<numFeats;i++){
			covars[i] = covars[i]/totp;
		}
		states[state].setParams(means, covars);
		
		// HMM Params
		int totra = 0;
		for(int sto : trans.keySet()){
			if (sto == 3)
				endProb = trans.get(sto);
			else
				transitions[state][sto] = trans.get(sto);
			totra += trans.get(sto);
		}
		for (int i=0; i<transitions[state].length;i++){
			transitions[state][i] = transitions[state][i]/totra; 
		}
		endProb = endProb/totra;
		//transitions[numStates-1][numStates-1] = 0.5;
	}
	
	public ArrayList<Integer> Viterbi (Matrix m){
		ArrayList<Integer> seq = new ArrayList<Integer>(m.getRowDimension());
		double [][] trellis = new double[m.getRowDimension()][numStates];
		int [][] backp = new int [m.getRowDimension()][numStates];
		// at t=0, in state 0
		trellis[0][0] = Math.log10(getEmmitProb(m, 0, states[0]));
		//System.out.println(trellis[0][0]);
		for (int i=1; i<trellis[0].length;i++){
			trellis[0][i] = 0;
		}
		backp[0][0] = 0;
		int bestj = 0;
		double bestp = 0;
		double cp = 0;
		double ep = 0;
		for(int i=1;i<trellis.length;i++){
			for (int j=0;j<trellis[0].length;j++){
				ep = getEmmitProb(m, i, states[j]);
				if (ep == 0)
					ep = 0;
				else
					ep = Math.log10(ep);
				//System.out.println("ep: " + ep);
				bestj = j;
				bestp = trellis[i-1][j]+Math.log10(transitions[j][j])+ep;
				if(j != 0){
					cp = trellis[i-1][j-1]+Math.log10(transitions[j-1][j])+ep;
					if (cp > bestp){
						bestp = cp;
						bestj = j-1;
					}	
				}
				trellis[i][j] = bestp;
				backp[i][j] = bestj;
				//System.out.println("best: " + bestp);
			}
		}
		//System.out.println("1,0: "+trellis[1][0]);
		int sid = backp[0].length-1;
		seq.add(0, sid);
		for (int i=backp.length-2;i>=0;i--){
			sid = backp[i+1][sid];
			seq.add(0, sid);
		}
		//System.out.println("Path scr: "+trellis[trellis.length-1][trellis[0].length-1]);
		return seq;
	}
	
	public double Score (Matrix m){
		
		double [][] trellis = new double[m.getRowDimension()][numStates];
		double bestp = 0;
		double cp = 0;
		double ep = 0;
		
		// at t=0, in state 0
		ep = getEmmitProb(m, 0, states[0]);
		if (ep != 0)
			trellis[0][0] = Math.log10(ep);
		else
			trellis[0][0] = 0;
		//System.out.println(trellis[0][0]);
		for (int i=1; i<trellis[0].length;i++){
			trellis[0][i] = 0;
		}
		
		for(int i=1;i<trellis.length;i++){
			for (int j=0;j<trellis[0].length;j++){
				ep = getEmmitProb(m, i, states[j]);
				if (ep == 0)
					ep = 0;
				else
					ep = Math.log10(ep);
				//System.out.println("ep: " + ep);
				if (transitions[j][j] != 0)
					bestp = trellis[i-1][j]+Math.log10(transitions[j][j])+ep;
				else{
					bestp = Double.NEGATIVE_INFINITY;
					//System.out.println("Trans is 0");
				}
				/*if (bestp == Double.NEGATIVE_INFINITY)
					bestp = 0;*/
				if(j != 0){
					if (transitions[j][j] != 0)
						cp = trellis[i-1][j-1]+Math.log10(transitions[j-1][j])+ep;
					else{
						cp = Double.NEGATIVE_INFINITY;
						//System.out.println("Trans is 0");
					}
					if (cp > bestp){
						bestp = cp;
					}
					/*if (cp == Double.NEGATIVE_INFINITY)
						cp = 0;
					bestp += cp;*/
				}
				trellis[i][j] = bestp;
				//System.out.println("best: " + bestp);
			}
		}
		double res = trellis[trellis.length-1][trellis[0].length-1];
		//double res = trellis[15][11];
		return res;
	}
	
	
	private double getEmmitProb(Matrix m, int row, Gaussian clus){
		double dist = 1.0;
		double sumi = 0;
		for (int i = 0; i < numFeats; i++){
			sumi = (Math.pow((m.get(row, i) - clus.getMeans()[i]), 2)/(clus.getCovars()[i]));
			sumi = Math.exp(-0.5*sumi)*Math.pow(2*3.14*clus.getCovars()[i], -0.5);
			dist *= sumi;
		}
		return dist;
	}
	
	public void prettyPrint(String file) throws IOException{
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.write(endProb+"\n");
		bw.write(";;;;\n");
		for (int i=0;i<transitions.length;i++){
			for(int j=0; j<transitions.length;j++){
				bw.write(transitions[i][j] + "\t");
			}
			bw.write("\n");
		}
		bw.write(";;;;\n");
		for(Gaussian s: states){
			for(double d: s.getMeans()){
				bw.write(d+"\t");
			}
			bw.write("\n");
			for(double d: s.getCovars()){
				bw.write(d+"\t");
			}
			bw.write("\n");
			bw.write(s.getDeterminant()+"\n");
			bw.write(";;;;\n");
		}
		bw.close();
	}
	
	public void loadModel(String file) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		int sid = -2;
		int i=0;
		int j=0;
		endProb = Double.parseDouble(br.readLine().trim());
		while((line=br.readLine()) != null){
			if (line.contains(";;;")){
				sid ++;
				continue;
			}
			if (sid<0){
				for(String tv : line.split("\\t")){
					transitions[i][j] = Double.parseDouble(tv);
					j++;
				}
				i++;
				j=0;
			}
			else{
				double [] mean = new double[numFeats];
				int k=0;
				for(String tv : line.split("\\t")){
					tv = tv.trim();
					mean[k] = Double.parseDouble(tv);
					k++;
				}
				line = br.readLine();
				int k1=0;
				double [] covars = new double[numFeats];
				for(String tv : line.split("\\t")){
					tv = tv.trim();
					covars[k1] = Double.parseDouble(tv);
					k1++;
				}
				double det = Double.parseDouble(br.readLine().trim());
				states[sid].setParams(mean, covars, det);
			}
			
		}
	}
	
	
	public static double getEmissionProb(Matrix m, int row, Gaussian clus){
		double dist = 1.0;
		double sumi = 0;
		for (int i = 0; i < 39; i++){
			sumi = (Math.pow((m.get(row, i) - clus.getMeans()[i]), 2)/(clus.getCovars()[i]));
			sumi = Math.exp(-0.5*sumi)*Math.pow(2*3.14*clus.getCovars()[i], -0.5);
			dist *= sumi;
		}
		return dist;
	}
	
	
	
}
