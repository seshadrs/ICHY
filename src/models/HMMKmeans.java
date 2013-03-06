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
		System.out.println("In constructor...");
		this.features = new ArrayList<Matrix>();
		this.hmm = new HMM(numStates, numFeats);
	}
	
	private void initialize(ArrayList<Matrix> feats){
		int split = 0;
		int start = 0;
		int end = 0;
		totalPoints = 0;
		System.out.println("In initialize...");
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

	public void learnClusters(ArrayList<Matrix> feats){
	
		// initialize
		initialize(feats);
		double prevErr = 0;
		//double err = meanSampleProb();
		
		/*
		// recurse
		while(Math.abs((err - prevErr)) > convergenceThresh){
			prevErr = err;
			hmm.update(features);
			err = meanSampleProb();
		}*/
	}
	
	public double getSampleProb(Matrix feats){
		return hmm.forwardProb(feats);
	}
}
