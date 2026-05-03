package jflux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// static so we dont have to write TokenType. each time
import static jflux.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    // key is String, value is TokenType
    private static final Map<String, TokenType> keywords;

    // static block - runs when class loads to initialise const static fields
    static {
        keywords = new HashMap<>();
        keywords.put("and",       AND);
        keywords.put("class",   CLASS);
        keywords.put("else",     ELSE);
        keywords.put("false",   FALSE);
        keywords.put("for",       FOR);
        keywords.put("func",     FUNC);
        keywords.put("if",         IF);
        keywords.put("null",     NULL);
        keywords.put("or",         OR);
        keywords.put("print",   PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",   SUPER);
        keywords.put("this",     THIS);
        keywords.put("true",     TRUE);
        keywords.put("var",       VAR);
        keywords.put("while",   WHILE);
    }

    private int start   = 0; // start of lexeme
    private int current = 0; // current pointer of character in the lexeme
    private int line    = 1; // current line number we are at

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while(!isAtEnd()){
            // we are now at the beginning of the next lexeme
            start = current;
            scanToken();
        }

        // last token should be EOF so the parser knows its at the end of the tokens
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case '*': addToken(STAR); break;
            case ';': addToken(SEMICOLON); break;

            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;    
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;    
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;    
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break; 
                
            case '/':
                if (match('/')) {
                    // A comment goes till the end of the line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    // A comment that goes till block is closed
                    while ( !isAtEnd() && !(peek() == '*' && peekNext() == '/')) {
                        if(peek() == '\n') line++;
                        advance(); // check next chars
                    }
                    if (isAtEnd()) {
                        Flux.error(line, "Unterminated block comment.");
                        return;
                    }
                    advance(); // consume *
                    advance(); // consume /
                } else {
                    addToken(SLASH);
                }
                break;

            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace
                break;
            case '\n':
                line++;
                break;

            // Strings
            case '"': string(); break;

            // Numbers
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else{
                    Flux.error(line, "Unexpected character -> " + c + " .");
                }
                break;
        }
    }

    private void string() {
        while( peek() != '"' && !isAtEnd()) {
            if(peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            Flux.error(line, "Unterminated String.");
            return;
        }

        // the closing ".
        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text    = source.substring(start, current);
        TokenType type = keywords.get(text); // check if its a registered keyword
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    private boolean match(char expected){
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isAlpha(char c) {
        return  (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c == '_');
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void number() {
        // while the next character is a number, advance
        while (isDigit(peek())) advance();
        
        // look for fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            // consume the "."
            advance();

            while (isDigit(peek())) advance();
        }

        addToken(NUMBER,
            Double.parseDouble(source.substring(start, current)));
    }

    private char peekNext() { // peek twice into future to see after dp
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

}
