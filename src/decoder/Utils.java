package decoder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import models.phoneme.HMM;

public class Utils {

	public static HashMap<String,String> loadDict(String filename) throws IOException
	{
		Map dic = new HashMap<String,String>();// dictionary
		
		BufferedReader dicr = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = dicr.readLine()) != null) {
			String [] ll = line.split("\\s", 2);
			if (!ll[0].trim().contains("("))
				dic.put(ll[0].trim(), ll[1].trim());
		}
		dicr.close();
		return (HashMap<String, String>) dic;
	}
	
	public static HashMap<String,HMM> loadModels(ArrayList<String> phones, String path) throws IOException
	{
		 	HashMap<String, HMM> phHmms;
		 	int K=3, numFeats=39;
			phHmms = new HashMap<String, HMM>(phones.size());
			// create an HMM with 3 states for each phoneme
			for (String ph:phones){
				phHmms.put(ph,  new HMM(K,39));
			}
		
			//System.out.println("In initialize..");
			
			// initialization of HMM Gaussians with phoneme models trained on IW
			for (String mm: phHmms.keySet()){
				phHmms.get(mm).setFeats(numFeats);
				//System.out.println(path+mm+".model");
				phHmms.get(mm).loadModel(path+mm+".model");
			}
			
			return phHmms;
	}
	
	public static ArrayList<String> loadPhones(String filename) throws IOException
	{
		ArrayList<String> phones = new ArrayList<String>();
		BufferedReader dicr = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = dicr.readLine()) != null) {
			 phones.add(line.trim());
		}
		dicr.close();
		
		return phones;
	}
	
}
