package Parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

class LALRParser {
    private ArrayList<Token> tokens;
    private int index;
    private Node current;
    private Node root;
    private HashMap<Map.Entry<Integer, String>, String> table;
    private ArrayList<String> productions = new ArrayList<>();
    private Stack<Integer> stackStatus = new Stack<>();
    private Stack<Node> stackNode = new Stack<>();
    private ArrayList<String> vts = new ArrayList<>();

    @SuppressWarnings("unchecked")
    LALRParser() {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("LALRTabble")));
            vts = (ArrayList<String>) ois.readObject();
            table = (HashMap<Map.Entry<Integer, String>, String>) ois.readObject();

            Object obj;
            while ((obj = ois.readObject()) != null) {
                productions.add((String) obj);
            }
            productions.remove(0);
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        LALRParser lalrParser = new LALRParser();
        lalrParser.StartParsing(new Lexer().StartLexicalAnalysis(Lexer.readFile()));
        if (lalrParser.root != null)
            Node.print(lalrParser.root, null);
    }

    Node getRoot() {
        return root;
    }

    ArrayList<Token> getTokens() {
        return tokens;
    }

    void StartParsing(ArrayList<Token> arrayList) {
        index = 0;
        tokens = arrayList;
        current = getNextNode();
        stackStatus.push(0);
        String action;

        while (true) {
            action = lookupTable(current);
            if (action == null) {
                Node tmp = current;
                if (!dealWithException()) {
                    System.err.println("Unknown error near line " + tmp.getToken().getLine() + ", token<" + tmp.getToken().getValue() + ">");
                    break;
                }
                continue;
            }

            if (action.equals("acc")) {
                root = stackNode.pop();
                break;
            }

            if (action.startsWith("s")) {
                stackNode.push(current);
                current = getNextNode();
                stackStatus.push(Integer.parseInt(action.substring(1)));
                continue;
            }

            if (action.startsWith("r")) {
                String production = productions.get(Integer.parseInt(action.substring(1)) - 1);

                Node node = new Node(production.split(":")[0]);
                int num = Integer.parseInt(production.split(":")[1]);
                Node[] nodeChild = new Node[num];

                for (int i = num - 1; i >= 0; i--) {
                    nodeChild[i] = stackNode.pop();
                    stackStatus.pop();
                }

                for (int i = 0; i < num; i++) {
                    node.getChildren().add(nodeChild[i]);
                }

                stackStatus.push(Integer.parseInt(lookupTable(node)));
                stackNode.push(node);
            } else {
                System.err.println("LALRTable error at (" + stackStatus.peek() + " , " + current.getType() + ")");
                break;
            }
        }
    }

    private boolean dealWithException() {
        int status = stackStatus.peek();
        Token token = tokens.get(index >= 1 ? index - 1 : index);
        token.setInValid();
        System.err.println("Syntax error near line " + token.getLine() + ", token<"
                + token.getValue() + ">");

        Node node = null;
        while (lookupTable(status, "statement") == null && !stackNode.isEmpty()) {
            node = stackNode.pop();
            status = stackStatus.pop();
        }
        stackStatus.push(status);
        stackNode.push(node);

        String string = lookupTable(status, "statement");
        if (string == null)
            status = 0;
        else
            status = Integer.valueOf(string);

        stackStatus.push(status);
        stackNode.push(new Node("statement"));
        HashSet<String> follows = getFollows(status);
        current = getNextLineNode();
        while (!follows.contains(current.getType())) {
            current = getNextLineNode();
            if (current.getType().equals("$"))
                return false;
        }
        return true;
    }

    private HashSet<String> getFollows(int status) {
        HashSet<String> result = new HashSet<>();
        for (String vt : vts) {
            if (lookupTable(status, vt) != null) {
                result.add(vt);
            }
        }
        return result;
    }

    private Node getNextNode() {
        if (index == tokens.size()) {
            return new Node("$");
        }
        if (tokens.get(index).getType() == TokenType.ERR || tokens.get(index).getType() == TokenType.COMMENT) {
            index++;
            return getNextNode();
        }
        return new Node(tokens.get(index++));
    }

    private Node getNextLineNode() {
        Node tmp = getNextNode();
        if (tmp.getType().equals("$")) {
            return tmp;
        }
        if (tmp.getToken().getLine() == current.getToken().getLine())
            return getNextLineNode();
        return tmp;
    }

    private String lookupTable(Node node) {
        return lookupTable(stackStatus.peek(), node.getType());
    }

    private String lookupTable(int row, String column) {
        return table.get(new AbstractMap.SimpleEntry<>(row, column));
    }
}


