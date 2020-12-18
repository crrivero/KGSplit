package edu.rit.goal.kgsplit.checker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import edu.rit.goal.kgsplit.split.Degree;
import edu.rit.goal.kgsplit.split.Split;
import edu.rit.goal.kgsplit.split.Splitter;
import edu.rit.goal.kgsplit.stat.Cucconi;
import edu.rit.goal.kgsplit.stat.CucconiTest;
import edu.rit.goal.kgsplit.stat.KolmogorovSmirnov;
import edu.rit.goal.kgsplit.stat.NonparametricUnpairedStatTest;
import scala.concurrent.forkjoin.ThreadLocalRandom;

public class DegreeTest {
	public static void main(String[] args) throws Exception {
		String name = Split.SPLIT_ATTRIBUTE_NAME;
		
		for (int k = 0; k < 100; k++) {
			System.out.println(new Date() + " -- Checking: " + k);
			File dbFile = new File("test_db/indeg_test");
			FileUtils.deleteDirectory(dbFile);
			
			// Create a random graph with several predicates.
			BatchInserter inserter = BatchInserters.inserter(dbFile);
			// Create nodes (at least 10).
			List<Long> nodes = new ArrayList<>();
			for (int i = ThreadLocalRandom.current().nextInt(10)+15; i >= 0; i--) {
				inserter.createNode(i, new HashMap<>(), Label.label("X"));
				nodes.add((long) i);
			}
			// In the form s-p->o.
			Map<String, Long> edges = new HashMap<>();
			// Predicates 0, 1, 2. Total number of edges: n*(n-1) per predicate.
			int n = nodes.size();
			for (int i = 0; i < .75*n*(n-1)*3; i++) {
				// Select two random nodes (not the same one).
				long s = nodes.get(ThreadLocalRandom.current().nextInt(nodes.size())),
						o = nodes.get(ThreadLocalRandom.current().nextInt(nodes.size()));
				long p = ThreadLocalRandom.current().nextLong(3l);
				
				String spo = s+"-"+p+"->"+o;
				if (s != o && !edges.containsKey(spo)) {
					long relId = inserter.createRelationship(s, o, RelationshipType.withName(p+""), new HashMap<>());
					edges.put(spo, relId);
				} else
					i--;
			}
			inserter.shutdown();
			List<Long> tripleIds = new ArrayList<>(edges.values());
			
			GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(dbFile);
			for (String p : new String[] {null, "0", "1", "2"})
				for (Degree degType : Degree.values())
					for (double alpha : new double[] {.001, .01, .05, .1})
						for (NonparametricUnpairedStatTest test : new NonparametricUnpairedStatTest[]{new KolmogorovSmirnov(alpha), new Cucconi(alpha)}) {
							// Step by step.
							DegreeChecker checker = new DegreeChecker(db, p, test, degType, name);
							
							// All edges are part of the training split.
							Transaction tx = db.beginTx();
							long triplesUpdated = (long) db.execute(
									"MATCH ()-[p]->() SET p.split=$split RETURN COUNT(p) AS cnt", Map.of("split", Split.TRAINING.ordinal())).next().get("cnt");
							tx.success();
							tx.close();
							
							Splitter.initializeDegreeCount(db);
							
							if (triplesUpdated != tripleIds.size())
								System.out.println("Some triples were not updated!");
							
							double[] originalDegrees = getDegrees(db, degType, p, null);
							
							Collections.shuffle(tripleIds);
							int done = 0;
							// Take each edge at a time.
							for (Long triple : tripleIds) {
								// Stop when we have done a certain amount; for small values, the behavior of the Apache implementation changes for small sizes.
								if (done > tripleIds.size()*.1)
									break;
								
								tx = db.beginTx();
								Relationship rel = db.getRelationshipById(triple);
								long s = rel.getStartNodeId(), relId = rel.getId(), o = rel.getEndNodeId();
								String type = rel.getType().name();
								
								if (p != null && !type.equals(p))
									continue;
								
								boolean oursAccepted = checker.accepted(relId, s, type, o);
								tx.close();
								
								double[] currentDegrees = getDegrees(db, degType, p, relId);
								
								double ourStat = checker.getTest().getLastStatistic();
								Double otherStat = null;
								if (test.getName().equals(KolmogorovSmirnov.NAME))
									otherStat = new KolmogorovSmirnovTest().kolmogorovSmirnovStatistic(originalDegrees, currentDegrees);
								if (test.getName().equals(Cucconi.NAME))
									otherStat = new CucconiTest().cucconiStatistic(originalDegrees, currentDegrees, false);
								
								// Check w.r.t. an external library.
								if (Math.abs(ourStat - otherStat) > 1e-10) {
									System.out.println("Statistics were different: " + ourStat + "; " + otherStat + "; Test: " + test.getName());
									double[] internalDegrees = NonparametricUnpairedStatTest.getDegrees(test.getY());
									Arrays.sort(currentDegrees);
									Arrays.sort(internalDegrees);
									
									boolean equal = currentDegrees.length==internalDegrees.length;
									for (int i = 0; equal && i < currentDegrees.length; i++)
										equal = equal && currentDegrees[i]==internalDegrees[i];
									
									if (!equal)
										System.out.println("The degrees were different!");
								}
								
								if (oursAccepted) {
									tx = db.beginTx();
									String outp = "s."+Degree.OUTDEGREE.getPropertyName(p)+"=s."+Degree.OUTDEGREE.getPropertyName(p)+"+1",
											inp = "o."+Degree.INDEGREE.getPropertyName(p)+"=o."+Degree.INDEGREE.getPropertyName(p)+"+1";
									db.execute("MATCH (s)-[p]->(o) WHERE id(p)=$relId SET p.split=$split, "+outp +", "+inp+" RETURN id(p)", 
											Map.of("relId", relId, "split", Split.VALIDATION.ordinal())).close();
									tx.success();
									tx.close();
									done++;
								} else
									checker.restore();
							}
							
							Splitter.cleanDegreeCount(db);
							
							// Random selection.
							checker = new DegreeChecker(db, p, test, degType, name);
							
							// All edges are part of the training split.
							tx = db.beginTx();
							db.execute("MATCH ()-[p]->() SET p.split=$split RETURN COUNT(p) AS cnt", Map.of("split", Split.TRAINING.ordinal())).next().get("cnt");
							tx.success();
							tx.close();
							
							Collections.shuffle(tripleIds);
							for (int i = 0; i < tripleIds.size()*.1; i++) {
								tx = db.beginTx();
								db.execute("MATCH ()-[p]->() WHERE id(p)=$relId SET p.split=$split RETURN id(p)", 
										Map.of("relId", tripleIds.get(i), "split", Split.VALIDATION.ordinal())).close();
								tx.success();
								tx.close();
							}
							
							checker.accepted();
							
							double ourStat = checker.getTest().getLastStatistic();
							
							double[] currentDegrees = getDegrees(db, degType, p, null);
							Double otherStat = null;
							if (test.getName().equals(KolmogorovSmirnov.NAME))
								otherStat = new KolmogorovSmirnovTest().kolmogorovSmirnovStatistic(originalDegrees, currentDegrees);
							if (test.getName().equals(Cucconi.NAME))
								otherStat = new CucconiTest().cucconiStatistic(originalDegrees, currentDegrees, false);
							
							// Check w.r.t. an external library.
							if (Math.abs(ourStat - otherStat) > 1e-10)
								System.out.println("Statistics were different: " + ourStat + "; " + otherStat + "; Test: " + test.getName());
						}
			db.shutdown();
		}
	}
	
	private static double[] getDegrees(GraphDatabaseService db, Degree type, String p, Long relId) {
		Transaction tx = db.beginTx();
		List<Double> degrees = new ArrayList<>();
		Map<String, Object> params = new HashMap<>(Map.of("split", Split.TRAINING.ordinal()));
		if (p != null)
			params.put("p", p);
		if (relId != null)
			params.put("relId", relId);
		// Optional match is necessary to account for degrees of zero.
		Result res = db.execute("MATCH (n) OPTIONAL MATCH (n)"+type.getArrow(0)+"-[p]-"+type.getArrow(1)+
				"(x) WHERE p.split=$split " + (p!=null?" AND type(p)=$p":"") + (relId!=null?" AND id(p)<>$relId":"") + " RETURN id(n), COUNT(x) AS deg", params);
		while (res.hasNext())
			degrees.add(((Number) res.next().get("deg")).doubleValue());
		res.close();
		tx.close();
		return degrees.stream().mapToDouble(Double::doubleValue).toArray();
	}
	
}
