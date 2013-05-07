package models.phoneme;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import speech.audio.feat.MFCC;
import speech.audio.utils.IO;

import comirva.audio.util.math.Matrix;

public class Runner {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		String phfi = "an4/etc/an4.phone"; // list of phonemes
		String dicfi = "an4/etc/an4.dic"; // word -> phoneme dic
		String trainfi = "an4/etc/an4_train.transcription"; // transcription file
		String isofi = "an4/etc/an4_iso.transcription";
		String trainpath = "an4/wav/train/"; // path to audio files
		String isopath = "an4/wav/isow/";
		String isomodpath = "an4/models/ph_isow/";
		String testpath = "an4/wav/test/";
		String testfi = "an4/etc/an4_test.transcription";
		
		//TrainHmm(phfi, dicfi, isofi, isopath, "iso");
		//TrainHmm(phfi, dicfi, trainfi, trainpath, "con");
		TestHmm(phfi, dicfi);
		//TestHmmFull(phfi, dicfi, testpath, testfi);
	}
	
	private static void TestHmm(String phfi, String dicfi) throws IOException{
		
		String aufi = "an4/wav/test/fcaw/an408-fcaw-b.wav"; //B A O Z FIVE THREE
		String [] cands = {"W O O D", "J A N E T", "B A O Z FIVE THREE"};
		
		//String aufi = "an4/wav/isow/FEBRUARY.wav";
		//String [] cands = {"FEBRUARY", "FEBRUARY SIXTH", "MAY", "MARCH"};
		
		ArrayList<String> phDic = new ArrayList<String>();
		HashMap<String, String> dic = new HashMap<String, String>();
		ArrayList<String> words = new ArrayList<String>(cands.length);
		MFCC featExtractor = new MFCC(16000, 50, 7000, 40);
		int numFeats = 39;
		String line;
		
		// phoneme list
		BufferedReader phr = new BufferedReader(new FileReader(phfi));
		while ((line = phr.readLine()) != null) {
		    phDic.add(line.trim());
		}
		phr.close();
		
		// dictionary
		BufferedReader dicr = new BufferedReader(new FileReader(dicfi));
		while ((line = dicr.readLine()) != null) {
			String [] ll = line.split("\\s", 2);
			dic.put(ll[0].trim(), ll[1].trim());
		}
		dicr.close();
		
		for (String s: cands){
			String word = "SIL ";
			for(String w : s.split("\\s")){
				word += dic.get(w) + " ";
			}
			words.add(word+"SIL");
		}
		
		
		double [] audioData = IO.read(aufi); // wav file
		double[][] featVectors = featExtractor.extractAll39Features(audioData);
		Matrix m = new Matrix(featVectors);
		
		PhonemeHmmTester testhmm = new PhonemeHmmTester(phDic);
		int best = testhmm.testOne(words, m, numFeats);
		System.out.println("Best Scoring Candidate: " + cands[best]);
		
	}
	
	private static void TrainHmm(String phfi, String dicfi, String datafi, String path, String type) throws IOException{
		ArrayList<String> phDic = new ArrayList<String>();
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<Matrix> features = new ArrayList<Matrix>();
		HashMap<String, String> dic = new HashMap<String, String>();
		MFCC featExtractor = new MFCC(16000, 50, 7000, 40);
		int numFeats = 39;
		
		String line;
		
		// phoneme list
		BufferedReader phr = new BufferedReader(new FileReader(phfi));
		while ((line = phr.readLine()) != null) {
		    phDic.add(line.trim());
		}
		phr.close();
		
		// dictionary
		BufferedReader dicr = new BufferedReader(new FileReader(dicfi));
		while ((line = dicr.readLine()) != null) {
			String [] ll = line.split("\\s", 2);
			dic.put(ll[0].trim(), ll[1].trim());
		}
		dicr.close();
		
		// training samples list
		BufferedReader dtr = new BufferedReader(new FileReader(datafi));
		String wfi = "";
		while ((line = dtr.readLine()) != null) {
			String[] ll = line.split("\\(");
			String id = ll[1].replace(")", "").trim();
			double[] audioData;
			if (type.equals("con")){
				audioData = IO.read(path+id.split("-")[1]+"/"+id+".wav"); // wav file
			}
			else{
				audioData = IO.read(path+id+".wav"); // wav file
			}
			double[][] featVectors = featExtractor.extractAll39Features(audioData);
			Matrix m = new Matrix(featVectors);
			String word = "SIL ";
			for(String w : ll[0].split("\\s")){
				word += dic.get(w) + " ";
			}
			//System.out.println(word);
			features.add(m);
			words.add(word+"SIL");
		}
		dtr.close();
		
		// train phoneme HMM from isolated words
		if (type.equals("iso")){ 
			PhonemeHmmIW iwhmm = new PhonemeHmmIW(phDic);
			iwhmm.train(words, features, numFeats);
		}
		else{ // from continuous data
			PhonemeHmmCW cwhmm = new PhonemeHmmCW(phDic);
			cwhmm.train(words, features, numFeats);
		}
	}
	
	private static void TestHmmFull(String phfi, String dicfi, String testpath, String testfi) throws IOException{
		
		ArrayList<String> phDic = new ArrayList<String>();
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<String> ids = new ArrayList<String>();
		ArrayList<Matrix> features = new ArrayList<Matrix>();
		HashMap<String, String> dic = new HashMap<String, String>();
		
		MFCC featExtractor = new MFCC(16000, 50, 7000, 40);
		int numFeats = 39;
		String line;
		
		// phoneme list
		BufferedReader phr = new BufferedReader(new FileReader(phfi));
		while ((line = phr.readLine()) != null) {
		    phDic.add(line.trim());
		}
		phr.close();
		
		// dictionary
		BufferedReader dicr = new BufferedReader(new FileReader(dicfi));
		while ((line = dicr.readLine()) != null) {
			String [] ll = line.split("\\s", 2);
			dic.put(ll[0].trim(), ll[1].trim());
		}
		dicr.close();
		
		// test transcript
		BufferedReader dtr = new BufferedReader(new FileReader(testfi));
		String wfi = "";
		while ((line = dtr.readLine()) != null) {
			String[] ll = line.split("\\(");
			String id = ll[1].replace(")", "").trim();
			ids.add(id);
			double[] audioData;
			audioData = IO.read(testpath+id.split("-")[1]+"/"+id+".wav"); // wav file
			double[][] featVectors = featExtractor.extractAll39Features(audioData);
			Matrix m = new Matrix(featVectors);
			String word = "SIL ";
			for(String w : ll[0].split("\\s")){
				word += dic.get(w) + " ";
			}
			//System.out.println(word);
			features.add(m);
			words.add(word+"SIL");
		}
		dtr.close();
		
		PhonemeHmmTester testhmm = new PhonemeHmmTester(phDic);
		testhmm.test(words, features, ids, numFeats);
		
	}

}
