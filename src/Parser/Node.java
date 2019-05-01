package Parser;

import java.util.ArrayList;

public class Node {
    private static final String PREFIX_BRANCH = "├─";// 树枝
    private static final String PREFIX_TRUNK = "│ ";// 树干
    private static final String PREFIX_LEAF = "└─";// 叶子
    private static final String PREFIX_EMP = "  ";// 空
    private Token token = null;
    private ArrayList<Node> children = new ArrayList<>();
    private String type;

    Node(Token token) {
        this.token = token;
        switch (token.getType()) {
            case INTEGER:
            case FLOAT:
            case DOUBLE:
            case CHARACTER:
                this.type = "constant";
                break;
            case IDENTIFIER:
                this.type = "identifier";
                break;
            case STRINGLITERAL:
                this.type = "string-literal";
                break;
            case KEYWORD:
            case PUNCTUATOR:
                this.type = token.getValue();
                break;
            default:
                this.type = "";
        }
    }

    Node(String type) {
        this.type = type;
    }

    static void print(Node node, String prefix) {
        if (prefix == null) {
            prefix = "";
            System.out.println(node.getValue());
        }
        prefix = prefix.replace(PREFIX_BRANCH, PREFIX_TRUNK);
        prefix = prefix.replace(PREFIX_LEAF, PREFIX_EMP);

        ArrayList<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            if (i == children.size() - 1) {// 最后一个是叶子
                System.out.println(prefix + PREFIX_LEAF + child.getValue());
                print(child, prefix + PREFIX_LEAF);
            } else {// 树枝
                System.out.println(prefix + PREFIX_BRANCH + child.getValue());
                print(child, prefix + PREFIX_TRUNK);
            }
        }
    }

    Token getToken() {
        return token;
    }

    ArrayList<Node> getChildren() {
        return children;
    }

    String getType() {
        return type;
    }

    private String getValue() {
        return token == null ? type : token.getValue();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (children.size() == 0) {
            return type + "[]";
        }
        for (Node node : children) {
            result.append(node.toString()).append(",");
        }
        return type + "[" + result.substring(0, result.length() - 1) + "]";
    }
}
