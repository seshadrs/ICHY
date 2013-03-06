package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import comirva.audio.util.math.Matrix;

import speech.audio.feat.MFCC;
import speech.audio.utils.IO;
import models.HMMKmeans;

public class Runner {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	public static void main(String[] args) throws IllegalArgumentException, IOException {
		
		ArrayList<Matrix> templates = new ArrayList<Matrix>();
		String path = args[0];
		int numTemplates = Integer.parseInt(args[1]);
		MFCC featExtractor = new MFCC(16000, 50, 7000, 40);
		
		for(int i=1; i<=numTemplates; i++){
			double[] audioData = IO.read(path+"-"+i+".wav"); // wav file
			double[][] featVectors = featExtractor.extractFeatures(audioData);
			Matrix m = new Matrix(featVectors);
			templates.add(m);
		}
		
		HMMKmeans segmenter = new HMMKmeans(5, templates.get(0).getColumnDimension());
		segmenter.learnClusters(templates);
		double prob = segmenter.getSampleProb(templates.get(0));
		
		System.out.println(prob);
		
	}

}
