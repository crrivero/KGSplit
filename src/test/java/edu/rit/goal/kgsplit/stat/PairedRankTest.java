package edu.rit.goal.kgsplit.stat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PairedRankTest {

	public static void main(String[] args) {
		CucconiTest test = new CucconiTest();
		
		double[] x = new double[] {2.0, .0, 1.0, 2.0, 3.0
				,2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0
//				,1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0
//				3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0
				},
				y = new double[] {2.0, .0, 1.0, 2.0, 2.0
				,2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0
//				,1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0
//				3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0
				};
		
//		test.computeRanksInPooledSample(x, y);
		
//		System.out.println("Ranks: " + test.ranks);
		System.out.println("Coriginal: " + test.cucconiStatistic(x, y, true));
		
		double n = x.length*1.0+y.length*1.0;
		double sigma = x.length * (n+1.0) * (2.0*n+1.0),
				tau = (2.0*n + 1.0)*(8.0*n + 11.0),
				rho = 2.0*(Math.pow(n, 2) - 4.0)/tau - 1.0;
		
		double[] p = new double[x.length+y.length];
		int current = 0;
		for (int i = 0; i < x.length; i++) {
			p[current] = x[i];
			current++;
		}
		for (int i = 0; i < y.length; i++) {
			p[current] = y[i];
			current++;
		}
		Arrays.sort(p);
		
		Map<Double, Double> firstRanks = new HashMap<>(), lastRanks = new HashMap<>();
		current = 1;
		for (int i = 0; i < p.length; i++) {
			if (!firstRanks.containsKey(p[i]))
				firstRanks.put(p[i], current*1.0);
			lastRanks.put(p[i], current*1.0);
			current++;
		}
		
		Map<Double, Double> midRanks = new HashMap<>();
		for (Double d : firstRanks.keySet())
			midRanks.put(d, (firstRanks.get(d)+lastRanks.get(d))/2.0);
		
		double umid = ((6.0*computeU(x, midRanks, false))-sigma)/Math.sqrt(y.length*1.0*sigma*tau/(5.0*(2.0*n+1.0))), 
				vmid = ((6.0*computeV(x, midRanks, n, false))-sigma)/Math.sqrt(y.length*1.0*sigma*tau/(5.0*(2.0*n+1.0))),
				uall = ((6.0*computeU(x, firstRanks, true))-sigma)/Math.sqrt(y.length*1.0*sigma*tau/(5.0*(2.0*n+1.0))), 
				vall = ((6.0*computeV(x, firstRanks, n, true))-sigma)/Math.sqrt(y.length*1.0*sigma*tau/(5.0*(2.0*n+1.0)));
		
		System.out.println("UMid(x): " + (6.0*computeU(x, midRanks, false)));
		System.out.println("VMid(x): " + (6.0*computeV(x, midRanks, n, false)));
		System.out.println("UAll(x): " + (6.0*computeU(x, firstRanks, true)));
		System.out.println("VAll(x): " + (6.0*computeV(x, firstRanks, n, true)));
		System.out.println("Sigma(x): " + sigma);
		
		System.out.println("UMid(y): " + (6.0*computeU(y, midRanks, false)));
		System.out.println("VMid(y): " + (6.0*computeV(y, midRanks, n, false)));
		System.out.println("UAll(y): " + (6.0*computeU(y, firstRanks, true)));
		System.out.println("VAll(y): " + (6.0*computeV(y, firstRanks, n, true)));
		System.out.println("Sigma(y): " + (y.length * (n+1.0) * (2.0*n+1.0)));
		
		double cmid = (Math.pow(umid, 2)+Math.pow(vmid, 2)-(2.0*rho*umid*vmid))/(2.0*(1.0-Math.pow(rho, 2))),
				call = (Math.pow(uall, 2)+Math.pow(vall, 2)-(2.0*rho*uall*vall))/(2.0*(1.0-Math.pow(rho, 2)));
		
		System.out.println("Cmid: " + cmid);
		System.out.println("Call: " + call);
	}
	
	private static double computeU(double[] sample, Map<Double, Double> ranks, boolean modify) {
		Map<Double, Double> copyRanks = new HashMap<>(ranks);
		
		double ret = .0;
		for (Double v : sample) {
			ret+=Math.pow(copyRanks.get(v), 2);
			if (modify)
				copyRanks.put(v, copyRanks.get(v)+2.0);
		}
		return ret;
	}
	
	private static double computeV(double[] sample, Map<Double, Double> ranks, double n, boolean modify) {
		Map<Double, Double> copyRanks = new HashMap<>(ranks);
		
		double ret = .0;
		for (Double v : sample) {
			ret+=Math.pow(n+1.0-copyRanks.get(v), 2);
			if (modify)
				copyRanks.put(v, copyRanks.get(v)+2.0);
		}
		return ret;
	}

}
