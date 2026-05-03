package jflux;

enum TokenType {
    // Single-Character Tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // One or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, OR, CLASS, FALSE, TRUE, FUNC, FOR, WHILE, 
    IF, ELSE, NULL, PRINT, RETURN, SUPER, THIS, VAR,

    // End Of File
    EOF
}
