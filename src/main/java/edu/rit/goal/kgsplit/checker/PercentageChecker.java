package edu.rit.goal.kgsplit.checker;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import edu.rit.goal.kgsplit.split.Split;

public class PercentageChecker extends Checker {
	private GraphDatabaseService db;
	private String p;
	private double percentage;
	private long totalInitialTriples, totalCurrentTriples;
	private String name;
	
	public PercentageChecker(GraphDatabaseService db, String p, double percentage, String name) {
		this.db = db;
		this.p = p;
		this.percentage = percentage;
		this.name = name;
		totalInitialTriples = getTotal(null);
		totalCurrentTriples = totalInitialTriples;
	}
	
	private long getTotal(Split split) {
		Map<String, Object> params = new HashMap<>();
		if (p != null)
			params.put("type", p);
		if (split != null)
			params.put("split", split.ordinal());
		
		StringBuffer query = new StringBuffer();
		query.append("MATCH ()-[p]->()");
		
		if (p != null || split != null)
			query.append(" WHERE ");
		
		if (p != null) {
			query.append("type(p)=$type");
			if (split != null)
				query.append(" AND ");
		}
		
		if (split != null)
			query.append("p."+name+"=$split ");
		
		query.append(" RETURN COUNT(p) AS cnt");
		
		Transaction tx = db.beginTx();
		long ret = (long) db.execute(query.toString(), params).next().get("cnt");
		tx.close();
		
		return ret;
	}

	private boolean wasDecreased;
	// The percentage of triples in training should not go below percentage.
	@Override
	public boolean accepted(long tid, long s, String p, long o) {
		// Assuming we are already in the context of a transaction.
		
		wasDecreased = false;
		// If the predicates are different, there is nothing against this triple.
		if (this.p != null && !this.p.equals(p))
			return true;
		
		totalCurrentTriples--;
		wasDecreased = true;
		
		return checkPercentage();
	}
	
	private double computePercentage() {
		return totalCurrentTriples*1.0/totalInitialTriples;
	}
	
	private boolean checkPercentage() {
		return computePercentage() >= percentage;
	}

	@Override
	public void restore() {
		if (wasDecreased)
			totalCurrentTriples++;
	}

	@Override
	public boolean accepted() {
		totalCurrentTriples = getTotal(Split.TRAINING);
		return checkPercentage();
	}

	@Override
	public String toString() {
		String predicate = "All";
		if (p != null)
			predicate = p;
		return "PercentageChecker (at least "+(percentage*100)+"% of triples in training); Predicate: "+predicate;
	}

	@Override
	public String explanation() {
		return "Percentage of triples in training: " + computePercentage();
	}

}
