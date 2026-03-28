import java.util.*;
import java.io.*;

/**
 * ErrorHandler — Task 2.4 (full implementation)
 *
 * Features:
 *  - Line number + column (token index) in every error message
 *  - True panic-mode recovery: skip input tokens AND pop stack until sync point
 *  - Terminal mismatch: shows "Expected: 'x'  Found: 'y'"
 *  - Never aborts after first error — collects all errors per string
 *  - Prints every recovery action taken
 */
public class ErrorHandler {

    private int errorCount;
    private List<String> errorMessages;
    private Map<String, Set<String>> followSets;
    private Grammar grammar;
    private int lineNumber;   // line number of the string currently being parsed

    public ErrorHandler(Grammar grammar, Map<String, Set<String>> followSets) {
        this.grammar    = grammar;
        this.followSets = followSets;
        this.errorCount = 0;
        this.errorMessages = new ArrayList<>();
        this.lineNumber = 1;
    }

    /** Call before parsing each new input line. */
    public void setLineNumber(int ln) { this.lineNumber = ln; }

    // ---------------------------------------------------------------
    // Error reporting
    // ---------------------------------------------------------------

    /**
     * Record and print a parse error.
     *
     * @param type   EMPTY_ENTRY | TERMINAL_MISMATCH | PREMATURE_END | EXTRA_INPUT
     * @param expected  what the parser expected (NT name or terminal)
     * @param found     what was actually seen
     * @param step   trace step number
     * @param col    1-based token position in the input (column approximation)
     * @param tw     trace file writer (may be null)
     */
    public void reportError(String type, String expected, String found,
                            int step, int col, PrintWriter tw) {
        errorCount++;
        String location = "[Line " + lineNumber + ", Col " + col + ", Step " + step + "]";
        String detail   = buildDetail(type, expected, found);
        String msg      = location + " " + detail;
        errorMessages.add(msg);
        String line = "  *** ERROR " + msg;
        System.out.println(line);
        if (tw != null) tw.println(line);
    }

    private String buildDetail(String type, String expected, String found) {
        switch (type) {
            case "TERMINAL_MISMATCH":
                return "Terminal mismatch.  Expected: '" + expected
                       + "'  Found: '" + found + "'.";
            case "EMPTY_ENTRY":
                return "No production for non-terminal '" + expected
                       + "' with lookahead '" + found + "' in parsing table."
                       + "  Expected one of: { " + followTokensFor(expected) + " }.";
            case "PREMATURE_END":
                return "Premature end of input.  Expected: '" + expected
                       + "'  Found: '$' (end of input).";
            case "EXTRA_INPUT":
                return "Extra input after parse end.  Expected: '$'  Found: '"
                       + found + "'.";
            default:
                return "Unexpected token '" + found + "'.";
        }
    }

    /** Tokens listed in FOLLOW(nt) — what the parser can legally see after nt. */
    private String followTokensFor(String nt) {
        if (!grammar.isNonTerminal(nt)) return nt;
        Set<String> f = followSets.getOrDefault(nt, Collections.emptySet());
        return f.isEmpty() ? "(none)" : String.join(", ", f);
    }

    // ---------------------------------------------------------------
    // True panic-mode recovery
    // ---------------------------------------------------------------

    /**
     * Panic-mode recovery (Task 2.4 requirement).
     *
     * Algorithm:
     *  1. Build syncSet = union of FOLLOW(X) for every NT currently on the stack.
     *  2. Skip input tokens that are NOT in syncSet, printing each skip.
     *  3. Pop stack symbols until the top NT has the current input in its FOLLOW,
     *     printing each pop.
     *  4. Return true  => caller resumes the parse loop.
     *     Return false => stack is exhausted; caller should stop parsing this string.
     *
     * The nodeStack is kept in sync so the parse tree is not corrupted.
     */
    public boolean panicModeRecover(Stack<String> stack,
                                    Stack<ParseTreeNode> nodeStack,
                                    List<String> inputTokens,
                                    int[] inputPos,
                                    int step,
                                    PrintWriter tw) {

        log("    [Recovery] Initiating panic-mode recovery...", tw);

        // ---- Step 1: build combined sync set from all NTs on the stack ----------
        Set<String> syncSet = new LinkedHashSet<>();
        for (String sym : stack.toList()) {
            if (grammar.isNonTerminal(sym)) {
                syncSet.addAll(followSets.getOrDefault(sym, Collections.emptySet()));
            }
        }
        syncSet.add("$"); // $ is always a safe stopping point
        log("    [Recovery] Sync set: { " + String.join(", ", syncSet) + " }", tw);

        // ---- Step 2: skip input tokens until we reach a sync token --------------
        boolean skipped = false;
        while (inputPos[0] < inputTokens.size()
               && !syncSet.contains(inputTokens.get(inputPos[0]))) {
            log("    [Recovery] Skipping unexpected token '"
                + inputTokens.get(inputPos[0]) + "'", tw);
            inputPos[0]++;
            skipped = true;
        }
        String resumeToken = (inputPos[0] < inputTokens.size())
                             ? inputTokens.get(inputPos[0]) : "$";
        if (skipped) {
            log("    [Recovery] Resuming at token '" + resumeToken + "'", tw);
        }

        // ---- Step 3: pop stack until top NT can handle resumeToken --------------
        int pops = 0;
        while (!stack.isEmpty()) {
            String top = stack.top();

            // Reached bottom-of-stack marker
            if (top.equals("$")) {
                if (resumeToken.equals("$")) {
                    log("    [Recovery] Both stack and input at '$' — will accept.", tw);
                    return true;
                }
                log("    [Recovery] Stack exhausted. Cannot recover.", tw);
                return false;
            }

            // Check if this NT can handle the resume token
            if (grammar.isNonTerminal(top)) {
                Set<String> follow = followSets.getOrDefault(top, Collections.emptySet());
                if (follow.contains(resumeToken)) {
                    if (pops == 0 && !skipped) {
                        // We haven't moved at all — force-pop this NT so the loop
                        // can't re-enter the same error endlessly.
                        log("    [Recovery] Force-popping stuck NT '" + top
                            + "' (no progress otherwise).", tw);
                        stack.pop();
                        if (!nodeStack.isEmpty()) nodeStack.pop();
                    } else {
                        log("    [Recovery] Sync point found: NT '" + top
                            + "' has '" + resumeToken + "' in FOLLOW. "
                            + "Popped " + pops + " symbol(s) from stack.", tw);
                    }
                    // Keep nodeStack aligned
                    while (nodeStack.size() > stack.size()) nodeStack.pop();
                    return true;
                }
            }

            log("    [Recovery] Popping '" + top + "' from stack.", tw);
            stack.pop();
            if (!nodeStack.isEmpty()) nodeStack.pop();
            pops++;
        }

        log("    [Recovery] Stack emptied — no sync point found.", tw);
        inputPos[0] = inputTokens.size(); // drain remaining input
        return false;
    }

    // ---------------------------------------------------------------
    // Summary
    // ---------------------------------------------------------------

    public void printSummary(PrintWriter tw) {
        String line = (errorCount == 0)
            ? "  Result: String ACCEPTED successfully! No errors."
            : "  Result: Parsing completed with " + errorCount + " error(s).";
        System.out.println(line);
        if (tw != null) tw.println(line);
        for (String msg : errorMessages) {
            System.out.println("    - " + msg);
            if (tw != null) tw.println("    - " + msg);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void log(String msg, PrintWriter tw) {
        System.out.println(msg);
        if (tw != null) tw.println(msg);
    }

    public int           getErrorCount()    { return errorCount; }
    public List<String>  getErrorMessages() { return errorMessages; }
    public void          reset()            { errorCount = 0; errorMessages.clear(); }
}