package models;

//input: vectors
//output: numStates number of gaussians, assignment of vectors to each state

// Algo:
//	create numStates number of Gaussians
//	segment all template file vectors into numStates
//	calculate gaussian params for each state
//	re assign vectors
//	continue to convergence

import java.util.ArrayList;
import java.util.Vector;

import models.Gaussian;
import comirva.audio.util.math.Matrix;

public class KMeans {
	
	private Gaussian[] states;
	private int numStates;
	private int numFeats;
	private double convergenceThresh = 0.005;
	private int totalPoints;
	private ArrayList<ArrayList<Double>> [] features;
	
	public KMeans(int numStates){
		System.out.println("In constructor...");
		this.states = new Gaussian [numStates];
		this.numStates = numStates;
	}
	
	public KMeans(String modelFile){
		// TODO: Read the state models from a file
		// 		 so we are ready to classify.
		
	}
	
	private void initialize(Vector<Matrix> feats){
		
		System.out.println("In initialize...");
		// iterate over each matrix (each template) and get the segment values 
		int split = 0;
		int start = 0;
		int end = 0;
		totalPoints = 0;
		for (Matrix m : feats){
			// assuming each row is a frame vector
			int nFrames = m.getRowDimension();
			int idx = 0;
			split = nFrames/numStates;
			end = split - 1;
			start = 0;
			while (idx < numStates){
				features[idx] = new ArrayList<ArrayList<Double>>();
				while (start <= end){
					ArrayList<Double> vals = new ArrayList<Double>(numFeats);
					for (int j=0; j<numFeats; j++){
						vals.add(m.get(start, j));
					}
					totalPoints += 1;
					start += 1;
					features[idx].add(vals);
				}
				end += Math.min(split, nFrames-1);
				idx += 1;
			}	
		}
		for (int i=0; i<numStates; i++){
			states[i] = new Gaussian(numFeats);
		}
				
	}

	private void update(){
		
		System.out.println("In update...");
		double [][] means = new double[numStates][numFeats];
		double [][] covars = new double[numStates][numFeats];
		int state;
		int feat;
		
		//initialize with 0s
		for (int i=0; i<numStates; i++){
			for (int j=0; j<numFeats; j++){
				means[i][j] = 0;
				covars[i][j] = 0;
			}
		}
		
		// iterate over each matrix (each template) and get the segment values 
		
		state = 0;
		feat = 0;
		
		for (ArrayList<ArrayList<Double>> m : features){			
			for (ArrayList<Double> vals : m){
				feat = 0;
				for (double val : vals){
					means[state][feat] += val;
					feat += 1;
				}
			}
			state += 1;
		}
		
		// calculate means
		
		for (int i=0; i<numStates; i++){
			double count = features[i].size();
			for(int k=0; k<numFeats; k++){
				means[i][k] = means[i][k]/count;
			}
		}
		
		// calculate covariances
		state = 0;
		feat = 0;
		
		for (ArrayList<ArrayList<Double>> m : features){			
			for (ArrayList<Double> vals : m){
				feat = 0;
				for (double val : vals){
					covars[state][feat] += Math.pow((val-means[state][feat]), 2);
					feat += 1;
				}
				
			}
			state += 1;
		}
		for (int i=0; i<numStates; i++){
			double count = features[i].size();
			for(int k=0; k<numFeats; k++){
				covars[i][k] = covars[i][k]/count;
			}
		}
		
		// Update the states
		for (int i=0; i<numStates; i++){
			states[i].setParams(means[i], covars[i], ((double) features[i].size()/totalPoints));
		}	
	
	}
	
	private void segment(){
		System.out.println("In segment...");
		ArrayList [] newSeg = new ArrayList[numStates];
		double minDist = Integer.MAX_VALUE;
		int minIdx = 0;
		for (int i=0; i<numStates; i++){
			newSeg[i] = new ArrayList<ArrayList<Double>>();
		}
		for(ArrayList<ArrayList<Double>> st : features){
			for (ArrayList<Double> vec : st){
				int idx = 0;
				minDist = Integer.MAX_VALUE;
				minIdx = 0;
				Double [] vals = new Double[numFeats];
				vec.toArray(vals);
				for (Gaussian state : states){
					double dist = getDistance(vals, state);
					if(dist < minDist){
						minDist = dist;
						minIdx = idx;
					}
					idx ++;
				}
				newSeg[minIdx].add(vec);
			}
		}
		for (int i=0; i<numStates; i++){
			features[i] = newSeg[i];
		}
	}
	
	private double getDistance(Double[] vals, Gaussian clus){
		double dist = 0;
		double sumi = 0;
		dist = -0.5 * Math.log(2*3.14*clus.getDeterminant());
		for (int i = 0; i < numFeats; i++){
			sumi += (Math.pow((vals[i].doubleValue() - clus.getMeans()[i]), 2)/(clus.getCovars()[i]));
		}
		dist = dist - 0.5*sumi - Math.log(clus.getWeight());
		return dist;
	}
	
	private double meanSegmentationError(){
		double error = 0;
		int idx = 0;
		for (ArrayList<ArrayList<Double>> state : features){
			Gaussian cluster = states[idx];
			for (ArrayList<Double> vec : state){
				Double [] vals = new Double[numFeats];
				vec.toArray(vals);
				error += getDistance(vals, cluster);
			}
			idx ++;
		}
		System.out.println(error/totalPoints);
		return error/totalPoints;
	}
	
	
	public void learnClusters(Vector<Matrix> feats){
		// setup and initialize
		this.numFeats = feats.get(0).getColumnDimension();
		this.features = new ArrayList[numStates];
		initialize(feats);
		update();
		segment();
		double prevErr = 0;
		double err = meanSegmentationError();
		
		// recurse
		while(Math.abs((err - prevErr)) > convergenceThresh){
			prevErr = err;
			update();
			segment();
			err = meanSegmentationError();
		}
	}
	
	public int [] segmentSample(Matrix sample){
		int [] classes = new int [sample.getRowDimension()];
		double minDist = 0;
		int minIdx = 0;
		for (int i=0; i<sample.getRowDimension(); i++){
			Double [] vals = new Double [numFeats];
			for(int j =0; j<numFeats; j++){
				vals[j] = (Double) sample.get(i, j);
			}
			int idx = 1;
			minDist = Integer.MAX_VALUE;
			minIdx = 0;
			for (Gaussian state : states){
				double dist = getDistance(vals, state);
				if(dist < minDist){
					minDist = dist;
					minIdx = idx;
				}
				idx ++;
			}
			classes[i] = minIdx;	
		}
		return classes;
	}
}
