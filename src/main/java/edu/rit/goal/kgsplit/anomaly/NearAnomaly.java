package edu.rit.goal.kgsplit.anomaly;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;

public class NearAnomaly extends Anomaly {
	private Map<String, Double> sameByPredicate = new HashMap<>();
	
	public NearAnomaly(RelationshipType pred, String type) {
		super();
		this.pred = pred;
		this.type = type;
	}

	@Override
	public void compute(GraphDatabaseService db) {
		long total = 0, sameRegardless = 0;
		
		Result r = db.execute("MATCH (s)-[p]->(o) WHERE type(p)=$p RETURN COUNT(id(p)) AS cnt", Map.of("p", pred.name()));
		if (r.hasNext())
			total = (long) r.next().get("cnt");
		r.close();
		
		String patternToMatch = null;
		if (type.equals(NEARSAME))
			patternToMatch = "(s)-[p]->(o)<-[pp]-(s)";
		else if (type.equals(NEARREVERSE))
			patternToMatch = "(s)-[p]->(o)-[pp]->(s)";
		String queryCommon = "MATCH " + patternToMatch + " WHERE type(p)=$p AND type(p)<>type(pp) ";
		
		r = db.execute(queryCommon + " RETURN COUNT(DISTINCT id(p)) AS cnt", Map.of("p", pred.name()));
		if (r.hasNext())
			sameRegardless = (long) r.next().get("cnt");
		r.close();
		
		r = db.execute(queryCommon + "RETURN type(pp) AS type, COUNT(DISTINCT id(p)) AS cnt", Map.of("p", pred.name()));
		while (r.hasNext()) {
			Map<String, Object> row = r.next();
			sameByPredicate.put((String) row.get("type"), ((long) row.get("cnt"))*1.0);
		}
		r.close();
		
		result = sameRegardless*1.0/total;
		for (String other : sameByPredicate.keySet())
			sameByPredicate.put(other, sameByPredicate.get(other)/total);
	}

	@Override
	public String additionalInfo() {
		// Get predicate with largest value.
		return " Max value by predicate: " + getMaxByPredicate();
	}

	@Override
	public double getMaxByPredicate() {
		double max = .0;
		for (String other : sameByPredicate.keySet()) {
			double value = sameByPredicate.get(other);
			if (value > max)
				max = value;
		}
		return max;
	}

}
