package jflux;

import java.util.ArrayList;
import java.util.Arrays;
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
            if (match(FUNC)) return function("function"); 
            if (match(VAR))  return varDeclaration();

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
        if (match(IF))         return ifStatement(); 
        if (match(PRINT))      return printStatement();
        if (match(RETURN))     return returnStatement();
        if (match(FOR))        return forStatement();
        if (match(WHILE))      return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect a '(' after 'if'");
        // the if condition we evaluate to be true or false
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect a ')' after if condition");

        // branch becomes the single next statement. If a block is there the "statement" is the whole code block
        Stmt thenBranch = statement();
        Stmt elseBranch = null;

        // else branch belongs to most recent if always.
        if(match(ELSE)) { // else not required
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt returnStatement() {
        Token keyword = previous(); // stores line number for error reporting
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expext ';' after value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect a '(' after 'for'");

        Stmt initializer;
        if (match(SEMICOLON)) { // no initialiser
            initializer = null;
        } else if (match(VAR)) { // if we are declaring the iterator
            initializer = varDeclaration();
        } else { // if we are reusing an old variable as the iterator
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) { // if expression given, use it as the condition
            condition = expression();
        }
        consume(SEMICOLON, "Expect a ; after loop condition");

        Expr increment = null;
        if(!check(SEMICOLON)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect a ')' after if clauses");

        Stmt body = statement();

        // if we have an increment, make a block with the increment at the end, which we will put into a while loop
        if (increment != null) {
            body = new Stmt.Block(
                Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
                )
            );
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body); // put the while loop for the for loop in the block

        if(initializer != null) { // if we have an initializer put it at the start of the block
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        // TODO add break and continue keywords
        return body;
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect a '(' after 'while'");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect a ')' after while condition");
        Stmt body = statement();

        return new Stmt.While(condition, body);
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
        Expr expr = or();

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

    private Expr or() {
        Expr expr = and(); // left side expr

        while(match(OR)) {
            Token operator = previous();
            Expr right = and(); // right side expr
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while(match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
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

        return call();
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();

        if(!check(RIGHT_PAREN)) // closing paren
        {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "cant have more than 255 arguments");
                }
                arguments.add(expression());
            } while (match(COMMA)); // if no match for a comme, then it must have been last arg
        }

        Token paren = consume(RIGHT_PAREN, "Expext a ')' after the arguments");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr call() {
        Expr expr = primary();

        while(true) { // check for chained calls
            if (match(LEFT_PAREN)) { // opening paren
                expr = finishCall(expr); // make an expression reprersenting the function call
            } else {
                break;
            }
        }

        return expr;
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

    /**
     * parses a function declaration statement
     * 
     * @param kind to differentiate between if the function is a normal function or method
     */
    private Stmt.Function function(String kind) {
        // FUNCTION NAME AND PARAMETER DECLARATION
        // scan for a name for the function
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) { // check if the next token is not a RIGHT_PAREN => have parameters
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Cant have more than 255 parameters");
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA)); // while more parameters
        }
        consume(RIGHT_PAREN, "Expect a ')' after parameters");

        // FUNCTION BODY DECLARATION
        consume(LEFT_BRACE, "Expect a  '{' before " + kind + " body."); // block doesnt check for '{'
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
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
