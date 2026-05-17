package jflux;

import java.util.ArrayList;
import java.util.List;

import static jflux.TokenType.*;

/**
 * Parses a token stream into statement and expression AST nodes.
 */
public class Parser {
    // runtime error is an unchecked exception type
    // Runtime errors will bubble up silently until something catches it
    // also if we had an unchecked exception type, we couldnt see multiple errors in the code,
    // as the code would just crash.
    /**
     * Marker exception type for parser errors.
     */
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    /**
     * Creates a parser for the given token stream.
     *
     * @param tokens list of scanned tokens
     */
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    
    /**
     * Parses all statements from the token list.
     *
     * @return list of parsed statements
     */
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()){
            statements.add(declaration());
        }

        return statements;
    }

    private Stmt declaration() {
        try{
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch(ParseError error) { // if fails parsing, resynchronize at nect statement
            synchronize(); // we start with declaration at each statement so its a good place to resynchronize
            return null;
        }
    }

    /**
     * Parses a single statement.
     *
     * @return parsed statement node
     */
    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }


    /**
     * Parses an expression statement.
     *
     * @return expression statement node
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while(!check(RIGHT_BRACE) && !isAtEnd()){
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * Parses an expression.
     *
     * @return parsed expression node
     */
    // simply expands to the equality rule
    private Expr expression() {
        return assignment();
    }

    /**
     * Parses assignment expressions
     */
    private Expr assignment() {
        // assume left side is an expression
        Expr expr = equality();

        // finds an = so we now know its assignment
        if(match(EQUAL)) {
            Token equals = previous(); // store so we can show which = the error happened at
            Expr  value  = assignment(); // recursively checks right side for more assignment

            // if the left side of the equals sign is a variable, assign the variable.
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            // if left side of equals sign is not a variable, report error
            error(equals, "Invalid assignment target");
        }

        return expr;
    }

    // equality → comparison ( ( "!=" | "==" ) comparison )* ;
    /**
     * Parses equality expressions.
     *
     * @return parsed expression node
     */
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
    /**
     * Parses comparison expressions.
     *
     * @return parsed expression node
     */
    private Expr comparison() {
        Expr expr = term();

        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right     = term();
            expr           = new Expr.Binary(expr, operator, right);  
        }

        return expr;
    }

    /**
     * Parses addition and subtraction expressions.
     *
     * @return parsed expression node
     */
    private Expr term() {
        Expr expr = factor();

        while(match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right     = factor();
            expr = new  Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parses multiplication and division expressions.
     *
     * @return parsed expression node
     */
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
    /**
     * Parses unary expressions.
     *
     * @return parsed expression node
     */
    private Expr unary() {
        if(match(BANG, MINUS)) {
            Token operator = previous();
            Expr right     = primary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    //primary → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"
    /**
     * Parses primary expressions like literals and grouped expressions.
     *
     * @return parsed expression node
     */
    private Expr primary(){
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE))  return new Expr.Literal(true);
        if (match(NULL))  return new Expr.Literal(null);

        if(match(NUMBER, STRING)) return new Expr.Literal(previous().literal);

        if(match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if(match(LEFT_PAREN)) {
            // match(LEFT_PAREN) consumes the '(' expression() is now the inner of the bracket
            Expr expr = expression(); // gets the inner of the bracket
            consume(RIGHT_PAREN, "Expect ')' after expression."); // checks and consumes ')'
            return new Expr.Grouping(expr);
        }
        // no expression given at all
        throw error(peek(), "Expect expression");
    }

    /**
     * Parses a print statement.
     *
     * @return print statement node
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expext ';' after value");
        return new Stmt.Print(value);
    }

    /**
     * parses a variable declaration statement
     */
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null; // if no equal found, value just initialised to null.
        if (match(EQUAL)) {
            initializer = expression(); // if equals, initialised to the expression after it.
        }

        consume(SEMICOLON, "Expext ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    // checks to see if the current token has any of the given types
    /**
     * Consumes the next token if it matches any of the given types.
     *
     * @param types expected token types
     * @return true if a match was consumed
     */
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
    /**
     * Consumes a token of the expected type or throws a parse error.
     *
     * @param type expected token type
     * @param message error message if not matched
     * @return consumed token
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    /**
     * Reports a parse error at the given token.
     *
     * @param token token where error occurred
     * @param message error description
     * @return a ParseError exception
     */
    private ParseError error(Token token, String message) {
        Flux.error(token, message);
        return new ParseError();
    }

    /**
     * Synchronizes the parser after an error to recover and continue parsing.
     */
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
    /**
     * Checks if the current token is of the given type.
     *
     * @param type expected token type
     * @return true if matched
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * Advances to the next token and returns the previous one.
     *
     * @return previous token
     */
    // returns the current Token and increments current counter
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * Determines whether the parser has reached the end of input.
     *
     * @return true if at EOF
     */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    /**
     * Returns the current token without consuming it.
     *
     * @return current token
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Returns the most recently consumed token.
     *
     * @return previous token
     */
    private Token previous() {
        return tokens.get(current - 1);
    }
}
