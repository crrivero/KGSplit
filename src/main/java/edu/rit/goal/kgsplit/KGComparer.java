package edu.rit.goal.kgsplit;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import edu.rit.goal.kgsplit.split.ComparerReport;
import edu.rit.goal.kgsplit.split.Split;

public class KGComparer {
	
	public static void main(String[] args) {
		// Create the options for the command line.
		Options options = new Options();
		
		options.addOption("db", "dbfolder", true, "Folder of the database");
		options.getOption("db").setRequired(true);
		
		options.addOption("ns", "source", true, "Source property to be analyzed");
		options.getOption("ns").setRequired(true);
		options.addOption("ps", "sourcesplit", true, "Source split to be analyzed (0, 1 or 2); "
				+ "use !{0|1|2} to refer to other splits, e.g., !0 means validation and test combined");
		options.getOption("ps").setRequired(true);
		
		options.addOption("nt", "target", true, "Target property to be analyzed");
		options.getOption("nt").setRequired(true);
		options.addOption("pt", "targetsplit", true, "Target split to be analyzed (0, 1 or 2); "
				+ "use !{0|1|2} to refer to other splits, e.g., !0 means validation and test combined");
		options.getOption("pt").setRequired(true);
		
		CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        
        String dbFolder = null;
        String sourceProp = null, targetProp = null;
        Split sourceSplit = null, targetSplit = null;
        boolean excludeSourceSplit = false, excludeTargetSplit = false;

        try {
        	cmd = parser.parse(options, args);
            
            dbFolder = cmd.getOptionValue("db");
            sourceProp = cmd.getOptionValue("ns");
            targetProp = cmd.getOptionValue("nt");
            
            String psStr = cmd.getOptionValue("ps");
            excludeSourceSplit = psStr.startsWith("!");
            sourceSplit = Split.values()[Integer.valueOf(psStr.replace("!", ""))];
            
            String ptStr = cmd.getOptionValue("pt");
            excludeTargetSplit = ptStr.startsWith("!");
            targetSplit = Split.values()[Integer.valueOf(ptStr.replace("!", ""))];
        } catch (Exception e) {
            System.out.println(e.getMessage());	
            formatter.printHelp("utility-name", options);
            System.exit(-1);
        }
        
        try {
        	long before = System.nanoTime();
        	GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbFolder));
        	ComparerReport report = ComparerReport.compute(db, sourceProp, sourceSplit, excludeSourceSplit, targetProp, targetSplit, excludeTargetSplit);
        	System.out.println(report);
            long after = System.nanoTime();
            System.out.println("Time: " + ((after-before)/1e9) + " secs");
        } catch (Throwable oops) {
        	oops.printStackTrace();
        }
	}

}
