package speech.trellis;

import java.io.IOException;
import java.util.ArrayList;

import speech.audio.feat.MFCC;
import speech.audio.utils.IO;
import speech.audio.utils.SignalData;
import speech.audio.utils.SignalMath;
import speech.lang.Template;
import speech.trellis.cost.Cost;
import speech.trellis.cost.Eucledian;

public class DTW 
{
	static Cost cost;
	
	DTW()
	{
		cost = new Eucledian();
	}
	
	
	/**
	 * Returns the Lehvenshtein (edit-distance) distance trellis of given input,
	 * template.
	 */
	public static double[][] lehvensteinTrellis(double[][] input, double[][] template) {
		
		if (cost == null)
			cost = new Eucledian();
		
		int yLen = template.length;
		int xLen = input.length;

		//System.out.println(xLen +" * "+input[0].length+" , "+ yLen+" * "+template[0].length);
		
		double[][] trellis = new double[yLen + 1][xLen + 1]; // additional column and row
													// for boundaries of trellis

		int i, j;
		
		//set all vals to maxval
		for (i = 0; i <= yLen; i++)
			for (j = 0; j <= xLen; j++)
				trellis[i][j] = Double.MAX_VALUE;
		
		//Initialize cost for start cell
		trellis[0][0] = 0;
		
		// Initialize cost for the boundary row
		for (j = 1; j <= xLen; j++)
			trellis[0][j] = j * cost.horizontalCost(SignalData.get(input, j-1));

		// Populate the trellis row by row
		for (i = 1; i <= yLen; i++)
			for (j = 1; j <= xLen; j++) 
			{
				double[] curInputCell = SignalData.get(input, j-1);
				double[] curTemplateCell = SignalData.get(template, i-1);
				boolean farthestCellExists = (i-2>=0)? true: false;
				double[] farthestCell;
				if (farthestCellExists)
					farthestCell = SignalData.get(template, i-2);
						
				trellis[i][j] = SignalMath.min(
						trellis[i][j - 1] + cost.horizontalCost(curInputCell),	//dragging (input cell inserted)
						trellis[i - 1][j - 1] + cost.substitutionCost(curInputCell, curTemplateCell),
						farthestCellExists ? trellis[i - 2][j - 1] + cost.substitutionCost(curInputCell, curTemplateCell) : Double.MAX_VALUE 
						);
			}
		return trellis;

	}
	
	public static double alignmentCost(double[][] input, double[][] template)
	{
		double cost = lehvensteinTrellis(input, template)[template.length][input.length];
		//System.out.println(cost);
		return cost;
	}
	
	public static void main(String[] args) throws IllegalArgumentException, IOException
	{
		String templatesString ="recordings/digits/0/rec.wav,recordings/digits/1/rec.wav,recordings/digits/2/rec.wav,recordings/digits/3/rec.wav,recordings/digits/4/rec.wav,recordings/digits/5/rec.wav,recordings/digits/6/rec.wav,recordings/digits/7/rec.wav,recordings/digits/8/rec.wav,recordings/digits/9/rec.wav";
		String inputsString = "recordings/digits/0/noisyrec.wav,recordings/digits/1/noisyrec.wav,recordings/digits/2/noisyrec.wav,recordings/digits/3/noisyrec.wav";
		//String inputsString ="recordings/digits/0/rec.wav,recordings/digits/1/rec.wav,recordings/digits/2/rec.wav,recordings/digits/3/rec.wav,recordings/digits/4/rec.wav,recordings/digits/5/rec.wav,recordings/digits/6/rec.wav,recordings/digits/7/rec.wav,recordings/digits/8/rec.wav,recordings/digits/9/rec.wav";
		
		ArrayList<Template> templates = new ArrayList<Template>();
		ArrayList<Template> inputs = new ArrayList<Template>();
		
		MFCC featExtractor = new MFCC(16000, 50, 7000, 40);
		
		for(String templateFile : templatesString.split(","))
		{
			double[][] featVectorsLogMel = featExtractor.extractFeaturesLogMel(IO.read(templateFile));
			Template t = new Template(templateFile.split("digits/")[1].split("/rec.wav")[0], featVectorsLogMel);
			templates.add(t);
		}
		
		for(String inputFile : inputsString.split(","))
		{
			double[][] featVectorsLogMel = featExtractor.extractFeaturesLogMel(IO.read(inputFile));
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
				double cost = alignmentCost(input.featureVector, template.featureVector);
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

}
