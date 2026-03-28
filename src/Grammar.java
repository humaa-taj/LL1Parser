import java.util.*;
import java.io.*;

public class Grammar {
    private List<String> nonTerminals;
    private Map<String, List<List<String>>> productions;
    private String startSymbol;

    public Grammar() {
        nonTerminals = new ArrayList<>();
        productions = new LinkedHashMap<>();
    }

    public void readFromFile(String filename) throws IOException {
        nonTerminals.clear(); productions.clear();
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue;
            int arrowIdx = line.indexOf("->");
            if (arrowIdx < 0) continue;
            String lhs = line.substring(0, arrowIdx).trim();
            String rhsAll = line.substring(arrowIdx + 2).trim();
            if (!isNonTerminal(lhs)) continue;
            if (!productions.containsKey(lhs)) { nonTerminals.add(lhs); productions.put(lhs, new ArrayList<>()); }
            if (startSymbol == null) startSymbol = lhs;
            for (String alt : rhsAll.split("\\|")) {
                alt = alt.trim();
                List<String> symbols = new ArrayList<>();
                if (alt.equals("epsilon") || alt.equals("@")) { symbols.add("epsilon"); }
                else { for (String tok : alt.split("\\s+")) if (!tok.isEmpty()) symbols.add(tok); }
                productions.get(lhs).add(symbols);
            }
        }
        br.close();
    }

    public boolean isNonTerminal(String sym) { return sym != null && !sym.isEmpty() && Character.isUpperCase(sym.charAt(0)); }
    public boolean isTerminal(String sym) { return !isNonTerminal(sym) && !sym.equals("epsilon"); }
    public boolean isEpsilon(String sym) { return sym.equals("epsilon") || sym.equals("@"); }

    private String freshPrime(String base) {
        String candidate = base + "Prime";
        int suffix = 1;
        while (productions.containsKey(candidate)) { candidate = base + "Prime" + suffix; suffix++; }
        return candidate;
    }

    public void applyLeftFactoring() {
        boolean changed = true; int limit = 100;
        while (changed && limit-- > 0) {
            changed = false;
            for (String A : new ArrayList<>(nonTerminals)) if (leftFactorOne(A)) changed = true;
        }
    }

    private boolean leftFactorOne(String A) {
        List<List<String>> alts = productions.get(A);
        if (alts == null || alts.size() < 2) return false;
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < alts.size(); i++) {
            List<String> alt = alts.get(i);
            if (alt.isEmpty() || (alt.size()==1 && isEpsilon(alt.get(0)))) continue;
            groups.computeIfAbsent(alt.get(0), k -> new ArrayList<>()).add(i);
        }
        List<String> prefix = null; List<Integer> groupIdx = new ArrayList<>();
        for (Map.Entry<String,List<Integer>> e : groups.entrySet()) {
            if (e.getValue().size() >= 2) {
                groupIdx = e.getValue();
                prefix = longestCommonPrefix(alts, groupIdx);
                if (prefix != null && !prefix.isEmpty()) break;
            }
        }
        if (prefix == null || prefix.isEmpty()) return false;
        List<List<String>> newAlts = new ArrayList<>();
        for (int i = 0; i < alts.size(); i++) if (!groupIdx.contains(i)) newAlts.add(alts.get(i));
        String APrime = freshPrime(A);
        List<String> head = new ArrayList<>(prefix); head.add(APrime); newAlts.add(head);
        List<List<String>> primeAlts = new ArrayList<>();
        for (int idx : groupIdx) {
            List<String> suf = alts.get(idx).subList(prefix.size(), alts.get(idx).size());
            primeAlts.add(suf.isEmpty() ? Collections.singletonList("epsilon") : new ArrayList<>(suf));
        }
        productions.put(A, newAlts); nonTerminals.add(APrime); productions.put(APrime, primeAlts);
        return true;
    }

    private List<String> longestCommonPrefix(List<List<String>> alts, List<Integer> indices) {
        List<String> prefix = new ArrayList<>(alts.get(indices.get(0)));
        for (int i = 1; i < indices.size(); i++) {
            List<String> alt = alts.get(indices.get(i)); int len = 0;
            while (len < prefix.size() && len < alt.size() && prefix.get(len).equals(alt.get(len))) len++;
            prefix = prefix.subList(0, len);
        }
        return prefix;
    }

    public void removeLeftRecursion() {
        List<String> order = new ArrayList<>(nonTerminals);
        for (int i = 0; i < order.size(); i++) {
            String Ai = order.get(i);
            if (!productions.containsKey(Ai)) continue;
            for (int j = 0; j < i; j++) { String Aj = order.get(j); if (productions.containsKey(Aj)) substituteIndirect(Ai, Aj); }
            removeDirectLeftRecursion(Ai);
        }
    }

    private void substituteIndirect(String Ai, String Aj) {
        List<List<String>> aiP = productions.get(Ai), ajP = productions.get(Aj);
        if (aiP == null || ajP == null) return;
        List<List<String>> newP = new ArrayList<>();
        for (List<String> alt : aiP) {
            if (!alt.isEmpty() && alt.get(0).equals(Aj)) {
                List<String> alpha = alt.subList(1, alt.size());
                for (List<String> beta : ajP) {
                    List<String> na = new ArrayList<>();
                    if (!(beta.size()==1 && isEpsilon(beta.get(0)))) na.addAll(beta);
                    na.addAll(alpha); if (na.isEmpty()) na.add("epsilon"); newP.add(na);
                }
            } else newP.add(alt);
        }
        productions.put(Ai, newP);
    }

    private void removeDirectLeftRecursion(String A) {
        List<List<String>> alts = productions.get(A); if (alts == null) return;
        List<List<String>> rec = new ArrayList<>(), nonRec = new ArrayList<>();
        for (List<String> alt : alts) { if (!alt.isEmpty() && alt.get(0).equals(A)) rec.add(alt.subList(1, alt.size())); else nonRec.add(alt); }
        if (rec.isEmpty()) return;
        String AP = freshPrime(A);
        List<List<String>> newA = new ArrayList<>();
        for (List<String> beta : nonRec) {
            List<String> na = new ArrayList<>();
            if (!(beta.size()==1 && isEpsilon(beta.get(0)))) na.addAll(beta);
            na.add(AP); newA.add(na);
        }
        if (newA.isEmpty()) newA.add(Collections.singletonList(AP));
        List<List<String>> primeAlts = new ArrayList<>();
        for (List<String> alpha : rec) {
            List<String> na = new ArrayList<>();
            if (!(alpha.size()==1 && isEpsilon(alpha.get(0)))) na.addAll(alpha);
            na.add(AP); primeAlts.add(na);
        }
        primeAlts.add(Collections.singletonList("epsilon"));
        productions.put(A, newA); nonTerminals.add(AP); productions.put(AP, primeAlts);
    }

    public void printGrammar(String title) {
        System.out.println("\n" + "=".repeat(60)); System.out.println("  " + title); System.out.println("=".repeat(60));
        for (String nt : nonTerminals) {
            List<List<String>> alts = productions.get(nt); if (alts == null) continue;
            StringBuilder sb = new StringBuilder(); sb.append(String.format("  %-20s -> ", nt));
            for (int i = 0; i < alts.size(); i++) { if (i > 0) sb.append(" | "); sb.append(String.join(" ", alts.get(i))); }
            System.out.println(sb);
        }
        System.out.println("=".repeat(60));
    }

    public void writeGrammarToFile(String filename, String title) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(filename));
        pw.println(title); pw.println("=".repeat(60));
        for (String nt : nonTerminals) {
            List<List<String>> alts = productions.get(nt); if (alts == null) continue;
            StringBuilder sb = new StringBuilder(); sb.append(String.format("%-20s -> ", nt));
            for (int i = 0; i < alts.size(); i++) { if (i > 0) sb.append(" | "); sb.append(String.join(" ", alts.get(i))); }
            pw.println(sb);
        }
        pw.println("=".repeat(60)); pw.close();
    }

    public List<String> getNonTerminals() { return nonTerminals; }
    public Map<String, List<List<String>>> getProductions() { return productions; }
    public String getStartSymbol() { return startSymbol; }
    public void setStartSymbol(String s) { startSymbol = s; }

    public Set<String> getTerminals() {
        Set<String> t = new LinkedHashSet<>();
        for (List<List<String>> alts : productions.values()) for (List<String> alt : alts) for (String s : alt) if (isTerminal(s)) t.add(s);
        return t;
    }
}
