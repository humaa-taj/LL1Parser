import java.util.*;

public class Stack<T> {
    private LinkedList<T> data;
    public Stack() { data = new LinkedList<>(); }
    public void push(T item) { data.addLast(item); }
    public T pop() { if (isEmpty()) throw new EmptyStackException(); return data.removeLast(); }
    public T top() { if (isEmpty()) throw new EmptyStackException(); return data.getLast(); }
    public boolean isEmpty() { return data.isEmpty(); }
    public int size() { return data.size(); }
    public String toStringBottomToTop() {
        StringBuilder sb = new StringBuilder();
        for (T item : data) { if (sb.length() > 0) sb.append(" "); sb.append(item); }
        return sb.toString();
    }
    public List<T> toList() { return new ArrayList<>(data); }
    @Override public String toString() { return toStringBottomToTop(); }
}
