package jflux;

import static jflux.TokenType.BANG_EQUAL;
import static jflux.TokenType.EQUAL_EQUAL;
import static jflux.TokenType.GREATER;
import static jflux.TokenType.GREATER_EQUAL;
import static jflux.TokenType.LESS;
import static jflux.TokenType.LESS_EQUAL;
import static jflux.TokenType.PLUS;

import jflux.Expr.Unary;

// declare the class as a visitor to the expressions
public class Interpreter implements Expr.Visitor<Object> {
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

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

                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings");

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

    public void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error){
            Flux.runtimeError(error);
        }
    }

    // lets the expression route itself to the correct expression type using accept()
    public Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if(operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        // if object is a boolean return it
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) 
    { // == compares pointers not memory so we need to check like this
        if(a == null && b == null) return true;
        if(a == null)              return false;

        return a.equals(b);
    }

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