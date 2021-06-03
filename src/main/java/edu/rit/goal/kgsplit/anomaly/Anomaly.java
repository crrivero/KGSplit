package edu.rit.goal.kgsplit.anomaly;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;

public abstract class Anomaly {
	public static final String ALL = "All", NEARSAME = "NearSame", NEARREVERSE = "NearReverse", CP = "CartesianProduct";
	
	protected RelationshipType pred;
	protected double result;
	protected String type;
	
	public RelationshipType getPred() {
		return pred;
	}

	public double getMaxRegardlessOfPredicate() {
		return result;
	}

	public String getType() {
		return type;
	}

	// All NearSame NearReverse CartesianProduct
	public static List<Anomaly> parse(String str, RelationshipType pred) {
		List<Anomaly> ret = new ArrayList<>();
		if (str.equals(ALL)) {
			ret.addAll(parse(NEARSAME, pred));
			ret.addAll(parse(NEARREVERSE, pred));
			ret.addAll(parse(CP, pred));
		}
		if (str.equals(NEARSAME))
			ret.add(new NearAnomaly(pred, NEARSAME));
		if (str.equals(NEARREVERSE))
			ret.add(new NearAnomaly(pred, NEARREVERSE));
		if (str.equals(CP))
			ret.add(new CartesianProductAnomaly(pred));
		return ret;
	}
	
	public abstract void compute(GraphDatabaseService db);
	public abstract String additionalInfo();
	public abstract double getMaxByPredicate();

	@Override
	public String toString() {
		String otherInfo = additionalInfo();
		return "Anomaly type: " + type + "; Predicate: " + pred + "; Value: " + result + (otherInfo==null?"":"; "+otherInfo);
	}
}
