package LALR;

import java.util.Arrays;

public class Production {
    private String left;
    private String[] right;

    Production(String left, String[] right) {
        this.left = left;
        this.right = right;
    }

    String getLeft() {
        return left;
    }

    String[] getRight() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Production production = (Production) o;
        if (!left.equals(production.left)) return false;
        return Arrays.equals(right, production.right);
    }

    @Override
    public int hashCode() {
        int result = left.hashCode();
        result = 31 * result + Arrays.hashCode(right);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(left).append("->");
        for (String s : right) result.append(s).append(" ");
        return result.toString().substring(0, result.length() - 1);
    }
}