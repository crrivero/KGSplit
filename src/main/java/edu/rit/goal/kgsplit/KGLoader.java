package edu.rit.goal.kgsplit;

import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import edu.rit.goal.kgsplit.loader.LoaderReport;
import edu.rit.goal.kgsplit.loader.NumericLoader;
import edu.rit.goal.kgsplit.split.Split;

public class KGLoader {
	public enum InputFormat {NUMERIC};
	
	public static void main(String[] args) {
		// Create the options for the command line.
		Options options = new Options();
		
		options.addOption("db", "dbfolder", true, "Folder of the database");
		options.getOption("db").setRequired(true);
		options.addOption("i", "input", true, "Input file to read");
		options.getOption("i").setRequired(true);
		options.addOption("f", "format", true, "Format of the input file "+Arrays.toString(InputFormat.values()));
		options.getOption("f").setRequired(true);
		options.addOption("t", "triples", true, "If numeric, how triples are represented: spo, sop, etc.");
		options.addOption("s", "separator", true, "If numeric, separator used to separate subject, predicate and object");
		options.addOption("p", "split", true, "Training (0), Validation (1) or Test (2)");
		options.addOption("n", "name", true, "Name of the attribute to store the split information ('split' by default)");
		
		CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        
        String dbFolder = null, inputFile = null, triples = null, sep = null;
        InputFormat format = null;
        Split split = null;
        String name = Split.SPLIT_ATTRIBUTE_NAME;

        try {
        	cmd = parser.parse(options, args);
            
            dbFolder = cmd.getOptionValue("db");
            inputFile = cmd.getOptionValue("i");
            format = InputFormat.valueOf(cmd.getOptionValue("f"));
            String nameStr = cmd.getOptionValue("n");
            if (nameStr != null)
            	name = nameStr;
            
            if (format.equals(InputFormat.NUMERIC)) {
            	triples = cmd.getOptionValue("t");
            	sep = cmd.getOptionValue("s");
            	
            	if (triples.length() != 3 || !triples.contains("s") || !triples.contains("p") || !triples.contains("o"))
            		throw new Exception("Triple format is not correct, use spo (or any other combination)");
            	
            	String splitStr = cmd.getOptionValue("p");
            	if (splitStr != null)
            		split = Split.values()[Integer.valueOf(splitStr)];
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());	
            formatter.printHelp("utility-name", options);
            System.exit(-1);
        }
        
        try {
        	long before = System.nanoTime();
        	LoaderReport report = null;
            if (format.equals(InputFormat.NUMERIC))
            	report = NumericLoader.load(dbFolder, inputFile, sep, triples, split, name);
            long after = System.nanoTime();
            
            System.out.println(report);
            System.out.println("Time: " + ((after-before)/1e9) + " secs");
        } catch (Throwable oops) {
        	oops.printStackTrace();
        }
	}

}
