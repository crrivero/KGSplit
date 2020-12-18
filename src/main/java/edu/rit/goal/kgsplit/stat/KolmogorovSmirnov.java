package edu.rit.goal.kgsplit.stat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;

// Check: https://en.wikipedia.org/wiki/Kolmogorov%E2%80%93Smirnov_test#Two-sample_Kolmogorov%E2%80%93Smirnov_test
public class KolmogorovSmirnov extends NonparametricUnpairedStatTest {
	public static final String NAME = "KolmogorovSmirnov";
	
	private KolmogorovSmirnovTest other;
	private double[] arrayX;
	
	public KolmogorovSmirnov(double alpha) {
		super(alpha);
	}
	
	public KolmogorovSmirnov(double alpha, boolean verify) {
		super(alpha);
		if (verify)
			other = new KolmogorovSmirnovTest();
	}
	
	@Override
	public void initX(Map<Long, AtomicInteger> count) {
		super.initX(count);
		if (other != null)
			arrayX = getDegrees(x);
	}
	
	@Override
	public void computeRejectionThreshold() {
		// c(alpha) * sqrt((n+m)/(n*m)), where c(alpha)=sqrt(-ln(alpha/2)/2)
		rejectionThreshold = Math.sqrt(-Math.log(alpha/2.0)/2.0) * Math.sqrt((n*1.0+m)/(n*1.0*m));
	}
	
	@Override
	public boolean nullHypothesisRejected() {
		boolean rejected = super.nullHypothesisRejected();
		
		if (other != null) {
			double[] arrayY = getDegrees(y);
			boolean otherRejected = other.kolmogorovSmirnovTest(arrayX, arrayY) < alpha;
			if (rejected != otherRejected)
				System.out.println("Different rejections!");
		}
		
		return rejected;
	}

	@ Override
	public double getStatistic() {
		// Find max difference between the cumulative distributions.
		double maxDiff = .0;
		Long currentX = x.firstKey(), currentY = y.firstKey();
		AtomicInteger xAccum = new AtomicInteger(x.get(currentX).intValue()),
				yAccum = new AtomicInteger(y.get(currentY).intValue());
		
		while (true) {
			int cmp = Long.compare(currentX, currentY);
			
			Integer valueX = null, valueY = null;
			
			if (cmp == 0) {
				valueX = xAccum.intValue();
				valueY = yAccum.intValue();
			} else if (cmp < 0) {
				valueX = xAccum.intValue();
				valueY = yAccum.intValue()-y.get(currentY).intValue();
			} else {
				valueX = xAccum.intValue()-x.get(currentX).intValue();
				valueY = yAccum.intValue();
			}
			
			double diff = Math.abs(valueX*1.0/n-valueY*1.0/m);
			if (diff > maxDiff)
				maxDiff = diff;
			
			if (cmp <= 0) {
				currentX = x.higherKey(currentX);
				if (currentX == null)
					break;
				xAccum.addAndGet(x.get(currentX).intValue());
			}
			
			if (cmp >= 0) {
				currentY = y.higherKey(currentY);
				if (currentY == null)
					break;
				yAccum.addAndGet(y.get(currentY).intValue());
			}
		}
		
		lastStat = maxDiff;
		
		return maxDiff;
	}
	
	@Override
	public String getName() {
		return NAME;
	}

}
