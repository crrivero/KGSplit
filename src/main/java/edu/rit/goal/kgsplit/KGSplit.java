package edu.rit.goal.kgsplit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import edu.rit.goal.kgsplit.checker.Checker;
import edu.rit.goal.kgsplit.split.Split;
import edu.rit.goal.kgsplit.split.Splitter;
import edu.rit.goal.kgsplit.split.SplitterReport;

public class KGSplit {

	public static void main(String[] args) {
		// Create the options for the command line.
		Options options = new Options();
		
		options.addOption("db", "dbfolder", true, "Folder of the database");
		options.getOption("db").setRequired(true);
		options.addOption(Option.builder("c").longOpt("checkers").desc("Checkers to evaluate; a list of the following: "
				+ "{Indeg/Outdeg}_{C|KS}_{All/Each}_alpha NonZero_{All/Each} Percentage_XX_{All/Each}").required().hasArgs().build());
		options.addOption("n", "name", true, "Name of the attribute to store the split information ('split' by default)");
		
		CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        String dbFolder = null;
        String[] checkersStr = null;
        String name = Split.SPLIT_ATTRIBUTE_NAME;
        
        try {
        	cmd = parser.parse(options, args);
            
            dbFolder = cmd.getOptionValue("db");
            checkersStr = cmd.getOptionValues("c");
            
            String nameStr = cmd.getOptionValue("n");
            if (nameStr != null)
            	name = nameStr;
        } catch (Exception e) {
            System.out.println(e.getMessage());	
            formatter.printHelp("utility-name", options);
            System.exit(-1);
        }
        
        try {
        	long before = System.nanoTime();
        	List<Checker> checkers = new ArrayList<>();
            GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbFolder));
            
            // Instantiate checkers.
            for (String chStr : checkersStr)
            	checkers.addAll(Checker.parse(chStr, db, false, name));
            
            Splitter.perform(db, checkers, name);
            
            long after = System.nanoTime();
            System.out.println("Split time: " + ((after-before)/1e9) + " secs");
            
            before = System.nanoTime();
            SplitterReport result = SplitterReport.compute(db, name);
            after = System.nanoTime();
            
            db.shutdown();
            
            System.out.println(result);
            System.out.println("Report time: " + ((after-before)/1e9) + " secs");
        } catch (Throwable oops) {
        	oops.printStackTrace();
        }
	}

}
