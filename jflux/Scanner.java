package jflux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// static so we dont have to write TokenType. each time
import static jflux.TokenType.*;

/**
 * Scans source text and produces a list of tokens.
 */
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

    /**
     * Creates a scanner for the given source text.
     *
     * @param source text to scan
     */
    Scanner(String source) {
        this.source = source;
    }

    /**
     * Scans the entire source and returns the token list.
     *
     * @return list of tokens
     */
    public List<Token> scanTokens() {
        while(!isAtEnd()){
            // we are now at the beginning of the next lexeme
            start = current;
            scanToken();
        }

        // last token should be EOF so the parser knows its at the end of the tokens
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    /**
     * Scans the next lexeme and adds a token.
     */
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

    /**
     * Scans a string literal.
     */
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

    /**
     * Scans an identifier or keyword.
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text    = source.substring(start, current);
        TokenType type = keywords.get(text); // check if its a registered keyword
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    /**
     * Conditionally consumes the next character if it matches.
     *
     * @param expected expected character
     * @return true if consumed
     */
    private boolean match(char expected){
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    /**
     * Checks whether a character is alphabetic or numeric.
     *
     * @param c character to test
     * @return true if alphanumeric
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Checks whether a character is alphabetic or underscore.
     *
     * @param c character to test
     * @return true if alphabetic
     */
    private boolean isAlpha(char c) {
        return  (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c == '_');
    }

    /**
     * Checks whether a character is a digit.
     *
     * @param c character to test
     * @return true if digit
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Scans a numeric literal.
     */
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

    /**
     * Peeks one character ahead without consuming it.
     *
     * @return next character or '\0' at end
     */
    private char peekNext() { // peek twice into future to see after dp
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    /**
     * Consumes the next character and returns it.
     *
     * @return consumed character
     */
    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    /**
     * Peeks the current character without consuming it.
     *
     * @return current character or '\0' at end
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * Adds a token with no literal value.
     *
     * @param type token type
     */
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /**
     * Adds a token with an optional literal value.
     *
     * @param type token type
     * @param literal literal value
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    /**
     * Checks whether the end of the source has been reached.
     *
     * @return true if at end of source
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

}
