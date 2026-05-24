package jflux;

import java.util.List;

// anything which we can call using a ()
public interface FluxCallable {
    int arity(); // num of arguments
    Object call(Interpreter interpreter, List<Object> arguments);
}