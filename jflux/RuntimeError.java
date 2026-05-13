package jflux;

class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        // store error message in parent
        super(message);
        this.token = token;
    }
}
