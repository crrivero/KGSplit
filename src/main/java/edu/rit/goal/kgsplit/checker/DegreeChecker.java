package edu.rit.goal.kgsplit.checker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import edu.rit.goal.kgsplit.split.Degree;
import edu.rit.goal.kgsplit.split.Split;
import edu.rit.goal.kgsplit.stat.NonparametricUnpairedStatTest;

public class DegreeChecker extends Checker {
	private GraphDatabaseService db;
	private String p;
	private NonparametricUnpairedStatTest test;
	private Degree type;
	private String query;
	private String name;
	
	public DegreeChecker(GraphDatabaseService db, String p, NonparametricUnpairedStatTest test, Degree type, String name) {
		this.db = db;
		this.p = p;
		this.test = test;
		this.type = type;
		this.name = name;
		test.initX(getDegrees(null));
		
		query = "MATCH (n) WHERE id(n)=$id RETURN size((n)"+type.getArrow(0)+"-["+(p!=null?":`"+p+"`":"")+"]-"+
				type.getArrow(1)+"()) AS deg, n."+type.getPropertyName(p)+" AS cntNotInTraining";
	}
	
	private Map<Long, AtomicInteger> getDegrees(Split split) {
		// Create query.
		Map<String, Object> params = new HashMap<>();
		if (p != null)
			params.put("p", p);
		if (split != null)
			params.put("split", split.ordinal());
		// Optional match is necessary to account for degrees of zero.
		StringBuffer query = new StringBuffer("MATCH (n) OPTIONAL MATCH (n)");
		query.append(type.getArrow(0)+"-[p]-"+type.getArrow(1));
		query.append("(x)");
		
		List<String> conditions = new ArrayList<>();
		if (p != null)
			conditions.add("type(p)=$p");
		if (split != null)
			conditions.add("p."+name+"=$split");
		if (!conditions.isEmpty()) {
			query.append(" WHERE ");
			query.append(conditions.get(0));
			for (int i = 1; i < conditions.size(); i++) {
				query.append(" AND ");
				query.append(conditions.get(i));
			}
		}
		query.append(" RETURN id(n) AS id, COUNT(x) AS cnt");
		
		Map<Long, AtomicInteger> degrees = new HashMap<>();
		
		Transaction tx = db.beginTx();
		// Run query grouping by predicate and node.
		Result res = db.execute(query.toString(), params);
		while (res.hasNext()) {
			Map<String, Object> row = res.next();
			long cnt = (long) row.get("cnt");
			if (!degrees.containsKey(cnt))
				degrees.put(cnt, new AtomicInteger(1));
			else
				degrees.get(cnt).incrementAndGet();
		}
		res.close();
		tx.close();
		
		return degrees;
	}
	
	@Override
	public boolean accepted() {
		// Initialize Y using the training split.
		test.initY(getDegrees(Split.TRAINING));
		boolean rejected = test.nullHypothesisRejected();
		return !rejected;
	}
	
	private long lastDegree;
	@Override
	public boolean accepted(long tid, long s, String p, long o) {
		// Assuming we are already in the context of a transaction.
		
		// If the predicates are different, there is nothing against this triple.
		if (this.p != null && !this.p.equals(p)) {
			// We need to inform that there were no changes.
			lastDegree = -1;
			return true;
		}
		
		// (s)-[p]->(o): get current degree in training.
		long id = -1;
		if (type.equals(Degree.INDEGREE))
			id=o;
		if (type.equals(Degree.OUTDEGREE))
			id=s;
		Map<String, Object> row = db.execute(query, Map.of("id",id)).next();
		lastDegree = (long) row.get("deg")-(long) row.get("cntNotInTraining");;
		
		test.updateY(lastDegree);
		boolean rejected = test.nullHypothesisRejected();
		return !rejected;
	}
	
	public NonparametricUnpairedStatTest getTest() {
		return test;
	}
	
	@Override
	public void restore() {
		if (lastDegree != -1)
			test.restoreY(lastDegree);
	}

	@Override
	public String toString() {
		String predicate = "All";
		if (p != null)
			predicate = p;
		return "DegreeChecker; Predicate: "+predicate+"; Type: "+type+"; Test: "+test.getClass().getSimpleName()+"; Alpha: "+test.getAlpha();
	}

	@Override
	public String explanation() {
		// TODO Explanation?
		return "";
	}

}
