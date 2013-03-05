package models;

public class Gaussian {
	
	double[] mean;
	double[] covariance;
	double weight;
	int N;
	double determinant;
	
	public Gaussian(double[] mu, double[] covar, int nFeats, double frac){
		N = nFeats;
		weight = frac;
		determinant = 1.0;
		mean = new double [nFeats];
		covariance = new double [nFeats];
		for (int i = 0; i < nFeats; i++){
			mean[i] = mu[i];
			covariance[i] = covar[i]; //may need to make this matrix
			determinant *= covar[i];
		}
	}

	public Gaussian(int nFeats){
		N = nFeats;
		mean = new double [nFeats];
		covariance = new double [nFeats];
	}
	
	public void setParams(double[] mu, double[] covar, double frac){
		determinant = 1.0;
		weight = frac;
		for (int i = 0; i < N; i++){
			mean[i] = mu[i];
			covariance[i] = covar[i]; //may need to make this matrix
			determinant *= covar[i];
		}
		
	}
	
	public double[] getMeans(){
		return mean;
	}
	
	public double[] getCovars(){
		return covariance;
	}
	
	public double getWeight(){
		return weight;
	}
	
	public double getDeterminant(){
		return determinant;
	}

}
