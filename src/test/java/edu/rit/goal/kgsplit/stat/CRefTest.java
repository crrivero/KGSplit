package edu.rit.goal.kgsplit.stat;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.distribution.LaplaceDistribution;

public class CRefTest {

	public static void main(String[] args) {
		double alpha = .05;
		
		System.out.println("Using alpha="+alpha+"; acceptance threshold: "+CucconiTest.getAcceptanceThreshold(alpha));
		System.out.println();
		
		// No ties.
		double[] placebo = new double[] {7, 5, 6, 4, 12},
				newdrug = new double[] {3, 6, 4, 2, 1};
		
		CucconiTest test = new CucconiTest();
		System.out.println("Statistic drawing from X: " + test.cucconiStatistic(placebo, newdrug, true));
		System.out.println("Statistic drawing from Y: " + test.cucconiStatistic(placebo, newdrug, false));
//		System.out.println("Accepted at "+alpha+" drawing from X: " + test.cucconiTest(placebo, newdrug, alpha, true));
//		System.out.println("Accepted at "+alpha+" drawing from Y: " + test.cucconiTest(placebo, newdrug, alpha, false));
		System.out.println();
		
		// Ties.
		double[] usualcare = new double[] {8, 7, 6, 2, 5, 8, 7, 3}, newprogram = new double[] {9, 8, 7, 8, 10, 9, 6};
		
		test = new CucconiTest();
		System.out.println("Statistic drawing from X: " + test.cucconiStatistic(usualcare, newprogram, true));
		System.out.println("Statistic drawing from Y: " + test.cucconiStatistic(usualcare, newprogram, false));
//		System.out.println("Accepted at "+alpha+" drawing from X: " + test.cucconiTest(usualcare, newprogram, alpha, true));
//		System.out.println("Accepted at "+alpha+" drawing from Y: " + test.cucconiTest(usualcare, newprogram, alpha, false));
		System.out.println();
		
		// Other with ties.
		double[] ties1 = new double[] {5,1,4,7,4,35,25}, ties2 = new double[] {5,1,4,7,4,35,25};
		test = new CucconiTest();
		System.out.println("Statistic drawing from X: " + test.cucconiStatistic(ties1, ties2, true));
		System.out.println("Statistic drawing from Y: " + test.cucconiStatistic(ties1, ties2, false));
//		System.out.println("Accepted at "+alpha+" drawing from X: " + test.cucconiTest(ties1, ties2, alpha, true));
//		System.out.println("Accepted at "+alpha+" drawing from Y: " + test.cucconiTest(ties1, ties2, alpha, false));
		System.out.println();
		
		for (double mu : new double[] {0, -2.5, 2.5, -5.0, 5.0})
			for (double beta : new double[] {1.0, 2.0, 4.0}) {
				System.out.println("Laplace distribution mu="+mu +" and beta="+beta);
				LaplaceDistribution dist = new LaplaceDistribution(mu, beta);
				
				for (int size : new int[] {100, 1000, 10000, 100000}) {
					System.out.println("Size: " + size);
					double[] x = new double[size], y = new double[size];
					for (int i = 0; i < x.length; i++) {
						x[i] = dist.sample();
						y[i] = dist.sample();
					}
					
					test = new CucconiTest();
					System.out.println("Statistic drawing from X: " + test.cucconiStatistic(x, y, true));
					System.out.println("Statistic drawing from Y: " + test.cucconiStatistic(x, y, false));
					System.out.println("Accepted at "+alpha+" drawing from X: " + test.cucconiTest(x, y, alpha, true));
					System.out.println("Accepted at "+alpha+" drawing from Y: " + test.cucconiTest(x, y, alpha, false));
					System.out.println();
				}
			}
		
		
		
		System.out.println("Uniform distribution between 1 and 100");
		for (int size : new int[] {100, 1000, 10000, 100000}) {
			System.out.println("Size: " + size);
			double[] x = new double[size], y = new double[size];
			for (int i = 0; i < x.length; i++) {
//				int next = ThreadLocalRandom.current().nextInt(10);
				x[i] = ThreadLocalRandom.current().nextInt(100);
				y[i] = ThreadLocalRandom.current().nextInt(100);
			}
	//		for (int i = 0; i < 2500; i++)
	//			y[ThreadLocalRandom.current().nextInt(y.length)] = ThreadLocalRandom.current().nextInt(10);
			
			test = new CucconiTest();
			System.out.println("Statistic drawing from X: " + test.cucconiStatistic(x, y, true));
			System.out.println("Statistic drawing from Y: " + test.cucconiStatistic(x, y, false));
			System.out.println("Accepted at "+alpha+" drawing from X: " + test.cucconiTest(x, y, alpha, true));
			System.out.println("Accepted at "+alpha+" drawing from Y: " + test.cucconiTest(x, y, alpha, false));
			System.out.println();
		}
	}

}
