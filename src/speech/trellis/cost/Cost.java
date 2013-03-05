package speech.trellis.cost;

public interface Cost {
	
	public double horizontalCost(double[] x);
	
	public double substitutionCost(double[] x,double[] y);
	
	public double diagonalCost(double[] x, double[] y);

}
