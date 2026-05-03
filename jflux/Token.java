package jflux;

public class Token {
    // final is the same as const
    final TokenType type;
    final String    lexeme;
    final Object    literal; // Object is the absolute base object type that everything derives from
    final int       line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type    = type;
        this.lexeme  = lexeme;
        this.literal = literal;
        this.line    = line;
    }

    // overide for the built in toString so we can print it
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
