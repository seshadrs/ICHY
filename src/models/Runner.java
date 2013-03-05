package models;

import java.io.IOException;
import java.util.Vector;

import comirva.audio.util.math.Matrix;

import speech.audio.feat.MFCC;
import speech.audio.utils.IO;
import models.KMeans;

public class Runner {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	public static void main(String[] args) throws IllegalArgumentException, IOException {
		
		Vector<Matrix> templates = new Vector<Matrix>();
		String path = args[0];
		int numTemplates = Integer.parseInt(args[1]);
		MFCC featExtractor = new MFCC(16000, 50, 7000, 40);
		int [][] trainer = new int [numTemplates][];
		
		for(int i=1; i<=numTemplates; i++){
			double[] audioData = IO.read(path+"-"+i+".wav"); // wav file
			double[][] featVectors = featExtractor.extractFeatures(audioData);
			Matrix m = new Matrix(featVectors);
			templates.add(m);
		}
		
		KMeans segmenter = new KMeans(5);
		segmenter.learnClusters(templates);
		
		int idx = 0;
		for (Matrix t: templates){
			trainer[idx] = new int [t.getRowDimension()];
			trainer[idx] = segmenter.segmentSample(t);
			idx ++;
		}
		
		// Print for testing
		
		/*for (int i=0; i<trainer.length; i++){
			for (int j=0; j< trainer[0].length; j++){
				System.out.print(trainer[i][j]);
				System.out.println("\t");
			}
			System.out.println();
		}*/
		
		// call HMM trainer
		
		//store hmm model
		
		
		
	}

}
