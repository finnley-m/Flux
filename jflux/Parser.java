package jflux;

import java.util.List;

import static jflux.TokenType.*;

public class Parser {
    // runtime error is an unchecked exception type
    // Runtime errors will bubble up silently until something catches it
    // also if we had an unchecked exception type, we couldnt see multiple errors in the code,
    // as the code would just crash.
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // parses through the tokens and returns a binary tree for the expression
    // in terms of the Expr subclasses instead of tokens
    Expr parse() {
        try {
            return expression();
        }
        catch (ParseError error) {
            return null;
        }
    }

    // simply expands to the equality rule
    private Expr expression() {
        return equality();
    }

    // equality → comparison ( ( "!=" | "==" ) comparison )* ;
    private Expr equality() {
        Expr expr = comparison();

        // if current is one of the 2 types,
        // create a new binary expression and return it
        // loop breaks when we dont hit anymore equality operators in the string of tokens
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right     = comparison();
            expr           = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private Expr comparison() {
        Expr expr = term();

        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right     = term();
            expr           = new Expr.Binary(expr, operator, right);  
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while(match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right     = factor();
            expr = new  Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // TODO MAYBE EXTEND TO "+" ASWELL
    //unary → ( "!" | "-" ) unary
    private Expr unary() {
        if(match(BANG, MINUS)) {
            Token operator = previous();
            Expr right     = primary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    //primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
    private Expr primary(){
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE))  return new Expr.Literal(true);
        if (match(NULL))  return new Expr.Literal(null);

        if(match(NUMBER, STRING)) return new Expr.Literal(previous().literal);

        if(match(LEFT_PAREN)) {
            // match(LEFT_PAREN) consumes the '(' expression() is now the inner of the bracket
            Expr expr = expression(); // gets the inner of the bracket
            consume(RIGHT_PAREN, "Expect ')' after expression."); // checks and consumes ')'
            return new Expr.Grouping(expr);
        }
        // no expression given at all
        throw error(peek(), "Expect expression");
    }

    // checks to see if the current token has any of the given types
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if(check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    // Checks if current character is of expected type, if so consumes,
    // else it throws an error with the given message
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Flux.error(token, message);
        return new ParseError();
    }

    // after we reach a semicolon, look for the next keyword and resynchronize there
    private void synchronize() {
        // skips past the bad token
        advance();

        while(!isAtEnd()) {
            // return when we reach the end of the bad statement
            if (previous().type == SEMICOLON) return; 

            // incase there is a missing smicolon
            switch(peek().type) {
                case CLASS:
                case FUNC:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    break;
            }   

            advance();
        }
     }

    // true if current token is of the given type
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // returns the current Token and increments current counter
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
