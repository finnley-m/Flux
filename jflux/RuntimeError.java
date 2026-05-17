package jflux;

/**
 * Runtime exception for interpreter errors.
 */
class RuntimeError extends RuntimeException {
    final Token token;

    /**
     * Creates a runtime error associated with a token.
     *
     * @param token token where error occurred
     * @param message error message
     */
    RuntimeError(Token token, String message) {
        // store error message in parent
        super(message);
        this.token = token;
    }
}
