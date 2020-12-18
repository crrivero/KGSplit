package edu.rit.goal.kgsplit.stat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;

import scala.concurrent.forkjoin.ThreadLocalRandom;

public class KSTest {

	public static void main(String[] args) {
		double alpha = .05;
		for (int i = 0; i < 100000; i++) {
			// Generate degree distributions.
			Set<Long> degrees = new HashSet<>(Arrays.asList(ThreadLocalRandom.current().longs(50, 1, 11).boxed().toArray(Long[]::new)));
			Map<Long, AtomicInteger> count = new HashMap<>();
			for (Long d : degrees)
				count.put(d, new AtomicInteger(ThreadLocalRandom.current().nextInt(10)+1));
//			System.out.println("Original count: " + count);
			
			KolmogorovSmirnov ks = new KolmogorovSmirnov(alpha);
			ks.initX(count);
			double[] original = KolmogorovSmirnov.getDegrees(count);
			
			for (int j = 0; j < 5; j++) {
				// Get random degree to update.
				long deg = new ArrayList<>(count.keySet()).get(ThreadLocalRandom.current().nextInt(count.size()));
				if (deg == 0) {
					j--;
					continue;
				}
				
//				System.out.println("Decreasing: " + deg);
				
				ks.updateY(deg);
				
				double[] before = KolmogorovSmirnov.getDegrees(count);
				double otherStatBefore = new KolmogorovSmirnovTest().kolmogorovSmirnovStatistic(original, before);
				
				// Update count and recompute cumulative.
				int value = count.get(deg).decrementAndGet();
				if (value == 0)
					count.remove(deg);
				if (!count.containsKey(deg-1))
					count.put(deg-1, new AtomicInteger(1));
				else
					count.get(deg-1).incrementAndGet();
				
				double[] after = KolmogorovSmirnov.getDegrees(count);
				double otherStatAfter = new KolmogorovSmirnovTest().kolmogorovSmirnovStatistic(original, after);
				ks.nullHypothesisRejected();
				double ourStatAfter = ks.getLastStatistic();
				
				// Check w.r.t. an external library.
				if (Math.abs(ourStatAfter - otherStatAfter) > 1e-10)
					System.out.println("Statistics after were different: " + ourStatAfter + "; " + otherStatAfter);
				
				ks.restoreY(deg);
				ks.nullHypothesisRejected();
				double ourStatBefore = ks.getLastStatistic();
				
				// Check w.r.t. an external library.
				if (Math.abs(ourStatBefore - otherStatBefore) > 1e-10)
					System.out.println("Statistics before were different: " + ourStatBefore + "; " + otherStatBefore);
				
				ks.updateY(deg);
			}
		}
	}
	
	

}
