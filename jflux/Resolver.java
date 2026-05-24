package jflux;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Stack;

// RESOLVING
// To figure out, at compile time, exactly which declaration every variable use refers to — and how far away it is.


/**
 * Variable resolution — for every variable use, 
 *                     figure out exactly which declaration it refers to, 
 *                     and how many scope "hops" away it lives. 
 *                     That distance gets stored so the interpreter can 
 *                     look it up directly instead of searching the environment chain at runtime.
 */
class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;
    // Boolean => false if declared but not defined, true if both, null if neither
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

     private enum FunctionType {
        NONE,
        FUNCTION
    }

    /**
     * Resolves a list of statements in order.
     * @param statements the list of statements to resolve
     */
    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    /**
     * Resolves a single statement by dispatching to its visitor method.
     * @param stmt the statement to resolve
     */
    void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * Resolves a variable use to its declaration, storing the scope distance
     * in the interpreter for efficient lookup at runtime.
     * @param expr the expression using the variable
     * @param name the variable's token
     */
    void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    /**
     * Resolves a function by creating a new scope, declaring and defining
     * all parameters, then resolving the body.
     * @param function the function statement to resolve
     */
    private void resolveFunction(Stmt.Function function, FunctionType type) {
        // store enclosing function type as we allow nested funcitons too
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope(); // new scope for funciton
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        // put back the origional function type as we finished the nested function if any
        currentFunction = enclosingFunction;
    }

    /**
     * Resolves a single expression by dispatching to its visitor method.
     * @param expr the expression to resolve
     */
    void resolve(Expr expr) {
        expr.accept(this);
    }

    // make a new scope block
    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    // if scope is done we just need to pop it
    private void endScope() {
        scopes.pop();
    }

    private void declare(Token name) {
        // if its not the initial global scope, as we just resolve it using the name
        if(scopes.isEmpty()) return;

        // add the name to the current scope and set it to false as it hasnt been defined
        Map<String, Boolean> scope = scopes.peek();

         if (scope.containsKey(name.lexeme)) {
            Flux.error(name,
            "Already variable with this name in this local scope.");
        }

        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if(scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if(stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        // check if variable is trying to be accessed inside its own initializer
        if(!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Flux.error(expr.name, "Can't read a local variable in its own initializer.");
        }
        // we used the variable so find hops away
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value); // dispatches visit method
        resolveLocal(expr, expr.name); // find hops when we hit the variable use
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

     @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }
    
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Flux.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }
}