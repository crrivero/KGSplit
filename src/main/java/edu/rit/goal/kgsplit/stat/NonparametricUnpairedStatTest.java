package edu.rit.goal.kgsplit.stat;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class NonparametricUnpairedStatTest {
	protected NavigableMap<Long, AtomicInteger> x, y;
	// n is the total in X; m is the total in Y.
	protected int n, m;
	protected double alpha, rejectionThreshold;
	
	public NonparametricUnpairedStatTest(double alpha) {
		super();
		this.alpha = alpha;
	}

	public void initX(Map<Long, AtomicInteger> count) {
		x = new TreeMap<>();
		n = init(count, x);
		// Assuming this will be used iteratively, Y is just a copy of X.
		y = new TreeMap<>();
		for (Long key : x.keySet())
			y.put(key, new AtomicInteger(x.get(key).intValue()));
		m = n;
		computeRejectionThreshold();
	}
	
	public void initY(Map<Long, AtomicInteger> count) {
		y = new TreeMap<>();
		m = init(count, y);
		computeRejectionThreshold();
	}
	
	protected abstract void computeRejectionThreshold();
	
	private int init(Map<Long, AtomicInteger> count, NavigableMap<Long, AtomicInteger> cumulative) {
		// Initialize cumulative functions.
		return initNavigableMap(count, cumulative);
	}
	
	private int initNavigableMap(Map<Long, AtomicInteger> count, NavigableMap<Long, AtomicInteger> map) {
		int total = 0;
		for (Long key : count.keySet()) {
			map.put(key, new AtomicInteger(count.get(key).intValue()));
			total+=count.get(key).intValue();
		}
		return total;
	}
	
	protected Double prevStat, lastStat;
	
	public void updateY(long key) {
		assert key > 0;
		
		int value = y.get(key).decrementAndGet();
		if (value == 0)
			y.remove(key);
		if (!y.containsKey(key-1))
			y.put(key-1, new AtomicInteger(1));
		else
			y.get(key-1).incrementAndGet();
		prevStat = lastStat;
	}
	
	public void restoreY(long key) {
		assert key > 0;
		
		if (!y.containsKey(key))
			y.put(key, new AtomicInteger());
		y.get(key).incrementAndGet();
		int value = y.get(key-1).decrementAndGet();
		if (value == 0)
			y.remove(key-1);
		lastStat = prevStat;
	}
	
	public boolean nullHypothesisRejected() {
		double stat = getStatistic();
		
		// Null hypothesis: the samples are drawn from the same distribution.
		// Null hypothesis is rejected at level alpha if stat > rejectionThreshold.
		// If true, null hypothesis is rejected.
		return stat > rejectionThreshold;
	}
	
	public abstract double getStatistic();
	
	public double getLastStatistic() {
		return lastStat;
	}
	
	public double getAlpha() {
		return alpha;
	}
	
	public NavigableMap<Long, AtomicInteger> getY() {
		return y;
	}

	public static double[] getDegrees(Map<Long, AtomicInteger> count) {
		int size = 0;
		for (AtomicInteger number : count.values())
			size += number.intValue();
		double[] ret = new double[size];
		int i = 0;
		for (Long key : count.keySet())
			for (int j = 0; j < count.get(key).intValue(); j++) {
				ret[i] = key*1.0;
				i++;
			}
		return ret;
	}
	
	public abstract String getName();
	
}
