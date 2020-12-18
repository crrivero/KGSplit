package edu.rit.goal.kgsplit.loader;

public class LoaderReport {
	protected int totalNodes, nodesCreated, totalTriples, triplesCreated, totalPredicates, triplesThatChangedSplit;

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		
		buf.append("Total triples processed: ");
		buf.append(totalTriples);
		buf.append("\n");
		
		buf.append("Total triples created: ");
		buf.append(triplesCreated);
		buf.append("\n");
		
		buf.append("Total triples that were moved to another split: ");
		buf.append(triplesThatChangedSplit);
		buf.append("\n");
		
		buf.append("Total nodes processed: ");
		buf.append(totalNodes);
		buf.append("\n");
		
		buf.append("Total nodes created: ");
		buf.append(nodesCreated);
		buf.append("\n");
		
		buf.append("Total predicates: ");
		buf.append(totalPredicates);
		buf.append("\n");
		
		return buf.toString();
	}
	
}
