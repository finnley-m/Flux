package jflux;

public class Return extends RuntimeException{
    final Object value;

    Return(Object value) {
        // disables JVM machinery of a normal RuntimeException
        super(null, null, false, false);
        this.value = value;
    }
}
