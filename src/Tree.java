import java.util.*;
import java.io.*;

public class Tree {
    private List<ParseTreeNode> parsedTrees;
    private List<String> inputStrings;

    public Tree() { parsedTrees = new ArrayList<>(); inputStrings = new ArrayList<>(); }

    public void addTree(ParseTreeNode root, String inputString) { parsedTrees.add(root); inputStrings.add(inputString); }

    public void printAllTrees() {
        if (parsedTrees.isEmpty()) { System.out.println("\n[No parse trees generated - no strings were accepted]"); return; }
        System.out.println("\n" + "=".repeat(60)); System.out.println("  PARSE TREES"); System.out.println("=".repeat(60));
        for (int i = 0; i < parsedTrees.size(); i++) {
            System.out.println("\n  Input: \"" + inputStrings.get(i) + "\"");
            System.out.println("  " + "-".repeat(56));
            parsedTrees.get(i).printTree();
        }
        System.out.println("\n" + "=".repeat(60));
    }

    public void writeToFile(String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(filename));
        if (parsedTrees.isEmpty()) { pw.println("No parse trees generated - no strings were accepted."); }
        else {
            pw.println("PARSE TREES"); pw.println("=".repeat(60));
            for (int i = 0; i < parsedTrees.size(); i++) {
                pw.println("\nInput: \"" + inputStrings.get(i) + "\"");
                pw.println("-".repeat(56)); pw.print(parsedTrees.get(i).treeToString());
            }
        }
        pw.close();
    }
    public int getCount() { return parsedTrees.size(); }
}
