package edu.rit.goal.kgsplit.anomaly;

import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;

public class CartesianProductAnomaly extends Anomaly {
	public CartesianProductAnomaly(RelationshipType pred) {
		super();
		this.pred = pred;
		this.type = CP;
	}

	@Override
	public void compute(GraphDatabaseService db) {
		long total = 0, uniqueSubjects = 0, uniqueObjects = 0;
		
		Result r = db.execute("MATCH (s)-[p]->(o) WHERE type(p)=$p RETURN COUNT(id(p)) AS cnt", Map.of("p", pred.name()));
		if (r.hasNext())
			total = (long) r.next().get("cnt");
		r.close();
		
		r = db.execute("MATCH (s)-[p]->() WHERE type(p)=$p RETURN COUNT(DISTINCT id(s)) AS cnt", Map.of("p", pred.name()));
		if (r.hasNext())
			uniqueSubjects = (long) r.next().get("cnt");
		r.close();
		
		r = db.execute("MATCH ()-[p]->(o) WHERE type(p)=$p RETURN COUNT(DISTINCT id(o)) AS cnt", Map.of("p", pred.name()));
		if (r.hasNext())
			uniqueObjects = (long) r.next().get("cnt");
		r.close();
		
		result = total*1.0/(uniqueSubjects*uniqueObjects);
	}

	@Override
	public String additionalInfo() {
		return null;
	}

	@Override
	public double getMaxByPredicate() {
		// There are no other predicates involved!
		return result;
	}

}
