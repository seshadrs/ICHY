package speech.audio.utils;

public class SignalMath {
	
	/*
	 * Returns the Log(RMS) of the values in the array
	 * */
	public static float logRootMeanSquare(float[] data)
	{
		float squareSum=0;
		
		for ( float x : data)
		{
			squareSum += Math.pow(x, 2);
		}
		
		double RMS = Math.max((double)squareSum/(double)data.length,1) ;
		
		return (float) (Math.log10((float) RMS) * 20);
	}
	
	public static double min(double a, double b, double c)
	{
		return Math.min(a, Math.min(b, c));
	}

}
