package edu.rit.goal.kgsplit.checker;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import edu.rit.goal.kgsplit.split.Split;

public class NonZeroDegreeChecker extends Checker {
	private GraphDatabaseService db;
	private String p;
	private String name;
	
	public NonZeroDegreeChecker(GraphDatabaseService db, String p, String name) {
		this.db = db;
		this.p = p;
		this.name = name;
	}

	// s and o should never have a degree of zero in training.
	@Override
	public boolean accepted(long tid, long s, String p, long o) {
		// Assuming we are already in the context of a transaction.
		
		// If the predicates are different, there is nothing against this triple.
		if (this.p != null && !this.p.equals(p))
			return true;
		
		Map<String, Object> params = new HashMap<>(Map.of("s",s,"o",o,"split",Split.TRAINING.ordinal(),"tid",tid));
		if (this.p != null)
			params.put("p", this.p);
		
		boolean ret = true;
		Result res = db.execute("MATCH (s)-[p]-(x) WHERE id(s)=$s AND p."+name+"=$split AND id(p)<>$tid "+
				(this.p != null?" AND type(p)=$p":"")+" RETURN id(x) LIMIT 1", params);
		ret = ret && res.hasNext();
		res.close();
		
		if (ret) {
			res = db.execute("MATCH (o)-[p]-(x) WHERE id(o)=$o AND p."+name+"=$split AND id(p)<>$tid "+ 
				(this.p != null?" AND type(p)=$p":"")+" RETURN id(x) LIMIT 1", params);
			ret = ret && res.hasNext();
			res.close();
		}
		
		return ret;
	}

	@Override
	public void restore() { }

	private int totalNodes;
	@Override
	public boolean accepted() {
		Transaction tx = db.beginTx();
		long cnt = (long) db.execute("MATCH (n) WHERE "
				+ "NOT((n)-["+(p!=null?":`"+p+"`":"")+" {"+name+":"+Split.TRAINING.ordinal()+"}]-()) RETURN COUNT(n) AS cnt").next().get("cnt");
		totalNodes = (int) cnt;
		tx.close();
		return cnt == 0;
	}

	@Override
	public String toString() {
		String predicate = "All";
		if (p != null)
			predicate = p;
		return "NonZeroDegreeChecker; Predicate: "+predicate;
	}

	@Override
	public String explanation() {
		return "Total nodes with zero degree: " + totalNodes;
	}

}
