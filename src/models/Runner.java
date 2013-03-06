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
		
		
		String path = "recordings/digits/";//args[0]; //"recordings/digits/"
		String fileprefix = "template_";//args[1]; //"template_"
		int numTemplates = 5;//Integer.parseInt(args[2]); //5
		MFCC featExtractor = new MFCC(16000, 50, 7000, 40);
		String [] digits = {"0"};//, "2", "4"};
		ArrayList<HMMKmeans> models = new ArrayList<HMMKmeans>(digits.length);
		
		for(String s: digits){
			ArrayList<Matrix> templates = new ArrayList<Matrix>();
			for(int i=1; i<=numTemplates; i++){
				double[] audioData = IO.read(path+s+"/"+fileprefix+"-"+i+".wav"); // wav file
				double[][] featVectors = featExtractor.extractAll39Features(audioData);
				Matrix m = new Matrix(featVectors);
				templates.add(m);
			}
			HMMKmeans segmenter = new HMMKmeans(5, templates.get(0).getColumnDimension());
			segmenter.learnClusters(templates);
			models.add(segmenter);
		}
		
		//Testing
		int correct = 0;
		fileprefix = "test_";
		for (String s : digits){
			System.out.println("Testing digit "+s);
			for(int i=1; i<=numTemplates; i++){
				System.out.print("\t Sample "+ i + " : ");
				double maxProb = Double.NEGATIVE_INFINITY;
				String pred = "";
				double[] audioData = IO.read(path+s+"/"+fileprefix+"-"+i+".wav"); // wav file
				double[][] featVectors = featExtractor.extractAll39Features(audioData);
				Matrix m = new Matrix(featVectors);
				for (int k=0; k<models.size(); k++){
					double prob = models.get(0).getSampleProb(m);
					System.out.println(prob);
					if (prob > maxProb){
						maxProb = prob;
						pred = digits[k];
					}
				}
				System.out.print(pred);
				System.out.println();
				if (pred.equals(s)){
					correct += 1;
				}
			}
			System.out.println();
		}
		double acc = (double) correct/(numTemplates*digits.length);
		System.out.println("Total Accuracy = " + acc);
	
  }
	
}
