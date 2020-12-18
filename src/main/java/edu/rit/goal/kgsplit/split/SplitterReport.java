package edu.rit.goal.kgsplit.split;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import com.google.common.util.concurrent.AtomicDouble;

public class SplitterReport {
	protected double totalNodes;
	protected Map<Integer, AtomicInteger> totalTriplesBySplit, totalNodesBySplit, totalPredicatesBySplit;
	protected Map<Integer, Map<String, AtomicInteger>> triplesPerPredicateBySplit;
	protected Map<String, AtomicInteger> totalSubjectsPerPredicate, totalObjectsPerPredicate;
	protected Map<Integer, Map<String, AtomicInteger>> totalSubjectsPerPredicateBySplit, totalObjectsPerPredicateBySplit;

	public SplitterReport() {
		super();
		
		totalTriplesBySplit = new HashMap<>();
		totalNodesBySplit = new HashMap<>();
		totalPredicatesBySplit = new HashMap<>();
		triplesPerPredicateBySplit = new HashMap<>();
		totalSubjectsPerPredicate = new HashMap<>();
		totalObjectsPerPredicate = new HashMap<>();
		totalSubjectsPerPredicateBySplit = new HashMap<>();
		totalObjectsPerPredicateBySplit = new HashMap<>();
		
		for (Split s : Split.values()) {
			totalTriplesBySplit.put(s.ordinal(), new AtomicInteger());
			totalNodesBySplit.put(s.ordinal(), new AtomicInteger());
			totalPredicatesBySplit.put(s.ordinal(), new AtomicInteger());
			triplesPerPredicateBySplit.put(s.ordinal(), new HashMap<>());
			totalSubjectsPerPredicateBySplit.put(s.ordinal(), new HashMap<>());
			totalObjectsPerPredicateBySplit.put(s.ordinal(), new HashMap<>());
		}
	}
	
	public static SplitterReport compute(GraphDatabaseService db, String name) {
		SplitterReport report = new SplitterReport();
		
		// Total nodes and predicates by split.
		Transaction tx = db.beginTx();
		
		report.totalNodes = ((Number) db.execute("MATCH (n) RETURN COUNT(n) AS cnt").next().get("cnt")).doubleValue();
		
		for (Split s : Split.values()) {
			report.totalTriplesBySplit.get(s.ordinal()).set(((Number) db.execute(
					"MATCH ()-[p]->() WHERE p."+name+"=$split RETURN COUNT(p) AS cnt", Map.of("split",s.ordinal())).next().get("cnt")).intValue());
			report.totalNodesBySplit.get(s.ordinal()).set(((Number) db.execute(
					"MATCH (n)-[p]-() WHERE p."+name+"=$split RETURN COUNT(DISTINCT n) AS cnt", Map.of("split",s.ordinal())).next().get("cnt")).intValue());
			report.totalPredicatesBySplit.get(s.ordinal()).set(((Number) db.execute(
					"MATCH (n)-[p]-() WHERE p."+name+"=$split RETURN COUNT(DISTINCT type(p)) AS cnt", Map.of("split",s.ordinal())).next().get("cnt")).intValue());
			
			for (RelationshipType predicate : db.getAllRelationshipTypes()) {
				report.triplesPerPredicateBySplit.get(s.ordinal()).put(predicate.name(), new AtomicInteger(((Number) db.execute(
						"MATCH ()-[p]->() WHERE p."+name+"=$split AND type(p)=$p RETURN COUNT(p) AS cnt", 
							Map.of("split",s.ordinal(),"p",predicate.name())).next().get("cnt")).intValue()));
				
				report.totalSubjectsPerPredicateBySplit.get(s.ordinal()).put(predicate.name(), new AtomicInteger(((Number) db.execute(
						"MATCH (s)-[p]->() WHERE p."+name+"=$split AND type(p)=$p RETURN COUNT(DISTINCT s) AS cnt", 
							Map.of("split",s.ordinal(),"p",predicate.name())).next().get("cnt")).intValue()));
				
				report.totalObjectsPerPredicateBySplit.get(s.ordinal()).put(predicate.name(), new AtomicInteger(((Number) db.execute(
						"MATCH ()-[p]->(o) WHERE p."+name+"=$split AND type(p)=$p RETURN COUNT(DISTINCT o) AS cnt", 
							Map.of("split",s.ordinal(),"p",predicate.name())).next().get("cnt")).intValue()));
			}
		}
		
		for (RelationshipType predicate : db.getAllRelationshipTypes()) {
			report.totalSubjectsPerPredicate.put(predicate.name(), new AtomicInteger(((Number) db.execute(
					"MATCH (s)-[p]->() WHERE type(p)=$p RETURN COUNT(DISTINCT s) AS cnt", 
						Map.of("p",predicate.name())).next().get("cnt")).intValue()));
			
			report.totalObjectsPerPredicate.put(predicate.name(), new AtomicInteger(((Number) db.execute(
					"MATCH ()-[p]->(o) WHERE type(p)=$p RETURN COUNT(DISTINCT o) AS cnt", 
						Map.of("p",predicate.name())).next().get("cnt")).intValue()));
		}
		
		tx.close();
		
		return report;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		double totalTriples = .0;
		Map<String, AtomicDouble> avgIndegreeOriginalByPredicate = new HashMap<>(), avgOutdegreeOriginalByPredicate = new HashMap<>();
		Set<String> predicates = new HashSet<>();
		
		for (Split s : Split.values()) {
			totalTriples+=totalTriplesBySplit.get(s.ordinal()).doubleValue();
			for (String predicate : triplesPerPredicateBySplit.get(s.ordinal()).keySet()) {
				predicates.add(predicate);
				
				if (!avgIndegreeOriginalByPredicate.containsKey(predicate))
					avgIndegreeOriginalByPredicate.put(predicate, new AtomicDouble());
				
				if (!avgOutdegreeOriginalByPredicate.containsKey(predicate))
					avgOutdegreeOriginalByPredicate.put(predicate, new AtomicDouble());
				
				avgIndegreeOriginalByPredicate.get(predicate).addAndGet(triplesPerPredicateBySplit.get(s.ordinal()).get(predicate).doubleValue());
				avgOutdegreeOriginalByPredicate.get(predicate).addAndGet(triplesPerPredicateBySplit.get(s.ordinal()).get(predicate).doubleValue());
			}
		}
		
		for (String predicate : avgIndegreeOriginalByPredicate.keySet()) {
			avgIndegreeOriginalByPredicate.get(predicate).set(
					avgIndegreeOriginalByPredicate.get(predicate).doubleValue()/totalObjectsPerPredicate.get(predicate).doubleValue());
			
			avgOutdegreeOriginalByPredicate.get(predicate).set(
					avgOutdegreeOriginalByPredicate.get(predicate).doubleValue()/totalSubjectsPerPredicate.get(predicate).doubleValue());
		}
		
		buf.append("Total triples: " + ((int) totalTriples));
		buf.append("\n");
		buf.append("Total nodes: " + ((int) totalNodes));
		buf.append("\n");
		buf.append("Total predicates: " + predicates.size());
		buf.append("\n");
		
		for (Split s : Split.values()) {
			buf.append("Split: ");
			buf.append(s.name());
			buf.append("\n");
			
			buf.append("\tTotal triples: ");
			buf.append(totalTriplesBySplit.get(s.ordinal()));
			buf.append(" (");
			buf.append((totalTriplesBySplit.get(s.ordinal()).doubleValue()*100.0/totalTriples));
			buf.append("%)");
			buf.append("\n");
			buf.append("\tTotal nodes: ");
			buf.append(totalNodesBySplit.get(s.ordinal()));
			buf.append(" (");
			buf.append((totalNodesBySplit.get(s.ordinal()).doubleValue()*100.0/totalNodes));
			buf.append("%)");
			buf.append("\n");
			buf.append("\tTotal predicates: ");
			buf.append(totalPredicatesBySplit.get(s.ordinal()));
			buf.append(" (");
			buf.append((totalPredicatesBySplit.get(s.ordinal()).doubleValue()*100.0/predicates.size()));
			buf.append("%)");
			buf.append("\n");
			
			for (String predicate : triplesPerPredicateBySplit.get(s.ordinal()).keySet()) {
				buf.append("\t\tPredicate: ");
				buf.append(predicate);
				buf.append("; Total triples: ");
				buf.append(triplesPerPredicateBySplit.get(s.ordinal()).get(predicate).intValue());
				buf.append("; Average indegree in original graph: ");
				buf.append(avgIndegreeOriginalByPredicate.get(predicate).doubleValue());
				buf.append("; Average outdegree in original graph: ");
				buf.append(avgOutdegreeOriginalByPredicate.get(predicate).doubleValue());
				buf.append("; Average indegree in this split: ");
				buf.append(triplesPerPredicateBySplit.get(s.ordinal()).get(predicate).doubleValue()/
						totalObjectsPerPredicateBySplit.get(s.ordinal()).get(predicate).doubleValue());
				buf.append("; Average outdegree in this split: ");
				buf.append(triplesPerPredicateBySplit.get(s.ordinal()).get(predicate).doubleValue()/
						totalSubjectsPerPredicateBySplit.get(s.ordinal()).get(predicate).doubleValue());
				buf.append("\n");
			}
		}
		
		return buf.toString();
	}
	
}
