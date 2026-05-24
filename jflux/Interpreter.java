package jflux;

import java.util.List;
import java.util.ArrayList;

import static jflux.TokenType.*;


// declare the class as a visitor to the expressions
/**
 * Evaluates expressions and statements using the visitor pattern.
 */
public class Interpreter implements Expr.Visitor<Object>,
                                    Stmt.Visitor<Void> {
    
    // variables stay in memory as long as the interpreter is running
    final Environment globals = new Environment();
    private Environment environment = globals;
    
    public boolean isRepl = false;

    // TODO TEMP NATIVE FUNCTION, MOVE LATER TO AN FFI
    Interpreter() {
        globals.define("clock", new FluxCallable() {
            @Override
            public int arity() {return 0;}

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
            });
        };

    /**
     * Evaluates the given expression and prints the result.
     *
     * @param expression expression to interpret
     */
    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                excecute(statement);
            }
        } catch (RuntimeError error){
            Flux.runtimeError(error);
        }
    }

    /**
     * excuecutes a statement
     */
    private void excecute(Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * excecutes a block of statements in relation to a specific environment(scope)
     */
    public void excecuteBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            // temp set current environment to a new local environment
            this.environment = environment;

            for (Stmt statement : statements) {
                excecute(statement);
            }
        } finally {
            // set current environment to what it was before, reinstating the scope.
            this.environment = previous;
        }
    }

    /**
     * Visits a block statement node.
     * 
     * Excecutes all lines in the block and also creates a new local environment with a reference to the parent environment
     * 
     * @param stmt block statement
     * @return null
     */
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        excecuteBlock(stmt.statements, new Environment(environment));
        return null;
    }

    /**
     * Visits an expression statement node.
     *
     * @param stmt expression statement
     * @return null
     */
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        Object value = evaluate(stmt.expression);
        if(isRepl) System.out.println(value); // TODO FIX SO IT IS ONLY PRINTED IF ITS A STANDALONE EXPRESSION IF PROBLEM
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        FluxFunction function = new FluxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function); 
        return null;
    }

    /**
     * Logic for if statements
     */
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            excecute(stmt.thenBranch);
        } else if(stmt.elseBranch != null) {
            excecute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while(isTruthy(evaluate(stmt.condition))) {
            excecute(stmt.body);
        }
        return null;
    }

    /**
     * Visits a print statement node.
     */
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if(stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    /**
     * visits a Var statement node for defining variables
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null; // null if no initializer given
        if (stmt.initializer != null){
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    /**
     * return the variable associated with the specific name to be evaluated in the expression
     */
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    /**
     * reassign a variables value
     */
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    /**
     * Visits a literal expression node and returns its value.
     *
     * @param expr literal expression
     * @return literal value
     */
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if(isTruthy(left)) return left;
        } else { // and
            // left false to return false
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    /**
     * Visits a grouping expression node.
     *
     * @param expr grouping expression
     * @return evaluated inner expression
     */
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    /**
     * Visits a unary expression and applies the operator.
     *
     * @param expr unary expression
     * @return evaluated value
     */
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            default:
                break;
        }

        //unreachable
        return null;
    }

    /**
     * Visits a binary expression and applies the operator to both operands.
     *
     * @param expr binary expression
     * @return evaluated value
     */
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left  = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type) {
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                else if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }

                else if (left instanceof String && right instanceof Double ||
                        left instanceof Double && right instanceof String
                ) {
                    return (String)left.toString() + (String)right.toString();
                }

                throw new RuntimeError(expr.operator, "Operands must be number and string on both side");

            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case BANG_EQUAL:  return !isEqual(left, right);
            case EQUAL_EQUAL: return  isEqual(left, right);
            default:
                return null;
        }
    }

    public Object visitCallExpr(Expr.Call expr) {
        // looks up function name in environmentand returns the object of it
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if(!(callee instanceof FluxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes");
        }
        FluxCallable function = (FluxCallable)callee;

        // check argument size given
        if(arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments, but got "
                                                + arguments.size() + ".");
        }

        // reference to interpreter so the funciton can excecute its body
        return function.call(this, arguments);
    }

    // lets the expression route itself to the correct expression type using accept()
    /**
     * Evaluates an expression by dispatching to the visitor method.
     *
     * @param expr expression to evaluate
     * @return evaluated value
     */
    public Object evaluate(Expr expr) {
        return expr.accept(this);
    }
    

    /**
     * Ensures a unary operand is a number.
     *
     * @param operator operator token
     * @param operand operand value
     */
    private void checkNumberOperand(Token operator, Object operand) {
        if(operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    /**
     * Ensures both binary operands are numbers.
     *
     * @param operator operator token
     * @param left left operand
     * @param right right operand
     */
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers");
    }

    /**
     * Determines truthiness for the language semantics.
     *
     * @param object value to test
     * @return true if truthy
     */
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        // if object is a boolean return it
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    /**
     * Checks equality between two values.
     *
     * @param a first value
     * @param b second value
     * @return true if equal
     */
    private boolean isEqual(Object a, Object b) 
    { // == compares pointers not memory so we need to check like this
        if(a == null && b == null) return true;
        if(a == null)              return false;

        return a.equals(b);
    }

    /**
     * Converts a runtime value to its string representation.
     *
     * @param object value to stringify
     * @return string form
     */
    private String stringify(Object object) {
        if (object == null) return "null";
        
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length()-2); // gets rid of .0
            }
            return text;
        }

        return object.toString();
    }
}