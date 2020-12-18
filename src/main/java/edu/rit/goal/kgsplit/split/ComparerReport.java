package edu.rit.goal.kgsplit.split;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class ComparerReport {
	protected String sourceProp, targetProp;
	protected Split sourceSplit, targetSplit;
	protected boolean excludeSourceSplit, excludeTargetSplit;
	
	protected ComparisonResult entity, predicate, triple;
	

	public ComparerReport() {
		super();
	}
	
	public static ComparerReport compute(GraphDatabaseService db, String sourceProp, Split sourceSplit, boolean excludeSourceSplit,
			String targetProp, Split targetSplit, boolean excludeTargetSplit) {
		ComparerReport report = new ComparerReport();
		
		report.sourceProp = sourceProp;
		report.targetProp = targetProp;
		report.sourceSplit = sourceSplit;
		report.targetSplit = targetSplit;
		report.excludeSourceSplit = excludeSourceSplit;
		report.excludeTargetSplit = excludeTargetSplit;
		
		Transaction tx = db.beginTx();
		
		Map<String, Object> params = Map.of("ss", sourceSplit.ordinal(), "ts", targetSplit.ordinal());
		
		// Entities
		String entityToMatch = "(n)-[p]-()", entityToCount = "DISTINCT(n)";
		ComparisonResult entityResult = compute(db, entityToMatch, entityToCount, 
				sourceProp, excludeSourceSplit, targetProp, excludeTargetSplit, params);
		report.entity = entityResult;
		
		// Predicates
		String predicateToMatch = "()-[p]->()", predicateToCount = "DISTINCT(type(p))";
		ComparisonResult predicateResult = compute(db, predicateToMatch, predicateToCount, 
				sourceProp, excludeSourceSplit, targetProp, excludeTargetSplit, params);
		report.predicate = predicateResult;
		
		// Triples
		String tripleToMatch = "()-[p]->()", tripleToCount = "id(p)";
		ComparisonResult tripleResult = compute(db, tripleToMatch, tripleToCount, 
				sourceProp, excludeSourceSplit, targetProp, excludeTargetSplit, params);
		report.triple = tripleResult;
		
		tx.close();
		
		return report;
	}
	
	private class ComparisonResult {
		long intersect, src, tgt, union;
		
		double overlap() {
			return intersect*1.0/Math.min(src, tgt);
		}
		
		double jaccard() {
			return intersect*1.0/union;
		}
	}
	
	private static ComparisonResult compute(GraphDatabaseService db, String toMatch, String toCount, 
			String sourceProp, boolean excludeSourceSplit, String targetProp, boolean excludeTargetSplit, Map<String, Object> params) {
		ComparisonResult ret = new ComparerReport().new ComparisonResult();
		ret.intersect = (long) db.execute("MATCH "+toMatch+" WHERE "+(excludeSourceSplit?" NOT " : "")+" p." + sourceProp + "=$ss AND "
				+ (excludeTargetSplit?" NOT " : "") + " p." + targetProp + "=$ts RETURN COUNT("+toCount+") AS cnt", params).next().get("cnt");
		ret.union = (long) db.execute("MATCH "+toMatch+" WHERE "+(excludeSourceSplit?" NOT " : "")+" p." + sourceProp + "=$ss OR "
				+ (excludeTargetSplit?" NOT " : "") + " p." + targetProp + "=$ts RETURN COUNT("+toCount+") AS cnt", params).next().get("cnt");
		ret.src = (long) db.execute("MATCH "+toMatch+" WHERE "+(excludeSourceSplit?" NOT " : "")+" p." + sourceProp + "=$ss "
				+ "RETURN COUNT("+toCount+") AS cnt", params).next().get("cnt");
		ret.tgt = (long) db.execute("MATCH "+toMatch+" WHERE "+(excludeTargetSplit?" NOT " : "")+" p." + targetProp + "=$ts "
				+ "RETURN COUNT("+toCount+") AS cnt", params).next().get("cnt");
		return ret;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
				
		buf.append("Comparing property: " + sourceProp);
		buf.append(" w.r.t.: " + targetProp);
		buf.append("\n");
		buf.append("Comparing split: " + getSplits(sourceSplit, excludeSourceSplit));
		buf.append(" w.r.t.: " + getSplits(targetSplit, excludeTargetSplit));
		buf.append("\n");
		
		ComparisonResult[] results = new ComparisonResult[] {entity, predicate, triple};
		
		for (int i = 0; i < results.length; i++) {
			String current = null;
			if (i == 0)
				current = "Entity";
			else if (i == 1)
				current = "Predicate";
			else if (i == 2)
				current = "Triple";
			
			buf.append(current + " source total: " + results[i].src);
			buf.append("\n");
			buf.append(current + " target total: " + results[i].tgt);
			buf.append("\n");
			buf.append(current + " intersect total: " + results[i].intersect);
			buf.append("\n");
			buf.append(current + " union total: " + results[i].union);
			buf.append("\n");
			buf.append(current + " overlap: " + results[i].overlap());
			buf.append("\n");
			buf.append(current + " Jaccard: " + results[i].jaccard());
			buf.append("\n");
		}
		
		return buf.toString();
	}
	
	private String getSplits(Split p, boolean exclude) {
		String ret = null;
		if (!exclude)
			ret = p.name();
		else {
			List<Split> list = new ArrayList<>(Arrays.asList(Split.values()));
			list.remove(p);
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < list.size(); i++) {
				buf.append(list.get(i).name());
				buf.append(", ");
			}
			ret = buf.substring(0, buf.length() - 2);
		}
		return ret;
	}
	
}
