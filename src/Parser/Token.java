package Parser;

public class Token {
    private int line;
    private TokenType type;
    private String value;
    private int position;
    private boolean valid;

    Token(int line, String value, TokenType type, int position) {
        this.line = line;
        this.type = type;
        this.value = value;
        this.position = position;
        this.valid = type != TokenType.ERR;
    }

    int getLine() {
        return line;
    }

    TokenType getType() {
        return type;
    }

    String getValue() {
        return value;
    }

    int getPosition() {
        return position;
    }

    boolean isValid() {
        return valid;
    }

    void setInValid() {
        this.valid = false;
    }

    @Override
    public String toString() {
        return line + " " + type + " " + value + " " + position;
    }
}
