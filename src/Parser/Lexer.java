package Parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Lexer {
    private String[] keywords;
    private Pattern pattern;

    Lexer() {
        keywords = new String[]{"auto", "break", "extern", "return", "void", "case", "float", "short", "char", "for", "while", "const", "goto", "sizeof", "bool", "continue", "if", "static", "default", "inline", "struct", "do", "int", "switch", "double", "long", "typedef", "else", "union"};

        String regex_Comment = "//.*" + "|/\\*[\\s\\S]*?\\*/";
        String regex_String = "L?\"([^\"\\\\\n]|\\\\([bfnrt'\"\\\\]))*\"";
        String regex_Character = "L?\'([^\'\\\\]|\\\\([bfnrt'\"\\\\]))\'";
        String regex_Identifier = "[A-Z_a-z][A-Z_a-z0-9]*";
        String regex_Integer = "[1-9][0-9]*" + "|0[xX][0-9a-fA-F]+" + "|0[0-7]*";
        String regex_Float = "(0|[1-9][0-9]*)\\.[0-9]+([eE][-+]?[1-9][0-9]*)?[fF]?"
                + "|[1-9][0-9]*[eE][-+]?[1-9][0-9]*[fF]?"
                + "|0[xX][0-9a-fA-F]*\\.[0-9a-fA-F]+[pP][-+]?[0-9]+[fF]?"
                + "|0[xX][0-9a-fA-F]+\\.[pP][-+]?[0-9]+[fF]?"
                + "|0[xX][0-9a-fA-F]+[pP][-+]?[0-9]+[fF]?";

        String[] punctuator = new String[]{"...", "<<=", ">>=", "->", "++", "--", "<<", ">>", "<=", ">=", "==", "!=", "&&", "||", "*=", "/=", "%=", "+=", "-=", "&=", "^=", "|=", "[", "]", "(", ")", "{", "}", "&", "*", "+", "-", "~", "!", "/", "%", "<", ">", "^", "|", "?", ":", ";", ".", "=", ",", "#"};
        StringBuilder tmp = new StringBuilder();
        for (String s : punctuator)
            tmp.append("Separator").append(s);
        String regex_Punctuator = dealWithEscape(tmp.toString());
        regex_Punctuator = regex_Punctuator.replace("Separator", "|").substring(1);

        String regex = "\\s*("
                + "(?<COMMENT>" + regex_Comment + ")|"
                + "(?<PUNCTUATOR>" + regex_Punctuator + ")|"
                + "(?<STRINGLITERAL>" + regex_String + ")|"
                + "(?<CHARACTER>" + regex_Character + ")|"
                + "(?<IDENTIFIER>" + regex_Identifier + ")|"
                + "(?<FLOAT>" + regex_Float + ")|"
                + "(?<INTEGER>" + regex_Integer + ")|"
                + ".*)?";
        pattern = Pattern.compile(regex);
    }

    public static void main(String[] args) {
        ArrayList<Token> tokens = new Lexer().StartLexicalAnalysis(readFile());
        for (Token token : tokens) {
            System.out.println(token);
        }
        System.out.println("token size: " + tokens.size());
    }

    static String readFile() {
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File("test.txt")));
            String s;
            while ((s = br.readLine()) != null) {
                result.append(System.lineSeparator()).append(s);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString().substring(2);
    }

    ArrayList<Token> StartLexicalAnalysis(String text) {
        ArrayList<Token> tokens = new ArrayList<>();
        int pos = 0;
        int endPos = text.length();
        int line = 1;
        Matcher matcher = pattern.matcher(text);

        while (pos < endPos) {
            matcher.region(pos, endPos);
            if (matcher.lookingAt()) {
                String s = matcher.group(0);
                line += getNumOfLine(s);

                Token token;
                String m = matcher.group(1);
                if (m.equals("")) {
                    break;
                }
                pos += s.indexOf(m);
                if (matcher.group("COMMENT") != null) {
                    token = new Token(line - getNumOfLine(m), m, TokenType.COMMENT, pos);
                } else if (matcher.group("PUNCTUATOR") != null) {
                    token = new Token(line, m, TokenType.PUNCTUATOR, pos);
                } else if (matcher.group("STRINGLITERAL") != null) {
                    token = new Token(line, m, TokenType.STRINGLITERAL, pos);
                } else if (matcher.group("CHARACTER") != null) {
                    token = new Token(line, m, TokenType.CHARACTER, pos);
                } else if (matcher.group("IDENTIFIER") != null) {
                    if (isKeyword(m)) {
                        token = new Token(line, m, TokenType.KEYWORD, pos);
                    } else {
                        token = new Token(line, m, TokenType.IDENTIFIER, pos);
                    }
                } else if (matcher.group("FLOAT") != null) {
                    if (m.contains("f") || m.contains("F")) {
                        try {
                            Float.parseFloat(m);
                            token = new Token(line, m, TokenType.FLOAT, pos);
                        } catch (Exception e) {
                            System.err.println("Lexical error near line " + line + ", token<" + m + ">: " + e.getMessage());
                            token = new Token(line, m, TokenType.ERR, pos);
                        }
                    } else {
                        try {
                            Double.parseDouble(m);
                            token = new Token(line, m, TokenType.DOUBLE, pos);
                        } catch (Exception e) {
                            System.err.println("Lexical error near line " + line + ", token<" + m + ">: " + e.getMessage());
                            token = new Token(line, m, TokenType.ERR, pos);
                        }
                    }
                } else if (matcher.group("INTEGER") != null) {
                    try {
                        if (Integer.decode(m) != null) {
                            token = new Token(line, m, TokenType.INTEGER, pos);
                        } else {
                            token = new Token(line, m, TokenType.ERR, pos);
                        }
                    } catch (Exception e) {
                        System.err.println("Lexical error near line " + line + ", token<" + m + ">: " + e.getMessage());
                        token = new Token(line, m, TokenType.ERR, pos);
                    }
                } else {
                    token = new Token(line, m, TokenType.ERR, pos);
                }
                tokens.add(token);
                pos += m.length();
            } else {
                System.err.println("Lexical error near line " + line);
                break;
            }
        }
        return tokens;
    }

    private String dealWithEscape(String str) {
        return str.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("+", "\\+")
                .replace("^", "\\^")
                .replace("|", "\\|")
                .replace(".", "\\.")
                .replace("?", "\\?")
                .replace("$", "\\$")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private int getNumOfLine(String s) {
        return s.length() - s.replace("\n", "").length();
    }

    private boolean isKeyword(String s) {
        for (String keyword : keywords) {
            if (keyword.equals(s)) {
                return true;
            }
        }
        return false;
    }
}