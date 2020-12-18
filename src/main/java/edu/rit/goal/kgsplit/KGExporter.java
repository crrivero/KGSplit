package edu.rit.goal.kgsplit;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import edu.rit.goal.kgsplit.export.GraphMLExporter;
import edu.rit.goal.kgsplit.split.Split;

public class KGExporter {
	public enum OutputFormat {GraphML};
	
	public static void main(String[] args) {
		// Create the options for the command line.
		Options options = new Options();
		
		options.addOption("db", "dbfolder", true, "Folder of the database");
		options.getOption("db").setRequired(true);
		options.addOption("o", "output", true, "Output file to create");
		options.getOption("o").setRequired(true);
		options.addOption("f", "format", true, "Format of the output file "+Arrays.toString(OutputFormat.values()));
		options.getOption("f").setRequired(true);
		options.addOption("p", "split", true, "Training (0), Validation (1) or Test (2); "
				+ "use !{0|1|2} to refer to other splits, e.g., !0 means validation and test combined");
		options.addOption("r", "predicate", true, "Predicate to extract");
		options.addOption(Option.builder("n").longOpt("names").desc(
				"A list of names of the attributes to extract the split information ('split' by default)").hasArgs().build());
		
		CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        
        String dbFolder = null, outputFile = null;
        OutputFormat format = null;
        Split split = null;
        String selectedPredicate = null;
        String[] namesStr = null;
        boolean excludeSplit = false;

        try {
        	cmd = parser.parse(options, args);
            
            dbFolder = cmd.getOptionValue("db");
            outputFile = cmd.getOptionValue("o");
            format = OutputFormat.valueOf(cmd.getOptionValue("f"));
            namesStr = cmd.getOptionValues("n");
            if (namesStr == null)
            	namesStr = new String[] {Split.SPLIT_ATTRIBUTE_NAME};
            
            String pStr = cmd.getOptionValue("p");
            if (pStr != null) {
	            excludeSplit = pStr.startsWith("!");
	            split = Split.values()[Integer.valueOf(pStr.replace("!", ""))];
            }
            
        	String rStr = cmd.getOptionValue("r");
        	if (rStr != null)
        		selectedPredicate = rStr;
        } catch (Exception e) {
            System.out.println(e.getMessage());	
            formatter.printHelp("utility-name", options);
            System.exit(-1);
        }
        
        try {
        	long before = System.nanoTime();
        	GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbFolder));
            if (format.equals(OutputFormat.GraphML))
            	GraphMLExporter.export(db, new File(outputFile), split, excludeSplit, selectedPredicate, namesStr);
            long after = System.nanoTime();
            System.out.println("Time: " + ((after-before)/1e9) + " secs");
        } catch (Throwable oops) {
        	oops.printStackTrace();
        }
	}

}
