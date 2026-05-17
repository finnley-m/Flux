package jflux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main entry point for the Flux interpreter.
 * Handles running a script file or launching the interactive prompt.
 */
/**
 * Main entry point for the Flux interpreter.
 * Handles running a script file or launching the interactive prompt.
 */
public class Flux{
    private static final Interpreter interpreter = new Interpreter();

    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    /**
     * Program entry point.
     * 
     * @param args command-line argument
     * @throws IOException if reading input or file fails
     */
    /**
     * Program entry point.
     *
     * @param args command-line arguments
     * @throws IOException if reading input or file fails
     */
    public static void main(String[] args) throws IOException {
        // only parameter we should have it the script we are trying to run
        if(args.length > 1) {
            System.out.println("Usage: jflux [script]");
            System.exit(64); // error code 64 signifies bad arguments
        } else if (args.length == 1){
            // try to run file if file given
            runFile(args[0]);
        } else {
            // if no file given, run code line by line as its typed in
            runPrompt();
        }
    }

    /**
     * Reads and executes source code from a file.
     *
     * @param path path to the source file
     * @throws IOException if file reading fails
     */
    /**
     * Reads and executes source code from a file.
     *
     * @param path path to the source file
     * @throws IOException if file reading fails
     */
    private static void runFile(String path) throws IOException {
        // Converts the path string to an actual path and reads the bytes from that file
        byte[] bytes = Files.readAllBytes(Paths.get(path));

        // convert byte data into string and then run it
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    /**
     * Starts the interactive REPL prompt and excecutes lines entered by the user.
     * 
     * @throws IOException if reading from stdin fails
     */
    /**
     * Starts the interactive REPL prompt and executes lines entered by the user.
     *
     * @throws IOException if reading from stdin fails
     */
    private static void runPrompt() throws IOException {
        // get our input stream from the terminal(System.in)
        InputStreamReader input = new InputStreamReader(System.in);
        // puts multiple characters into a buffer which is faster than constantly 
        // requesting more characters
        BufferedReader reader = new BufferedReader(input);
        
        // while true loop
        for(;;)  {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    /**
     * Scans, parses, and interprets a source string.
     * 
     * @param source source code text
     */
    /**
     * Scans, parses, and interprets a source string.
     *
     * @param source source code text
     */
    private static void run(String source) {
        // scan source text to create tokens
        Scanner scanner    = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        
        // scan tokens to create expressions
        Parser parser   = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        
        // stop if there was a syntax error
        if(hadError) return;

        // evauluate expression using interpreter
        interpreter.interpret(statements);
    }


    /**
     * Reports a syntax error at a given line.
     *
     * @param line source line number
     * @param message error description
     */
    public static void error(int line, String message) {
        report(line, "", message);
    }

    /**
     * Reports a syntax error at a specific token.
     *
     * @param token the token that caused the error
     * @param message error description
     */
    public static void error(Token token, String message) {
        if(token.type == TokenType.EOF){ // specifically say at end of file as EOF isnt code
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    /**
     * Prints a formatted error report and marks that an error occurred.
     *
     * @param line source line number
     * @param where location description
     * @param message error description
     */
    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + " ] Error" + where + ": " + message);
        hadError = true;
    }

    /**
     * Handles runtime errors by printing the message and line.
     *
     * @param error the runtime exception
     */
    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + 
                           "\nline " + error.token.line + "]");
        hadRuntimeError = true;
    }
}