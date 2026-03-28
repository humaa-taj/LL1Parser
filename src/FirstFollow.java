import java.util.*;
import java.io.*;

public class FirstFollow {
    private Grammar grammar;
    private Map<String, Set<String>> firstSets;
    private Map<String, Set<String>> followSets;

    public FirstFollow(Grammar grammar) {
        this.grammar = grammar;
        firstSets = new LinkedHashMap<>();
        followSets = new LinkedHashMap<>();
    }

    public void computeFirst() {
        for (String nt : grammar.getNonTerminals()) firstSets.put(nt, new LinkedHashSet<>());
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String A : grammar.getNonTerminals()) {
                List<List<String>> alts = grammar.getProductions().get(A); if (alts == null) continue;
                for (List<String> alt : alts) {
                    Set<String> toAdd = firstOfString(alt); int before = firstSets.get(A).size();
                    firstSets.get(A).addAll(toAdd); if (firstSets.get(A).size() != before) changed = true;
                }
            }
        }
    }

    public Set<String> firstOfString(List<String> symbols) {
        Set<String> result = new LinkedHashSet<>();
        if (symbols.isEmpty()) { result.add("epsilon"); return result; }
        boolean allEps = true;
        for (String sym : symbols) {
            if (grammar.isEpsilon(sym)) { result.add("epsilon"); break; }
            else if (grammar.isTerminal(sym)) { result.add(sym); allEps = false; break; }
            else {
                Set<String> fs = firstSets.getOrDefault(sym, new LinkedHashSet<>());
                for (String s : fs) if (!s.equals("epsilon")) result.add(s);
                if (!fs.contains("epsilon")) { allEps = false; break; }
            }
        }
        if (allEps) result.add("epsilon");
        return result;
    }

    public void computeFollow() {
        for (String nt : grammar.getNonTerminals()) followSets.put(nt, new LinkedHashSet<>());
        followSets.get(grammar.getStartSymbol()).add("$");
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String A : grammar.getNonTerminals()) {
                List<List<String>> alts = grammar.getProductions().get(A); if (alts == null) continue;
                for (List<String> alt : alts) {
                    for (int i = 0; i < alt.size(); i++) {
                        String B = alt.get(i); if (!grammar.isNonTerminal(B)) continue;
                        List<String> beta = alt.subList(i+1, alt.size());
                        Set<String> fb = firstOfString(beta);
                        int before = followSets.get(B).size();
                        for (String s : fb) if (!s.equals("epsilon")) followSets.get(B).add(s);
                        if (fb.contains("epsilon")) followSets.get(B).addAll(followSets.get(A));
                        if (followSets.get(B).size() != before) changed = true;
                    }
                }
            }
        }
    }

    public void printFirstSets() {
        System.out.println("\n" + "=".repeat(60)); System.out.println("  FIRST Sets"); System.out.println("=".repeat(60));
        System.out.printf("  %-20s | %s%n", "Non-Terminal", "FIRST Set"); System.out.println("  " + "-".repeat(58));
        for (String nt : grammar.getNonTerminals()) System.out.printf("  %-20s | { %s }%n", nt, String.join(", ", firstSets.getOrDefault(nt, new LinkedHashSet<>())));
        System.out.println("=".repeat(60));
    }

    public void printFollowSets() {
        System.out.println("\n" + "=".repeat(60)); System.out.println("  FOLLOW Sets"); System.out.println("=".repeat(60));
        System.out.printf("  %-20s | %s%n", "Non-Terminal", "FOLLOW Set"); System.out.println("  " + "-".repeat(58));
        for (String nt : grammar.getNonTerminals()) System.out.printf("  %-20s | { %s }%n", nt, String.join(", ", followSets.getOrDefault(nt, new LinkedHashSet<>())));
        System.out.println("=".repeat(60));
    }

    public void writeToFile(String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(filename));
        pw.println("FIRST Sets"); pw.println("=".repeat(60));
        pw.printf("%-20s | %s%n", "Non-Terminal", "FIRST Set"); pw.println("-".repeat(58));
        for (String nt : grammar.getNonTerminals()) pw.printf("%-20s | { %s }%n", nt, String.join(", ", firstSets.getOrDefault(nt, new LinkedHashSet<>())));
        pw.println(); pw.println("FOLLOW Sets"); pw.println("=".repeat(60));
        pw.printf("%-20s | %s%n", "Non-Terminal", "FOLLOW Set"); pw.println("-".repeat(58));
        for (String nt : grammar.getNonTerminals()) pw.printf("%-20s | { %s }%n", nt, String.join(", ", followSets.getOrDefault(nt, new LinkedHashSet<>())));
        pw.close();
    }

    public Map<String, Set<String>> getFirstSets() { return firstSets; }
    public Map<String, Set<String>> getFollowSets() { return followSets; }
    public Set<String> getFirst(String nt) { return firstSets.getOrDefault(nt, Collections.emptySet()); }
    public Set<String> getFollow(String nt) { return followSets.getOrDefault(nt, Collections.emptySet()); }
}
