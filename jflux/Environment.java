package jflux;

import java.util.HashMap; // actual storage
import java.util.Map; // interface

/**
 * Place where variables are stored in a hash map
*/
public class Environment {
    // string -> variable name   object -> actual data
    private final Map<String, Object> values = new HashMap<>();

    /**
     * get the value associateed with a variable name
     */
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        throw new RuntimeError(name, "Undefined Variable '" + name.lexeme + "'.");
    }

    // TODO: Rn redefinition is allowed everywhere mabe only allow it in REPL and not scripts
    /**
     * add the variable name and value to the values map
     */
    void define(String name, Object object) {
        values.put(name, object);
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        throw new RuntimeError(name, "Undefined Variable '" + name.lexeme + "'.");
    }
}
