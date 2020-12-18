package edu.rit.goal.kgsplit.checker;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import edu.rit.goal.kgsplit.split.Degree;
import edu.rit.goal.kgsplit.stat.Cucconi;
import edu.rit.goal.kgsplit.stat.KolmogorovSmirnov;
import edu.rit.goal.kgsplit.stat.NonparametricUnpairedStatTest;

public abstract class Checker {
	public static final String INDEG = "Indeg", OUTDEG = "Outdeg", KS = "KS", ALL = "All", EACH = "Each", NONZERO = "NonZero",
			PERCENTAGE = "Percentage";
	
	public abstract boolean accepted();
	public abstract boolean accepted(long tid, long s, String p, long o);
	public abstract void restore();
	public abstract String explanation();
	
	// {Indeg/Outdeg}_{C|KS}_{All/Each}_alpha NonZero_{All/Each}
	public static List<Checker> parse(String str, GraphDatabaseService db, boolean verify, String name) {
		List<Checker> ret = new ArrayList<>();
		String[] split = str.split("\\_");
		
		if (split[0].equals(INDEG) || split[0].equals(OUTDEG)) {
			for (String p : getPredicates(split[2], db)) {
				Degree type = null;
				if (split[0].equals(INDEG))
					type = Degree.INDEGREE;
				if (split[0].equals(OUTDEG))
					type = Degree.OUTDEGREE;
				
				NonparametricUnpairedStatTest test = null;
				if (split[1].equals("KS"))
					test = new KolmogorovSmirnov(Double.valueOf(split[3]), verify);
				if (split[1].equals("C"))
					test = new Cucconi(Double.valueOf(split[3]), verify);
				
				ret.add(new DegreeChecker(db, p, test, type, name));
			}
		} else if (split[0].equals(NONZERO))
			for (String p : getPredicates(split[1], db))
				ret.add(new NonZeroDegreeChecker(db, p, name));
		else if (split[0].equals(PERCENTAGE)) {
			int perc = Integer.valueOf(split[1]);
			for (String p : getPredicates(split[2], db))
				ret.add(new PercentageChecker(db, p, perc*1.0/100, name));
		} else
			throw new Error("Checker not recognized: " + str);
		
		return ret;
	}
	
	private static List<String> getPredicates(String str, GraphDatabaseService db) {
		List<String> preds = new ArrayList<>();
		if (str.equals(ALL))
			preds.add(null);
		if (str.equals(EACH)) {
			Transaction tx = db.beginTx();
			for (RelationshipType type : db.getAllRelationshipTypes())
				preds.add(type.name());
			tx.close();
		}
		return preds;
	}
}
