package models;

import java.util.ArrayList;

import models.Gaussian;
import comirva.audio.util.math.Matrix;

public class HMM {
	
	protected Gaussian [] states;
	protected double [] weights;
	private double [] initials;
	private double [] finals;
	private double [][] transitions;
	private double [] denoms;
	private double [][] ascalers;
	private double [][] bscalers;
	protected ArrayList<Matrix> alphas;
	protected ArrayList<Matrix> betas;
	protected ArrayList<Matrix> gammas;
	protected ArrayList<ArrayList<Matrix>> tgammas;
	protected int numStates;
	protected int numFeats;
	protected int totalPoints;
	
	public HMM(int numStates, int numFeats){
		this.numStates = numStates;
		this.numFeats = numFeats;
		states = new Gaussian [this.numStates];
		for (int i=0; i<this.numStates; i++){
			states[i] = new Gaussian(numFeats);
		}
		initials = new double[numStates];
		finals = new double[numStates];
		weights = new double[numStates];
		transitions = new double [numStates][numStates];
		denoms = new double[numStates];
	}
	
	private void calculateHMMParams(int [][] segments){
		initials[0] = 1;
		finals[numStates-1] = 1;
		for(int i=0; i<segments.length; i++){
			int prev = segments[i][0];
			for (int j=1; j<segments[0].length;j++){
				int curr = segments[i][j];
				transitions[prev][curr] += 1;
				weights[curr] += 1;
				prev = curr;
			}
		}
		for(int i=0; i<numStates;i++){
			weights[i] = weights[i]/totalPoints;
			for(int j=0; j<numStates;j++){
				transitions[i][j] = (double)transitions[i][j]/(totalPoints-segments.length);
			}
		}
		
	}
	
	private void updateTransitions(int [][] segments){
		
		for(int i=0; i<segments.length; i++){
			int prev = segments[i][0];
			for (int j=1; j<segments[0].length;j++){
				int curr = segments[i][j];
				transitions[prev][curr] += 1;
				weights[curr] += 1;
				prev = curr;
			}
		}
		for(int i=0; i<numStates;i++){
			weights[i] = weights[i]/totalPoints;
			for(int j=0; j<numStates;j++){
				transitions[i][j] = (double)transitions[i][j]/(totalPoints-segments.length);
			}
		}
	}
	
	private void updateHMMParams(){
		double sumi = 0;
		for (int s=0; s<numStates; s++){
			for(int r=0; r<numStates;r++){
				for (int m=0; m<tgammas.size();m++){
					ArrayList<Matrix> temp = tgammas.get(m);
					for(int t=0; t<temp.size();t++){
						sumi += temp.get(t).get(s, r);
					}
				}
				transitions[s][r] = sumi/denoms[s];
				sumi = 0;
			}
		}
	}
	
	private void calculateGaussianParams(ArrayList<Matrix> features){
		double [] means = new double [numFeats];
		double [] covars = new double [numFeats];
		for(int i=0; i<numFeats; i++){
			means[i] = 0;
			covars[i] = 0;
		}
		for(Matrix m: features){
			for (int i=0; i<m.getRowDimension(); i++){
				for(int j=0; j<numFeats; j++){
					means[j] += m.get(i, j);
				}
			}
		}
		
		for(int i=0;i<means.length;i++){
			means[i] = means[i]/totalPoints;
		}
		
		for(Matrix m: features){
			for (int i=0; i<m.getRowDimension(); i++){
				for(int j=0; j<numFeats; j++){
					covars[j] += Math.pow((m.get(i, j)-means[j]),2);
				}
			}
		}
		
		for(int i=0;i<covars.length;i++){
			covars[i] = covars[i]/totalPoints;
		}
		
		// Update Gaussians
		for (int i=0; i<numStates; i++){
			states[i].setParams(means, covars, weights[i]);
		}	
	}
	
	private void updateGaussianParams(ArrayList<Matrix> features){
		double [][] means = new double [numStates][numFeats];
		double [][] covars = new double [numStates][numFeats];
		
		for(int i=0; i<numStates; i++){
			for(int j=0; j<numFeats; j++){
				means[i][j] = 0;
				covars[i][j] = 0;
			}
		}
		
		for(int s=0; s<numStates;s++){
			double denom = 0;
			for (int m=0; m<features.size();m++){
				Matrix temp = features.get(m);
				Matrix gamma = gammas.get(m);
				for(int t=0; t<temp.getRowDimension();t++){
					denom += gamma.get(s, t);
					for (int k=0; k<numFeats; k++){
						means[s][k] += (gamma.get(s, t)*temp.get(t, k));
					}
				}
			}
			for (int k=0; k<numFeats; k++){
				means[s][k] = means[s][k]/denom;
			}
			denoms[s] = denom;
		}
		
		for(int s=0; s<numStates;s++){
			//double denom = 0;
			for (int m=0; m<features.size();m++){
				Matrix temp = features.get(m);
				Matrix gamma = gammas.get(m);
				for(int t=0; t<temp.getRowDimension();t++){
					//denom += gamma.get(s, t);
					for (int k=0; k<numFeats; k++){
						double tmp = Math.pow((temp.get(t, k)-means[s][k]), 2);
						covars[s][k] += (gamma.get(s, t)*tmp);
					}
				}
			}
			for (int k=0; k<numFeats; k++){
				covars[s][k] = covars[s][k]/denoms[s];
			}
		}
		
		// Update Gaussians
		for (int i=0; i<numStates; i++){
			states[i].setParams(means[i], covars[i], weights[i]);
		}	
		
	}
	
	private ArrayList calculateAlphas(Matrix template, boolean test){
		Matrix alph = new Matrix(numStates, template.getRowDimension());
		double [] scaler = new double [template.getRowDimension()]; 
		alph.set(0, 0, 1);
		scaler[0] = 1;
		int nan = 0; //tester
		int nonan = 0; //tester
		for (int i=1; i<numStates; i++){
			alph.set(i, 0, 0);
		}
		for(int t= 1;t < template.getRowDimension(); t++){
			scaler[t] = 0;
			Double [] vals = new Double [numFeats];
			for(int j =0; j<numFeats; j++){
				vals[j] = (Double) template.get(t, j);
			}
			for (int s=0; s< numStates; s++){
				double sum = 0;
				for (int r=0; r< numStates; r++){
					sum += (alph.get(r,t-1)*transitions[r][s]*getEmmitProb(vals, states[s]));
				}
				scaler[t] += sum; 
				if (sum >= 0){ //tester
					nonan ++;
				}else{ //tester
					nan ++;
				}
					
				alph.set(s, t, sum);
			}
			for (int s=0; s< numStates; s++){
				double org = alph.get(s, t);
				alph.set(s, t, org/scaler[t]);
			}
		}
		
		if (!test){
			alphas.add(alph.copy());
		}
		System.out.println("Nans\t" + nan); //tester
		System.out.println("Non Nans\t" + nonan); //tester
		ArrayList ret = new ArrayList(2);
		ret.add(alph);
		ret.add(scaler);
		return ret;
	}
	
	private void calculateBetas(Matrix template){
		Matrix beta = new Matrix(numStates, template.getRowDimension());
		double [] scaler = new double [template.getRowDimension()]; 
		beta.set(numStates-1, template.getRowDimension()-1, 1);
		scaler[template.getRowDimension()-1] = 1;
		for (int i=0; i<numStates-1; i++){
			beta.set(i, template.getRowDimension()-1, 0);
		}
		for(int t=template.getRowDimension()-2; t>=0 ;t--){
			scaler[t] = 0;
			Double [] vals = new Double [numFeats];
			for(int j =0; j<numFeats; j++){
				vals[j] = (Double) template.get(t+1, j);
			}
			for (int s=0; s< numStates; s++){
				double sum = 0;
				for (int r=0; r< numStates; r++){
					sum += (beta.get(r,t+1)*transitions[s][r]*getEmmitProb(vals, states[r]));
				}
				scaler[t] += sum; 
				beta.set(s, t, sum);
			}
			for (int s=0; s< numStates; s++){
				double org = beta.get(s, t);
				beta.set(s, t, org/scaler[t]);
			}
		}
		
		betas.add(beta.copy());
	}
	
	private void calculateGammas(ArrayList<Matrix> features){
		for(int i=0;i<alphas.size();i++){
			System.out.println("Alpha size: "+alphas.size());
			Matrix alph = alphas.get(i);
			Matrix beta = betas.get(i);
			int nFrames = alph.getColumnDimension();
			Matrix gamma = new Matrix(numStates, nFrames);
			ArrayList<Matrix> tgamma = new ArrayList<Matrix>(nFrames);
			for (int t=0; t< nFrames; t++){
				double denom = 0;
				for (int s=0; s<numStates; s++){
					denom += (alph.get(s, t)*beta.get(s, t));
				}
				for (int s=0; s<numStates; s++){
					gamma.set(s, t, (alph.get(s, t)*beta.get(s, t))/denom);
				}	
			}
			for (int t=0; t< nFrames-1; t++){
				Matrix sr = new Matrix(numStates, numStates);
				double tdenom = 0;
				Double [] vals = new Double [numFeats];
				for(int j =0; j<numFeats ; j++){
					vals[j] = (Double) features.get(i).get(t+1, j);
				}
				for (int s=0; s<numStates; s++){
					for (int r=0; r<numStates; r++){
						tdenom += (alph.get(s, t)*beta.get(r, t+1)*transitions[s][r]*getEmmitProb(vals, states[r]));
					}
				}	
				for (int s=0; s<numStates; s++){
					for (int r=0; r<numStates; r++){
						double srv = (alph.get(s, t)*beta.get(r, t+1)*transitions[s][r]*getEmmitProb(vals, states[r]));
						sr.set(s, r, srv);
					}
				}	
				tgamma.add(sr);
			}
			tgammas.add(tgamma);
			gammas.add(gamma.copy());
		}
	}
	
	private void clearAll(){
		for(int i=0; i<numStates;i++){
			initials[i] = 0;
			finals[i] = 0;
			weights[i] = 0;
			for(int j=0; j<numStates;j++){
				transitions[i][j] = 0;
			}
		}
	}
	
	public void initialize(ArrayList<Matrix> features, int [][] segments){
		alphas = new ArrayList<Matrix>(features.size());
		betas = new ArrayList<Matrix>(features.size());
		gammas = new ArrayList<Matrix>(features.size());
		tgammas = new ArrayList<ArrayList<Matrix>>(features.size());
		ascalers = new double [features.size()][];
		bscalers = new double [features.size()][];
		clearAll();
		calculateHMMParams(segments);
		calculateGaussianParams(features);
		update(features, segments);
	}
	
	public void update(ArrayList<Matrix> features, int [][] segments){
		alphas.clear();
		betas.clear();
		gammas.clear();
		tgammas.clear();
		for(int t=0; t<features.size(); t++){
			calculateAlphas(features.get(t), false);
			calculateBetas(features.get(t));
		}
		calculateGammas(features);
		//updateTransitions(segments);
		updateGaussianParams(features);
	}
	
	public double forwardProb(Matrix sample){
		double result = 0;
		ArrayList res = calculateAlphas(sample, true);
		Matrix m = (Matrix) res.get(0);
		double [] consts = (double []) res.get(1);
		for(int i=0; i< m.getRowDimension(); i++){
			result += m.get(i, m.getColumnDimension()-1);
		}
		double sumLogC = 1.0;
	    for (int k = 0; k < consts.length; k++) {
	        sumLogC += Math.log(consts[k]);
	    	//sumLogC *= consts[k];
	    	//System.out.println(consts[k]);
	    }

	    result = Math.log(result) - sumLogC;
	    //result = result*sumLogC;
		return result;
	}
	
	private double getEmmitProb(Double[] vals, Gaussian clus){
		double dist = 0;
		double sumi = 0;
		dist = Math.pow(2*3.14*clus.getDeterminant(), 0.5);
		dist = (double) 1.0/dist;
		for (int i = 0; i < numFeats; i++){
			sumi += (Math.pow((vals[i].doubleValue() - clus.getMeans()[i]), 2)/(clus.getCovars()[i]));
		}
		dist = dist * Math.exp(-0.5*sumi);
		return dist;
	}
	
}
