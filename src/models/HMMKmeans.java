package models;

import java.util.ArrayList;
import java.util.Vector;

import comirva.audio.util.math.Matrix;

import models.HMM;

public class HMMKmeans {
	
	private HMM hmm;
	private double convergenceThresh = 0.005;
	private int totalPoints;
	private ArrayList<Matrix> features;
	private int [][] segments;

	public HMMKmeans(String modelFile){
		// TODO: Read the state models from a file
		// 		 so we are ready to classify.
		
	}
	
	public HMMKmeans(int numStates, int numFeats){
		System.out.println("Training Digit...");
		this.features = new ArrayList<Matrix>();
		this.hmm = new HMM(numStates, numFeats);
	}
	
	private void initialize(ArrayList<Matrix> feats){
		int split = 0;
		int start = 0;
		int end = 0;
		totalPoints = 0;
		//System.out.println("In initialize...");
		segments = new int[feats.size()][];
		int oidx = 0;
		for(Matrix m : feats){
			features.add(m.copy());
			int nFrames = m.getRowDimension();
			segments[oidx] = new int [nFrames]; 
			int idx = 0;
			split = nFrames/hmm.numStates;
			end = split - 1;
			start = 0;
			while (idx < hmm.numStates){
				while (start <= end){
					segments[oidx][start] = idx;
					totalPoints += 1;
					start += 1;
				}
				end += Math.min(split, nFrames-1);
				idx += 1;
			}
			oidx ++;

		}
		hmm.totalPoints = totalPoints;
		hmm.initialize(features, segments);
	}
	
	private double meanSampleProb(){
		double prob = 0;
		for (int te=0; te<features.size();te++){
			prob += hmm.forwardProb(features.get(te));
		}
		return prob/features.size();
	}

	
	private void segment(){
		//System.out.println("In segment...");
		double minDist = Integer.MAX_VALUE;
		int minIdx = 0;
		int oidx = 0;
		for(Matrix m : features){
			segments[oidx][0] = 0;
			segments[oidx][m.getRowDimension()-1] = hmm.numStates-1;
			for (int i=1; i<m.getRowDimension()-1; i++){
				int idx = 0;
				minDist = Integer.MAX_VALUE;
				minIdx = 0;
				Double [] vals = new Double[hmm.numFeats];
				for (int j=0; j<hmm.numFeats; j++){
					vals[j] = m.get(i, j);
				}
				for (Gaussian state : hmm.states){
					double dist = getDistance(vals, state);
					if(dist < minDist){
						minDist = dist;
						minIdx = idx;
					}
					idx ++;
				}
				segments[oidx][i] = minIdx;
			}
		oidx ++;
		}
	}
	
	private double getDistance(Double[] vals, Gaussian clus){
		double dist = 0;
		double sumi = 0;
		dist = -0.5 * Math.log(2*3.14*clus.getDeterminant());
		for (int i = 0; i < hmm.numFeats; i++){
			sumi += (Math.pow((vals[i].doubleValue() - clus.getMeans()[i]), 2)/(clus.getCovars()[i]));
		}
		dist = dist - 0.5*sumi - Math.log(clus.getWeight());
		return dist;
	}
	
	public void learnClusters(ArrayList<Matrix> feats){
	
		// initialize
		initialize(feats);
		double prevErr = 0;
		double err = meanSampleProb();
		
		int iter = 0;
		// recurse
		while((Math.abs((err - prevErr)) > convergenceThresh) && (iter < 5)){
			//System.out.println("Prob is: " + err);
			prevErr = err;
			segment();
			hmm.update(features, segments);
			err = meanSampleProb();
			iter ++;
		}
		//System.out.println("Prob is: " + err);
	}
	
	public double getSampleProb(Matrix feats){
		return hmm.forwardProb(feats);
	}
}
