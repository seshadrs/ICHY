package speech.scripts;

import java.io.IOException;
import java.util.ArrayList;

import speech.audio.feat.MFCC;
import speech.audio.utils.IO;
import speech.lang.Template;
import speech.trellis.DTW;

public class DTWSingleTemplate {
	
	public static void runDTWWithSigleTemplates(String inputsString, String templatesString ) throws IllegalArgumentException, IOException
	{
		ArrayList<Template> templates = new ArrayList<Template>();
		ArrayList<Template> inputs = new ArrayList<Template>();
		
		MFCC featExtractor = new MFCC(16000, 50, 7000, 40);
		
		for(String templateFile : templatesString.split(","))
		{
			double[][] featVectorsLogMel = featExtractor.extractAll39Features(IO.read(templateFile));
			Template t = new Template(templateFile.split("digits/")[1].split("/rec.wav")[0], featVectorsLogMel);
			templates.add(t);
		}
		
		for(String inputFile : inputsString.split(","))
		{
			double[][] featVectorsLogMel = featExtractor.extractAll39Features(IO.read(inputFile));
			Template t = new Template(inputFile.split("digits/")[1].split("/rec.wav")[0], featVectorsLogMel);
			inputs.add(t);
		}
		
		for (Template input : inputs)
		{
			System.out.println("Input : "+ input.name);
			double lowestCost = Double.MAX_VALUE;
			String bestPrediction = "";
			
			for (Template template : templates)
			{
				double cost = DTW.alignmentCost(input.featureVector, template.featureVector);
				if (cost < lowestCost)
				{
					bestPrediction = template.name;
					lowestCost = cost;
				}
				
				System.out.println("\tTemplate "+template.name+" cost "+cost);
				
			}
			
			System.out.println("Best prediction is "+ bestPrediction+"\n");
		}
	}
	
	
	public static void main(String[] args) throws IllegalArgumentException, IOException
	{
		
		String templatesString ="recordings/digits/0/rec.wav,recordings/digits/1/rec.wav,recordings/digits/2/rec.wav,recordings/digits/3/rec.wav,recordings/digits/4/rec.wav,recordings/digits/5/rec.wav,recordings/digits/6/rec.wav,recordings/digits/7/rec.wav,recordings/digits/8/rec.wav,recordings/digits/9/rec.wav";
		String inputsString ="recordings/digits/0/rec.wav,recordings/digits/1/rec.wav,recordings/digits/2/rec.wav,recordings/digits/3/rec.wav,recordings/digits/4/rec.wav,recordings/digits/5/rec.wav,recordings/digits/6/rec.wav,recordings/digits/7/rec.wav,recordings/digits/8/rec.wav,recordings/digits/9/rec.wav";
		//String inputsString = "recordings/digits/0/noisyrec.wav,recordings/digits/1/noisyrec.wav,recordings/digits/2/noisyrec.wav,recordings/digits/3/noisyrec.wav";
		
		runDTWWithSigleTemplates(inputsString, templatesString);
		
	}
	

}
