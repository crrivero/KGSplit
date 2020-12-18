package edu.rit.goal.kgsplit.split;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import edu.rit.goal.kgsplit.checker.Checker;

public class Splitter {
	public static void initializeDegreeCount(GraphDatabaseService db) {
		// For each predicate, initialize the number of edges related to each node that are not in training (zero for everybody at the beginning).
		Transaction tx = db.beginTx();
		Set<RelationshipType> allTypes = db.getAllRelationshipTypes().stream().collect(Collectors.toSet());
		tx.close();
		
		allTypes.add(null);
		for (RelationshipType type : allTypes) {
			tx = db.beginTx();
			db.execute("MATCH (n) SET n."+Degree.INDEGREE.getPropertyName(type==null?null:type.name())+"=0, "
					+ "n."+Degree.OUTDEGREE.getPropertyName(type==null?null:type.name())+"=0 "
					+ "RETURN COUNT(n) AS cnt").close();
			tx.success();
			tx.close();
		}
	}
	
	public static void cleanDegreeCount(GraphDatabaseService db) {
		// For each predicate, initialize the number of edges related to each node that are not in training (zero for everybody at the beginning).
		Transaction tx = db.beginTx();
		Set<RelationshipType> allTypes = db.getAllRelationshipTypes().stream().collect(Collectors.toSet());
		tx.close();
		
		allTypes.add(null);
		for (RelationshipType type : allTypes) {
			tx = db.beginTx();
			db.execute("MATCH (n) SET n."+Degree.INDEGREE.getPropertyName(type==null?null:type.name())+"=null, "
					+ "n."+Degree.OUTDEGREE.getPropertyName(type==null?null:type.name())+"=null "
					+ "RETURN COUNT(n) AS cnt").next().get("cnt");
			tx.success();
			tx.close();
		}
	}
	
	public static void perform(GraphDatabaseService db, List<Checker> checkers, String name) {
		// At the beginning, all triples are in training.
		Transaction tx = db.beginTx();
		int totalTriplesInTraining = ((Number) db.execute("MATCH ()-[p]->() SET p."+name+"=$split RETURN COUNT(p) AS cnt", 
				Map.of("split",Split.TRAINING.ordinal())).next().get("cnt")).intValue();
		tx.success();
		tx.close();
		
		initializeDegreeCount(db);
		
		// Let's check triple by triple. Right now, we will perform this based on the internal id. Other orders are possible.
		int totalRestTriples = 0, pending = totalTriplesInTraining;
		long totalTimeAccepting = 0, totalTimeRestoring = 0, totalTimeMoving = 0;
		tx = db.beginTx();
		Result res = db.execute("MATCH (s)-[p]->(o) RETURN id(p) AS tid, id(s) AS s, type(p) AS p, id(o) AS o ORDER BY id(p)");
		while (res.hasNext()) {
			Map<String, Object> row = res.next();
			long tid = (long) row.get("tid"), s = (long) row.get("s"), o = (long) row.get("o");
			String p = (String) row.get("p");
			pending--;
			
			if (pending % 1000 == 0)
				System.out.println(new Date() + " -- Pending: " + pending + "; Accepting (secs): " + (totalTimeAccepting/1e9) + 
						"; Moving (secs): " + (totalTimeMoving/1e9) + "; Restoring (secs): " + (totalTimeRestoring/1e9));
			
			// Check if the triple is accepted to be moved to another split.
			long before = System.nanoTime();
			boolean accepted = true;
			int i = 0;
			for (i = 0; accepted && i < checkers.size(); i++)
				accepted = accepted && checkers.get(i).accepted(tid, s, p, o);
			long after = System.nanoTime();
			totalTimeAccepting += after - before;
			
			if (accepted) {
				before = System.nanoTime();
				// Let's move the triple to another split! The strategy to select the split can be changed.
				Split selected = null;
				if (totalRestTriples%2==0)
					selected = Split.VALIDATION;
				else
					selected = Split.TEST;
				
				String outp = "s."+Degree.OUTDEGREE.getPropertyName(p)+"=s."+Degree.OUTDEGREE.getPropertyName(p)+"+1",
						inp = "o."+Degree.INDEGREE.getPropertyName(p)+"=o."+Degree.INDEGREE.getPropertyName(p)+"+1";
				db.execute("MATCH (s)-[p]->(o) WHERE id(p)=$tid SET p."+name+"=$split, "+outp+", "+inp+" RETURN id(p)", 
						Map.of("tid",tid,"split",selected.ordinal())).close();
					
				totalRestTriples++;
				totalTriplesInTraining--;
				after = System.nanoTime();
				
				totalTimeMoving += after - before;
			} else {
				before = System.nanoTime();
				// Revert changes.
				for (int j = 0; j < i; j++)
					checkers.get(j).restore();
				after = System.nanoTime();
				
				totalTimeRestoring += after - before;
			}
		}
		res.close();
		tx.success();
		tx.close();
		
		cleanDegreeCount(db);
	}
}
