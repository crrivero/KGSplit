package edu.rit.goal.kgsplit.split;

public enum Degree {
	INDEGREE, OUTDEGREE;
	
	public String getPropertyName(String p) {
		return this.name()+"_"+p;
	}
	
	public String getArrow(int pos) {
		String ret = "";
		if (pos==0 && this.equals(INDEGREE))
			ret = "<";
		if (pos==1 && this.equals(OUTDEGREE))
			ret = ">";
		return ret;
	}
}
