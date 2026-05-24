package jflux;

import java.util.HashMap; // actual storage
import java.util.Map; // interface

/**
 * Place where variables are stored in a hash map
*/
public class Environment {
    final Environment enclosing;

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    // string -> variable name   object -> actual data
    private final Map<String, Object> values = new HashMap<>();

    /**
     * get the value associateed with a variable name
     */
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if(enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined Variable '" + name.lexeme + "'.");
    }

    // TODO: Rn redefinition is allowed everywhere mabe only allow it in REPL and not scripts
    /**
     * add the variable name and value to the values map
     */
    void define(String name, Object object) {
        values.put(name, object);
    }

    Object getAt(int distance, String name) {
        // gets the value of the ancestor that distance away
        return ancestor(distance).values.get(name);
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }

        return environment;
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if(enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined Variable '" + name.lexeme + "'.");
    }
}
