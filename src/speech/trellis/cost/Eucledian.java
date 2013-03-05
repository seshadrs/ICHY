package speech.trellis.cost;

public class Eucledian implements Cost
{
	
	public double horizontalCost(double[] x)
	{
		double hc = 0.0;
		for(double a : x)
			hc += a*a;
		return hc;
	}
	
	public double substitutionCost(double[] x,double[] y)
	{
		double sc = 0.0;
		for(int i=0; i< x.length; i++)
			sc += Math.abs(x[i]*x[i]-y[i]*y[i]);
		return sc;
	}
	
	public double diagonalCost(double[] x, double[] y)
	{
		double dc = 0.0;
		for(int i=0; i< x.length; i++)
			dc += Math.abs(x[i]*x[i]-y[i]*y[i]);
		return dc;
	}

}
