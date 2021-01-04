package edu.rit.goal.kgsplit.loader;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.unsafe.batchinsert.BatchRelationship;

import com.google.common.collect.Maps;

import edu.rit.goal.kgsplit.split.Split;

public class NumericLoader {
	public static LoaderReport load(String folder, String input, String sep, String triples, Split split, String name) throws Exception {
		LoaderReport report = new LoaderReport();
		
		Map<String, Object> splitProp = new HashMap<>();
		splitProp.put(name, split!=null?split.ordinal():Split.TRAINING.ordinal());
		
		Set<Long> uniqueNodes = new HashSet<>(), uniqueNodesCreated = new HashSet<>();
		Set<RelationshipType> predicates = new HashSet<>();
		int totalTriples = 0, triplesCreated = 0, triplesThatChangedSplit = 0;
		
		int subjectIdx = triples.indexOf("s"), objectIdx = triples.indexOf("o"), predIdx = triples.indexOf("p");
		BatchInserter inserter = BatchInserters.inserter(new File(folder));
		Scanner sc = new Scanner(new File(input));
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			String[] triple = line.split(sep);
			
			// Ignoring lines that are not triples.
			if (triple.length != 3)
				continue;
			
			long s = Long.valueOf(triple[subjectIdx]), o = Long.valueOf(triple[objectIdx]);
			RelationshipType p = RelationshipType.withName(triple[predIdx]);
			
			uniqueNodes.add(s);
			uniqueNodes.add(o);
			predicates.add(p);
			totalTriples++;
			
			if (!inserter.nodeExists(s)) {
				inserter.createNode(s, new HashMap<>(), Label.label("X"));
				uniqueNodesCreated.add(s);
			}
			
			if (!inserter.nodeExists(o)) {
				inserter.createNode(o, new HashMap<>(), Label.label("X"));
				uniqueNodesCreated.add(o);
			}
			
			BatchRelationship found = null;
			Iterator<BatchRelationship> it = inserter.getRelationships(s).iterator();
			while (found == null && it.hasNext()) {
				BatchRelationship r = it.next();
				if (r.getStartNode()==s && r.getEndNode()==o && r.getType().equals(p))
					found=r;
			}
			
			if (found == null) {
				inserter.createRelationship(s, o, p, splitProp);
				triplesCreated++;
			} else {
				Map<String, Object> foundProps = inserter.getRelationshipProperties(found.getId());
				Map<String, Object> currentSplit = new HashMap<>();
				if (foundProps.containsKey(name))
					currentSplit.put(name, foundProps.get(name));
				if (!Maps.difference(currentSplit, splitProp).areEqual())
					triplesThatChangedSplit++;
				inserter.setRelationshipProperties(found.getId(), splitProp);
			}
		}
		sc.close();
		inserter.shutdown();
		
		report.nodesCreated = uniqueNodesCreated.size();
		report.totalNodes = uniqueNodes.size();
		report.totalPredicates = predicates.size();
		report.totalTriples = totalTriples;
		report.triplesCreated = triplesCreated;
		report.triplesThatChangedSplit = triplesThatChangedSplit;
		
		return report;
	}
}
