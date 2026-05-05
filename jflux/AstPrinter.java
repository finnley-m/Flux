package jflux;

import jflux.Expr;

// Ast Printer is our main debugger for the tree to see how the tree forms and stuff

// Means AstPrinter implements the Visitor Interface
// and will return a string in the interfaces methods.
public class AstPrinter implements Expr.Visitor<String>{
    String print(Expr expr) {
        return expr.accept(this); // calls the method based on the subclass of the expression
    }
 
    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right); // e.g. (+ 2 3)
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    // Literals are the base case which collapses the recursion+
    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "null";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            // This is where recursion happens. Each expression is resolved as we get to it,
            // creating the expressions continually and print the whole Abstract Syntax Tree
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
            new Expr.Unary(
                new Token(TokenType.MINUS, "-", null, 1),
                new Expr.Literal(123)
            ),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(new Expr.Literal(45.67)));

        System.out.println(new AstPrinter().print(expression));
    }
}
