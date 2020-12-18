package edu.rit.goal.kgsplit.stat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

// Reference implementation of the Cucconi test.
public class CucconiTest {

	private boolean drawFromX;
	// https://github.com/tpepler/nonpar/blob/master/R/cucconi.teststat.R
	public double cucconiStatistic(double[] x, double[] y, boolean drawFromX) {
		this.drawFromX = drawFromX;
		computeRanksInPooledSample(x, y);
		
		double u = compute(false), v = compute(true);
		return (Math.pow(u, 2) + Math.pow(v, 2) - 2.0*rho*u*v)/(2.0*(1.0-Math.pow(rho, 2)));
	}
	
	public boolean cucconiTest(double[] x, double[] y, double alpha, boolean drawFromX) {
		// If true, null hypothesis is accepted; rejected otherwise.
		return cucconiStatistic(x, y, drawFromX) < getAcceptanceThreshold(alpha);
	}
	
	public static double getAcceptanceThreshold(double alpha) {
		return -1.0 * Math.log(alpha);
	}
	
	private double compute(boolean isContraryRank) {
		NavigableMap<Double, AtomicInteger> selected = null;
		if (drawFromX)
			selected = uniqueValuesInX;
		else
			selected = uniqueValuesInY;
		
		double x = .0;
		Double current = selected.firstKey();
		while (true) {
			for (int i = 0; i < selected.get(current).intValue(); i++)
				if (!isContraryRank)
					x+= Math.pow(ranks.get(current).doubleValue()+(i*2.0), 2);
				else
					x+= Math.pow(nm + 1.0 - (ranks.get(current).doubleValue()+(i*2.0)), 2);
			
			current = selected.higherKey(current);
			if (current == null)
				break;
		}
		
		return (x - subtrahend) / divisor;
	}
	
	private double n, m, nm;
	private Map<Double, Double> ranks;
	private NavigableMap<Double, AtomicInteger> uniqueValuesInX, uniqueValuesInY;
	private double subtrahend, divisor, rho;
	
	private void computeRanksInPooledSample(double[] x, double[] y) {
		n = 1.0*x.length;
		m = 1.0*y.length;
		nm = 1.0*(n+m);
		
		subtrahend = (drawFromX?n:m)*(nm+1.0)*(2.0*nm+1.0)/6.0;
		divisor = Math.sqrt(n*m*(nm+1.0)*(2.0*nm+1.0)*(8.0*nm+11.0)/180.0);
		rho = (2.0*(Math.pow(nm, 2)-4.0)/((2.0*nm+1.0)*(8.0*nm+11.0)))-1.0;
		
		ranks = new HashMap<>();
		uniqueValuesInX = new TreeMap<>();
		uniqueValuesInY = new TreeMap<>();
		
		Arrays.sort(x);
		Arrays.sort(y);
		
		for (double xValue : x) {
			if (!uniqueValuesInX.containsKey(xValue))
				uniqueValuesInX.put(xValue, new AtomicInteger());
			uniqueValuesInX.get(xValue).incrementAndGet();
		}
		
		for (double yValue : y) {
			if (!uniqueValuesInY.containsKey(yValue))
				uniqueValuesInY.put(yValue, new AtomicInteger());
			uniqueValuesInY.get(yValue).incrementAndGet();
		}
		
		int lastRank = 1;
		Double currentX = uniqueValuesInX.firstKey(), currentY = uniqueValuesInY.firstKey();
		while (true) {
			int cmp = currentX.compareTo(currentY);
			
			if (cmp <= 0)
				ranks.put(currentX, 1.0*lastRank);
			else
				ranks.put(currentY, 1.0*lastRank);
			
			if (cmp <= 0) {
				lastRank+=uniqueValuesInX.get(currentX).intValue();
				currentX = uniqueValuesInX.higherKey(currentX);
				if (currentX == null)
					currentX = Double.MAX_VALUE;
			}
			
			if (cmp >= 0) {
				lastRank+=uniqueValuesInY.get(currentY).intValue();
				currentY = uniqueValuesInY.higherKey(currentY);
				if (currentY == null)
					currentY = Double.MAX_VALUE;
			}
			
			if (currentX.equals(Double.MAX_VALUE) && currentY.equals(Double.MAX_VALUE))
				break;
		}
	}
}
