import java.util.*;
import java.io.*;

public class Parser {
    private Grammar grammar;
    private FirstFollow ff;
    private Map<String, Map<String, List<String>>> table;
    private boolean isLL1;
    private List<String> conflicts;

    public Parser(Grammar grammar, FirstFollow ff) {
        this.grammar   = grammar;
        this.ff        = ff;
        this.table     = new LinkedHashMap<>();
        this.conflicts = new ArrayList<>();
        this.isLL1     = true;
    }

    // ---------------------------------------------------------------
    // Table construction
    // ---------------------------------------------------------------

    public void buildTable() {
        for (String nt : grammar.getNonTerminals())
            table.put(nt, new LinkedHashMap<>());

        for (String A : grammar.getNonTerminals()) {
            List<List<String>> alts = grammar.getProductions().get(A);
            if (alts == null) continue;
            for (List<String> alpha : alts) {
                Set<String> fa = ff.firstOfString(alpha);
                for (String a : fa)
                    if (!a.equals("epsilon")) addToTable(A, a, alpha);
                if (fa.contains("epsilon"))
                    for (String b : ff.getFollow(A)) addToTable(A, b, alpha);
            }
        }
    }

    private void addToTable(String nt, String terminal, List<String> production) {
        Map<String, List<String>> row = table.computeIfAbsent(nt, k -> new LinkedHashMap<>());
        if (row.containsKey(terminal)) {
            isLL1 = false;
            String c = "CONFLICT at M[" + nt + ", " + terminal + "]: "
                     + nt + " -> " + String.join(" ", row.get(terminal))
                     + "  AND  " + nt + " -> " + String.join(" ", production);
            if (!conflicts.contains(c)) { conflicts.add(c); System.err.println("[Parser] " + c); }
        } else {
            row.put(terminal, new ArrayList<>(production));
        }
    }

    // ---------------------------------------------------------------
    // Table display
    // ---------------------------------------------------------------

    public void printTable() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  LL(1) PARSING TABLE");
        System.out.println("=".repeat(80));
        Set<String> terms = collectTerminals();
        System.out.printf("  %-20s", "Non-Terminal");
        for (String t : terms) System.out.printf(" | %-18s", t);
        System.out.println(" |");
        System.out.println("  " + "-".repeat(20 + terms.size() * 21));
        for (String nt : grammar.getNonTerminals()) {
            System.out.printf("  %-20s", nt);
            Map<String, List<String>> row = table.getOrDefault(nt, new LinkedHashMap<>());
            for (String t : terms) {
                if (row.containsKey(t)) {
                    String p = nt + "->" + String.join(" ", row.get(t));
                    if (p.length() > 17) p = p.substring(0, 14) + "...";
                    System.out.printf(" | %-18s", p);
                } else {
                    System.out.printf(" | %-18s", "");
                }
            }
            System.out.println(" |");
        }
        System.out.println("=".repeat(80));
        System.out.println("  Grammar is " + (isLL1 ? "LL(1)." : "NOT LL(1) (conflicts exist)."));
        if (!isLL1) for (String c : conflicts) System.out.println("    " + c);
    }

    public void writeTableToFile(String filename) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(filename, true));
        pw.println("\nLL(1) PARSING TABLE");
        pw.println("=".repeat(80));
        Set<String> terms = collectTerminals();
        pw.printf("%-20s", "Non-Terminal");
        for (String t : terms) pw.printf(" | %-18s", t);
        pw.println(" |");
        pw.println("-".repeat(20 + terms.size() * 21));
        for (String nt : grammar.getNonTerminals()) {
            pw.printf("%-20s", nt);
            Map<String, List<String>> row = table.getOrDefault(nt, new LinkedHashMap<>());
            for (String t : terms) {
                if (row.containsKey(t)) {
                    String p = nt + "->" + String.join(" ", row.get(t));
                    if (p.length() > 17) p = p.substring(0, 14) + "...";
                    pw.printf(" | %-18s", p);
                } else {
                    pw.printf(" | %-18s", "");
                }
            }
            pw.println(" |");
        }
        pw.println("=".repeat(80));
        pw.println("Grammar is " + (isLL1 ? "LL(1)." : "NOT LL(1) (conflicts exist)."));
        if (!isLL1) for (String c : conflicts) pw.println("  " + c);
        pw.close();
    }

    private Set<String> collectTerminals() {
        Set<String> t = new LinkedHashSet<>();
        for (Map<String, List<String>> row : table.values()) t.addAll(row.keySet());
        t.add("$");
        return t;
    }

    // ---------------------------------------------------------------
    // Parse a file of input strings
    // ---------------------------------------------------------------

    public void parseFile(String inputFile, String traceFile, Tree treeObj) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        PrintWriter    tw = new PrintWriter(new FileWriter(traceFile));
        String line; int lineNum = 0;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue;
            lineNum++;
            System.out.println("\n" + "=".repeat(80));
            System.out.println("  Parsing String #" + lineNum + ": \"" + line + "\"");
            System.out.println("=".repeat(80));
            tw.println("\n" + "=".repeat(80));
            tw.println("  Parsing String #" + lineNum + ": \"" + line + "\"");
            tw.println("=".repeat(80));
            ParseTreeNode root = parseString(line, lineNum, tw);
            if (root != null) treeObj.addTree(root, line);
        }
        br.close(); tw.close();
    }

    // ---------------------------------------------------------------
    // Core LL(1) parsing loop — Task 2.3 + 2.4
    // ---------------------------------------------------------------

    public ParseTreeNode parseString(String inputLine, int lineNum, PrintWriter tw) {

        // Tokenise
        List<String> tokens = new ArrayList<>();
        for (String t : inputLine.trim().split("\\s+"))
            if (!t.isEmpty()) tokens.add(t);
        tokens.add("$");

        // Error handler with correct line number
        ErrorHandler err = new ErrorHandler(grammar, ff.getFollowSets());
        err.setLineNumber(lineNum);

        // Stack initialisation
        Stack<String>        stack     = new Stack<>();
        Stack<ParseTreeNode> nodeStack = new Stack<>();
        stack.push("$");
        stack.push(grammar.getStartSymbol());
        ParseTreeNode root = new ParseTreeNode(grammar.getStartSymbol(), false);
        nodeStack.push(null);   // sentinel below root
        nodeStack.push(root);

        int[] pos  = {0};
        int   step = 0;

        // Trace header
        String header = String.format("  %-5s | %-30s | %-25s | %s",
                                      "Step", "Stack (bottom->top)",
                                      "Input Remaining", "Action");
        String div = "  " + "-".repeat(105);
        System.out.println(header); System.out.println(div);
        if (tw != null) { tw.println(header); tw.println(div); }

        boolean accepted = false;

        // ---- Main loop --------------------------------------------------------
        outerLoop:
        while (true) {
            step++;
            String X = stack.top();
            String a = (pos[0] < tokens.size()) ? tokens.get(pos[0]) : "$";
            int    col = pos[0] + 1;   // 1-based column

            String stackStr = stack.toStringBottomToTop();
            String inputStr = remainingInput(tokens, pos[0]);
            String action;

            // ---- ACCEPT -------------------------------------------------------
            if (X.equals("$") && a.equals("$")) {
                action = "Accept";
                printStep(step, stackStr, inputStr, action, tw);
                accepted = true;
                break;
            }

            // ---- MATCH (terminal on top == current token) ----------------------
            if (!X.equals("$") && grammar.isTerminal(X) && X.equals(a)) {
                action = "Match '" + a + "'";
                printStep(step, stackStr, inputStr, action, tw);
                stack.pop(); nodeStack.pop(); pos[0]++;
                continue;
            }

            // ---- EXPAND (non-terminal) ----------------------------------------
            if (grammar.isNonTerminal(X)) {
                Map<String, List<String>> row = table.getOrDefault(X, Collections.emptyMap());
                List<String> prod = row.get(a);

                if (prod == null) {
                    // ---- ERROR: empty table entry ----------------------------
                    action = "ERROR [Line " + lineNum + ", Col " + col
                           + "]: No production for [" + X + ", " + a + "]"
                           + "  Expected one of: { " + followTokensFor(X) + " }";
                    printStep(step, stackStr, inputStr, action, tw);
                    err.reportError("EMPTY_ENTRY", X, a, step, col, tw);

                    // Panic-mode recovery — does NOT abort
                    boolean recovered = err.panicModeRecover(
                            stack, nodeStack, tokens, pos, step, tw);
                    if (!recovered) break outerLoop;
                    // Resume: re-read X, a at top of loop
                    continue;
                }

                // Normal expand
                action = X + " -> " + String.join(" ", prod);
                printStep(step, stackStr, inputStr, action, tw);
                stack.pop();
                ParseTreeNode cur = nodeStack.pop();

                if (!(prod.size() == 1 && prod.get(0).equals("epsilon"))) {
                    // Push children in reverse order
                    for (int i = prod.size() - 1; i >= 0; i--) {
                        String sym = prod.get(i);
                        boolean isTerm = grammar.isTerminal(sym) || grammar.isEpsilon(sym);
                        ParseTreeNode child = new ParseTreeNode(sym, isTerm);
                        if (cur != null) cur.addChild(child);
                        stack.push(sym);
                        nodeStack.push(child);
                    }
                    if (cur != null && !cur.getChildren().isEmpty())
                        Collections.reverse(cur.getChildren());
                } else {
                    if (cur != null) cur.addChild(new ParseTreeNode("epsilon", true));
                }
                continue;
            }

            // ---- TERMINAL ERRORS ---------------------------------------------

            // Extra input: stack has only $, but input remains
            if (X.equals("$") && !a.equals("$")) {
                action = "ERROR [Line " + lineNum + ", Col " + col
                       + "]: Extra input '" + a + "' after parse end. Expected: '$'";
                printStep(step, stackStr, inputStr, action, tw);
                err.reportError("EXTRA_INPUT", "$", a, step, col, tw);
                // Skip remaining input
                pos[0] = tokens.size();
                break;
            }

            // Premature end: expected terminal but input is $
            if (!X.equals("$") && a.equals("$")) {
                action = "ERROR [Line " + lineNum + ", Col " + col
                       + "]: Premature end of input. Expected: '" + X + "'  Found: '$'";
                printStep(step, stackStr, inputStr, action, tw);
                err.reportError("PREMATURE_END", X, a, step, col, tw);
                // Pop the mismatched terminal and continue to collect more errors
                stack.pop();
                if (!nodeStack.isEmpty()) nodeStack.pop();
                continue;
            }

            // Terminal mismatch: top is terminal X, current token is a, X != a
            action = "ERROR [Line " + lineNum + ", Col " + col
                   + "]: Terminal mismatch. Expected: '" + X + "'  Found: '" + a + "'";
            printStep(step, stackStr, inputStr, action, tw);
            err.reportError("TERMINAL_MISMATCH", X, a, step, col, tw);

            // Recovery: skip the bad input token (insert/delete strategy)
            String recLine = "    [Recovery] Skipping unexpected token '" + a + "'";
            System.out.println(recLine);
            if (tw != null) tw.println(recLine);
            pos[0]++;   // discard bad token, keep expected terminal on stack
        }
        // ---- End of loop -------------------------------------------------------

        System.out.println(div);
        err.printSummary(tw);
        if (tw != null) tw.println();

        // Only return root for a clean accept with zero errors
        return (accepted && err.getErrorCount() == 0) ? root : null;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String followTokensFor(String nt) {
        Set<String> f = ff.getFollowSets().getOrDefault(nt, Collections.emptySet());
        return f.isEmpty() ? "(none)" : String.join(", ", f);
    }

    private String remainingInput(List<String> tokens, int pos) {
        StringBuilder sb = new StringBuilder();
        for (int i = pos; i < tokens.size(); i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(tokens.get(i));
        }
        return sb.toString();
    }

    private void printStep(int step, String stackStr, String inputStr,
                           String action, PrintWriter tw) {
        String sd = stackStr.length() > 29
                  ? "..." + stackStr.substring(stackStr.length() - 26) : stackStr;
        String id = inputStr.length() > 24
                  ? inputStr.substring(0, 22) + "..." : inputStr;
        String line = String.format("  %-5d | %-30s | %-25s | %s", step, sd, id, action);
        System.out.println(line);
        if (tw != null) tw.println(line);
    }

    public boolean isLL1() { return isLL1; }
    public Map<String, Map<String, List<String>>> getTable() { return table; }
}
