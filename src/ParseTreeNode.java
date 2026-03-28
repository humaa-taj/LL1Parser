import java.util.*;

public class ParseTreeNode {
    private String symbol;
    private List<ParseTreeNode> children;
    private boolean isTerminal;

    public ParseTreeNode(String symbol, boolean isTerminal) {
        this.symbol = symbol; this.isTerminal = isTerminal; this.children = new ArrayList<>();
    }
    public void addChild(ParseTreeNode child) { children.add(child); }
    public String getSymbol() { return symbol; }
    public List<ParseTreeNode> getChildren() { return children; }
    public boolean isTerminal() { return isTerminal; }
    public boolean isLeaf() { return children.isEmpty(); }

    public void print(String prefix, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        String display = (isTerminal && !symbol.equals("epsilon")) ? "'" + symbol + "'" : symbol;
        System.out.println(prefix + connector + display);
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < children.size(); i++) children.get(i).print(childPrefix, i == children.size()-1);
    }

    public void printTree() {
        System.out.println(symbol);
        for (int i = 0; i < children.size(); i++) children.get(i).print("", i == children.size()-1);
    }

    public void writeTree(StringBuilder sb, String prefix, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        String display = (isTerminal && !symbol.equals("epsilon")) ? "'" + symbol + "'" : symbol;
        sb.append(prefix).append(connector).append(display).append("\n");
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < children.size(); i++) children.get(i).writeTree(sb, childPrefix, i == children.size()-1);
    }

    public String treeToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(symbol).append("\n");
        for (int i = 0; i < children.size(); i++) children.get(i).writeTree(sb, "", i == children.size()-1);
        return sb.toString();
    }
}
