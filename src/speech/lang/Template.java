package speech.lang;

public class Template 
{
	public String name;
	public double[][] featureVector;
	
	public Template(String templateName, double[][] templateFeatVector)
	{
		name = templateName;
		featureVector = templateFeatVector;
	}
}
