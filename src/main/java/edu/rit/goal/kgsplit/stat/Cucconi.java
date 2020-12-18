package edu.rit.goal.kgsplit.stat;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

// Check: https://link.springer.com/article/10.1007/s00184-019-00712-x
public class Cucconi extends NonparametricUnpairedStatTest {
	public static final String NAME = "Cucconi";
	
	private CucconiTest other;
	private double[] arrayX;
	
	public Cucconi(double alpha) {
		super(alpha);
	}
	
	public Cucconi(double alpha, boolean verify) {
		super(alpha);
		
		if (verify)
			other = new CucconiTest();
	}
	
	private double subtrahend, divisor, rho, nm, cDivisor;
	private void computeAux() {
		nm = 1.0*(n+m);
		
		// We will always draw from y.
		subtrahend = (1.0*m)*(nm+1.0)*(2.0*nm+1.0)/6.0;
		divisor = Math.sqrt(1.0*n*m*(nm+1.0)*(2.0*nm+1.0)*(8.0*nm+11.0)/180.0);
		rho = (2.0*(Math.pow(nm, 2)-4.0)/((2.0*nm+1.0)*(8.0*nm+11.0)))-1.0;
		cDivisor = (2.0*(1.0-Math.pow(rho, 2)));
	}
	
	private class RankRange {
		long first, last;

		public RankRange(long first, long last) {
			super();
			this.first = first;
			this.last = last;
		}

		@Override
		public String toString() {
			return "[" + first + ", " + last + "]";
		}
		
		public long getTotal() {
			return last-first+1;
		}
		
	}
	
	private NavigableMap<Long, RankRange> ranks;

	@Override
	public void initX(Map<Long, AtomicInteger> count) {
		super.initX(count);
		computeAux();
		if (other != null)
			arrayX = getDegrees(x);
		initRanks();
	}
	
	private void initRanks() {
		ranks = new TreeMap<>();
		int lastRank = 1;
		Long currentX = x.firstKey(), currentY = y.firstKey();
		while (true) {
			int cmp = currentX.compareTo(currentY);
			
			int firstRank = lastRank;
			
			if (cmp <= 0) {
				lastRank+=x.get(currentX).intValue();
				currentX = x.higherKey(currentX);
				if (currentX == null)
					currentX = Long.MAX_VALUE;
			}
			
			if (cmp >= 0) {
				lastRank+=y.get(currentY).intValue();
				ranks.put(currentY, new RankRange(firstRank, lastRank-1));
				currentY = y.higherKey(currentY);
				if (currentY == null)
					currentY = Long.MAX_VALUE;
			}
			
			if (currentX.equals(Long.MAX_VALUE) && currentY.equals(Long.MAX_VALUE))
				break;
		}
	}
	
	@Override
	public void initY(Map<Long, AtomicInteger> count) {
		super.initY(count);
		computeAux();
		initRanks();
	}
	
	private RankRange lastRange;
	@Override
	public void updateY(long key) {
		super.updateY(key);
		
		RankRange range = ranks.get(key);
		long prevFirst = range.first;
		range.first++;
		if (range.getTotal() == 0)
			ranks.remove(key);
		RankRange other = ranks.get(key-1);
		if (other == null)
			ranks.put(key-1, other = new RankRange(prevFirst, prevFirst-1));
		other.last++;
		
		lastRange = range;
	}

	@Override
	public void restoreY(long key) {
		super.restoreY(key);
		
		RankRange range = ranks.get(key);
		if (range == null)
			ranks.put(key, range = lastRange);
		range.first--;
		range = ranks.get(key-1);
		range.last--;
		if (range.getTotal() == 0)
			ranks.remove(key-1);
	}

	@Override
	public void computeRejectionThreshold() {
		rejectionThreshold = -1.0 * Math.log(alpha);
	}
	
	@Override
	public boolean nullHypothesisRejected() {
		boolean rejected = super.nullHypothesisRejected();
		
		if (other != null) {
			double[] arrayY = getDegrees(y);
			boolean otherRejected = !other.cucconiTest(arrayX, arrayY, alpha, false);
			if (rejected != otherRejected)
				System.out.println("Different rejections!");
		}
		
		return rejected;
	}
	
	@Override
	public double getStatistic() {
		double u = .0, v = .0;
		
		for (Long k : y.keySet()) {
			double up = .0, vp = .0;
			long rank = ranks.get(k).first;
			int total = y.get(k).intValue();
			
			for (int i = 0; i < total; i++) {
				up += Math.pow(rank+(i*2.0), 2);
				vp += Math.pow(nm + 1.0 - (rank+(i*2.0)), 2);
			}
			
			u += up;
			v += vp;
		}
		
		u = (u - subtrahend) / divisor;
		v = (v - subtrahend) / divisor;
		
		double c = (Math.pow(u, 2) + Math.pow(v, 2) - 2.0*rho*u*v)/cDivisor;
		
		lastStat = c;
		
		return c;
	}

	@Override
	public String getName() {
		return NAME;
	}

}
