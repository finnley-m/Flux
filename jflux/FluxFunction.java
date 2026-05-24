package jflux;

import java.util.List;

public class FluxFunction implements FluxCallable{
    private final Stmt.Function declaration; // declaration holds everything about the function, name, param, body
    private final Environment closure;
    FluxFunction(Stmt.Function declaration, Environment closure) {
        this.closure     = closure;
        this.declaration = declaration;
    }

    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        try{
            interpreter.excecuteBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
