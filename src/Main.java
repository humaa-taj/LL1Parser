import java.util.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        printBanner();
        String grammarFile = args.length>=1 ? args[0] : "input/grammar2.txt";
        String inputFile   = args.length>=2 ? args[1] : "input/input_valid.txt";
        System.out.println("  Grammar file : " + grammarFile);
        System.out.println("  Input file   : " + inputFile);

        String grammarBase = new File(grammarFile).getName().replace(".txt","");
        String inputBase   = new File(inputFile).getName().replace(".txt","");
        String traceFile = "output/parsing_trace_"+grammarBase+"_"+inputBase+".txt";
        String treeFile  = "output/parse_trees_"+grammarBase+"_"+inputBase+".txt";
        String tableFile = "output/parsing_table_"+grammarBase+".txt";
        String ffFile    = "output/first_follow_sets_"+grammarBase+".txt";
        String transFile = "output/grammar_transformed_"+grammarBase+".txt";

        Grammar grammar = new Grammar();
        try { grammar.readFromFile(grammarFile); } catch(IOException e){ System.err.println("Cannot read grammar: "+e.getMessage()); return; }
        grammar.printGrammar("ORIGINAL GRAMMAR");
        grammar.applyLeftFactoring();
        grammar.printGrammar("AFTER LEFT FACTORING");
        grammar.removeLeftRecursion();
        grammar.printGrammar("AFTER LEFT RECURSION REMOVAL");
        try { grammar.writeGrammarToFile(transFile,"TRANSFORMED GRAMMAR (after LF + LRR)"); } catch(IOException e){ System.err.println("Warning: "+e.getMessage()); }

        FirstFollow ff = new FirstFollow(grammar);
        ff.computeFirst(); ff.computeFollow();
        ff.printFirstSets(); ff.printFollowSets();
        try { ff.writeToFile(ffFile); } catch(IOException e){ System.err.println("Warning: "+e.getMessage()); }

        Parser parser = new Parser(grammar, ff);
        parser.buildTable(); parser.printTable();
        try { parser.writeTableToFile(tableFile); } catch(IOException e){ System.err.println("Warning: "+e.getMessage()); }
        if (!parser.isLL1()) System.out.println("\n  [Warning] Grammar is NOT LL(1). Parsing may be unreliable.");

        Tree treeObj = new Tree();
        try { parser.parseFile(inputFile, traceFile, treeObj); } catch(IOException e){ System.err.println("Cannot parse input: "+e.getMessage()); }
        treeObj.printAllTrees();
        try { treeObj.writeToFile(treeFile); } catch(IOException e){ System.err.println("Warning: "+e.getMessage()); }

        System.out.println("\n"+"=".repeat(60));
        System.out.println("  Processing complete. Output files written to: output/");
        System.out.println("=".repeat(60)+"\n");
    }

    private static void printBanner(){
        System.out.println("\n"+"=".repeat(60));
        System.out.println("  CS4031 - Compiler Construction  |  Assignment 02");
        System.out.println("  LL(1) Parser - Spring 2026");
        System.out.println("  Authors: 23i0743  |  23i0623");
        System.out.println("=".repeat(60)+"\n");
    }
}
