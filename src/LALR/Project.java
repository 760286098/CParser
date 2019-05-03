package LALR;

import java.util.HashSet;

public class Project {
    private Production production;
    private int dot;
    private HashSet<String> lookahead;

    Project(Production production, int dot) {
        this.production = production;
        this.dot = dot;
        this.lookahead = new HashSet<>();
    }

    Production getProduction() {
        return production;
    }

    HashSet<String> getLookahead() {
        return lookahead;
    }

    Project getNextProject() {
        Project tmp = new Project(production, dot + 1);
        tmp.lookahead = new HashSet<>();
        return tmp;
    }

    String getNextString() {
        return dot == production.getRight().length ? null : production.getRight()[dot];
    }

    String getStringOfNextString() {
        return dot < production.getRight().length - 1 ? production.getRight()[dot + 1] : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Project project = (Project) o;
        if (dot != project.dot) return false;
        return production.equals(project.production);
    }

    @Override
    public int hashCode() {
        int result = production.hashCode();
        result = 31 * result + dot;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(production.getLeft()).append("->");
        for (int i = 0; i < dot; i++)
            result.append(production.getRight()[i]).append(" ");
        result.append(". ");
        for (int i = dot; i < production.getRight().length; i++)
            result.append(production.getRight()[i]).append(" ");
        return result.toString().substring(0, result.length() - 1);
    }
}