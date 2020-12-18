package edu.rit.goal.kgsplit.export;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import edu.rit.goal.kgsplit.split.Split;

public class GraphMLExporter {
	public static void export(GraphDatabaseService db, File output, Split split, boolean excludeSplit, String predicate, String[] names) throws Exception {
		PrintWriter writer = new PrintWriter(output);
		
		// Header.
		writer.println(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
				"<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"  \n" + 
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" + 
				"    xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns \n" + 
				"     http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">");
		
		RelationshipType predType = null;
		if (predicate != null)
			predType = RelationshipType.withName(predicate);
		
		// Declaring split attributes.
		for (String name : names)
			writer.println("<key id=\""+name+"\" for=\"edge\" attr.name=\""+name+"\" attr.type=\"int\" />");
		// Declaring attribute: predicate.
		writer.println("<key id=\"predicate\" for=\"edge\" attr.name=\"predicate\" attr.type=\"string\" />");
		
		// Start graph.
		writer.println("<graph id=\"G\" edgedefault=\"directed\">");
		Transaction tx = db.beginTx();
		
		// Nodes first.
		Result res = db.execute("MATCH (n) RETURN n ORDER BY id(n)");
		while (res.hasNext()) {
			Node n = (Node) res.next().get("n"); 
			if (includeNode(n, predType, names, split, excludeSplit))
				writer.println("<node id=\""+n.getId()+"\"/>");
		}
		res.close();
		
		// Edges.
		res = db.execute("MATCH ()-[r]->() RETURN r ORDER BY id(r)");
		while (res.hasNext()) {
			Relationship triple = (Relationship)res.next().get("r");
			if (!includeTriple(triple, predType, names, split, excludeSplit))
				continue;
			writer.println("<edge id=\""+triple.getId()+"\" source=\""+triple.getStartNodeId()+"\" "
					+ "target=\""+triple.getEndNodeId()+"\">");
			writer.println("<data key=\"predicate\">"+triple.getType().name()+"</data>");
			for (String name : names)
				if (includeSplit(triple, predType, name, split, excludeSplit))
					writer.println("<data key=\""+name+"\">"+triple.getProperty(name)+"</data>");
			writer.println("</edge>");
		}
		res.close();
		
		tx.close();
		writer.println("</graph>");
		writer.println("</graphml>");
		writer.close();
	}
	
	private static boolean includeNode(Node n, RelationshipType predType, String[] names, Split split, boolean excludeSplit) {
		boolean ret = false;
		Iterator<Relationship> it = n.getRelationships().iterator();
		while (!ret && it.hasNext()) {
			Relationship triple = it.next();
			ret = includeTriple(triple, predType, names, split, excludeSplit);
		}
		return ret;
	}
	
	private static boolean includeTriple(Relationship triple, RelationshipType predType, String[] names, Split split, boolean excludeSplit) {
		boolean ret = true;
		if (predType != null || split != null) {
			boolean includeAccordingToPred = true;
			if (predType != null)
				includeAccordingToPred = triple.getType().equals(predType);
			
			boolean includeAccordingToSplit = true;
			if (includeAccordingToPred && split != null) {
				includeAccordingToSplit = false;
				for (int i = 0; !includeAccordingToSplit && i < names.length; i++) 
					includeAccordingToSplit = includeSplit(triple, predType, names[i], split, excludeSplit);
			}
			
			ret = includeAccordingToPred && includeAccordingToSplit;
		}
		return ret;
	}
	
	private static boolean includeSplit(Relationship triple, RelationshipType predType, String name, Split split, boolean excludeSplit) {
		boolean ret = true;
		if (split != null) {
			ret = false;
				
			if (triple.hasProperty(name)) {
				int splitOfTriple = ((int) triple.getProperty(name));
				ret = ((!excludeSplit && split.ordinal() == splitOfTriple) || (excludeSplit && split.ordinal() != splitOfTriple));
			}
		}
		return ret;
	}
	
}
