package edu.rit.goal.kgsplit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import edu.rit.goal.kgsplit.anomaly.Anomaly;

public class KGAnomalyAnalyzer {

	public static void main(String[] args) {
		// Create the options for the command line.
		Options options = new Options();

		options.addOption("db", "dbfolder", true, "Folder of the database");
		options.getOption("db").setRequired(true);
		options.addOption(Option.builder("c").longOpt("anomalies")
				.desc("Anomalies to analyze; a list of the following: " + "All NearSame NearReverse CartesianProduct")
				.required().hasArgs().build());

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		String dbFolder = null;
		String[] anomaliesStr = null;

		try {
			cmd = parser.parse(options, args);

			dbFolder = cmd.getOptionValue("db");
			anomaliesStr = cmd.getOptionValues("c");
		} catch (Exception e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);
			System.exit(-1);
		}

		try {
			long before = System.nanoTime();
			List<Anomaly> anomalies = new ArrayList<>();
			GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbFolder));

			Transaction tx = db.beginTx();
			
			long totalPredicates = db.getAllRelationshipTypes().stream().count();

			// Instantiate anomalies.
			for (String aStr : anomaliesStr)
				for (RelationshipType pred : db.getAllRelationshipTypes())
					anomalies.addAll(Anomaly.parse(aStr, pred));

			for (Anomaly anom : anomalies)
				anom.compute(db);

			tx.close();

			long after = System.nanoTime();
			System.out.println("Analysis time: " + ((after - before) / 1e9) + " secs");

			// Group by anomaly type.
			Map<String, List<Anomaly>> byType = new HashMap<>();
			for (Anomaly anom : anomalies) {
				String type = anom.getType();
				if (!byType.containsKey(type))
					byType.put(type, new ArrayList<>());
				byType.get(type).add(anom);
			}

			System.out.println("Anomalies report: ");
			for (String type : byType.keySet())
				for (Anomaly anom : byType.get(type))
					System.out.println(anom.toString());
			
			Map<RelationshipType, Double> anomalyRegardless = new HashMap<>(),
					anomalyByPredicate = new HashMap<>();
			Map<RelationshipType, String> typesRegardless = new HashMap<>(),
					typesByPredicate = new HashMap<>();
			for (Anomaly anom : anomalies) {
				RelationshipType p = anom.getPred();
				if (!anomalyRegardless.containsKey(p))
					anomalyRegardless.put(p, .0);
				if (!anomalyByPredicate.containsKey(p))
					anomalyByPredicate.put(p, .0);
				
				if (anom.getMaxRegardlessOfPredicate() > anomalyRegardless.get(p)) {
					anomalyRegardless.put(p, anom.getMaxRegardlessOfPredicate());
					typesRegardless.put(p, anom.getType());
				}
				
				if (anom.getMaxByPredicate() > anomalyByPredicate.get(p)) {
					anomalyByPredicate.put(p, anom.getMaxByPredicate());
					typesByPredicate.put(p, anom.getType());
				}
			}
			
			Map<Integer, List<RelationshipType>> quartilesRegardless = getQuartiles(anomalyRegardless),
					quartilesByPredicate = getQuartiles(anomalyByPredicate);
			
			System.out.println("Quartile information regardless of predicate: ");
			for (Integer q : quartilesRegardless.keySet())
				System.out.println("\tQ"+q+": "+(quartilesRegardless.get(q).size()*1.0/totalPredicates));
			
			System.out.println("Quartile information by predicate: ");
			for (Integer q : quartilesByPredicate.keySet())
				System.out.println("\tQ"+q+": "+(quartilesByPredicate.get(q).size()*1.0/totalPredicates));
			
			System.out.println("Total predicates: " + totalPredicates);
			
			Map<String, AtomicInteger> typeHistogramRegardless = getTypeHistogram(typesRegardless),
					typeHistogramByPredicate = getTypeHistogram(typesByPredicate);
			
			System.out.println("Anomaly type histogram regardless of predicate: ");
			System.out.println(typeHistogramRegardless);
			
			System.out.println("Anomaly type histogram by predicate: ");
			System.out.println(typeHistogramByPredicate);
			
			db.shutdown();
		} catch (Throwable oops) {
			oops.printStackTrace();
		}
	}
	
	private static Map<String, AtomicInteger> getTypeHistogram(Map<RelationshipType, String> types) {
		Map<String, AtomicInteger> ret = new HashMap<>();
		
		for (RelationshipType p : types.keySet()) {
			if (!ret.containsKey(types.get(p)))
				ret.put(types.get(p), new AtomicInteger());
			ret.get(types.get(p)).incrementAndGet();
		}
		
		return ret;
	}
	
	private static Map<Integer, List<RelationshipType>> getQuartiles(Map<RelationshipType, Double> anomalies) {
		Map<Integer, List<RelationshipType>> ret = new HashMap<>();
		for (RelationshipType p : anomalies.keySet()) {
			Integer q = null;
			if (anomalies.get(p) > .75)
				q = 1;
			else if (anomalies.get(p) > .5)
				q = 2;
			else if (anomalies.get(p) > .25)
				q = 3;
			else
				q = 4;
			if (!ret.containsKey(q))
				ret.put(q, new ArrayList<>());
			ret.get(q).add(p);
		}
		return ret;
	}

}
