package models;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LM {
	
	private static Map conditionalProbs = new HashMap<String,Double>();
	private static Map backoffProbs = new HashMap<String,Double>();
	
	public static void loadLM(String filename) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = br.readLine()) != null) 
		{
			if (!line.contains("\t"))
				continue;
			
			String[] splits = line.split("\\t");
			conditionalProbs.put(splits[1], Double.parseDouble(splits[0]));
			if (splits.length==3)
				backoffProbs.put(splits[1], Double.parseDouble(splits[2]));
		}
		br.close();
	}
	
	public static Double probability(String context, String word)
	{
		context = context.trim();
		word = word.trim();		
		
		if (context.length()==0)
		{
			return (Double) conditionalProbs.get(word); 
		}
		
		String key = context+" "+word;
		int wc = key.split(" ").length;
		Double prob;
		
		if(wc==2)
		{
			prob = (Double) conditionalProbs.get(key);
			if (prob==null)
				return (Double) conditionalProbs.get(word)+ (Double) backoffProbs.get(context);
			else
				return prob;
			
		}
		else 		//if(wc==3)
		{
			prob = (Double) conditionalProbs.get(key);
			if (prob==null)
			{
				String[] contextWords = context.split(" ");
				String wA = contextWords[0];
				String wB = contextWords[1];
				
				Double contextBackoff = (Double) backoffProbs.get(context);
				if (contextBackoff!=null)
					return probability(wB, word) + contextBackoff;
				else
					return probability(wB, word) + probability(wA, wB);
					
			}
			else
				return prob;
		}
		
	}
	
	public static void showLM()
	{
		Set<String> keys = conditionalProbs.keySet();
		for (String key : keys)
			System.out.println(key+"\t"+conditionalProbs.get(key)+"\t"+backoffProbs.get(key));
	}
	
	public static void testLM()
	{
		String[] words = {"A","AND","APOSTROPHE","EIGHT","F","FIFTY","L","Q"};
		
		for(String word : words)
			System.out.println(word+"\t"+probability("", word));
				
		for(String wA : words)
			for(String w: words) 
			{
				String context = wA;
				System.out.println(context+" "+w+"\t"+probability(context, w));
			}
		
		for(String wA : words)
			for(String wB : words)
				for(String w: words) 
				{
					String context = wA+" "+wB;
					System.out.println(context+" "+w+"\t"+probability(context, w));
				}
	}
	
	public static void main(String args[]) throws IOException
	{
		loadLM("data/an4.trigramlm");
		showLM();
		testLM();
		
	}

	public static Double getProbability(String ctxt, String word) {
		
		String[] prevWords = ctxt.split(" ");
		String context = "";
		for(int i=prevWords.length-1; i>0 && i>prevWords.length-4;i--)
			context=prevWords[i]+" "+context;
		context=context.trim();
		//System.out.println(probability(context, word));
		//return Math.log10(Math.exp(5* Math.log(Math.pow(probability(context, word),10))));
		return probability(context, word);
	}

	public static Double getDummyProbability(String context, String word) {
		// TODO Auto-generated method stub
		return 0.0;
	}

}
