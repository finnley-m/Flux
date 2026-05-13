package jflux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Flux{
    private static final Interpreter interpreter = new Interpreter();

    static boolean hadError = false;
    static boolean hadRuntimeError = false;

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

    private static void runFile(String path) throws IOException {
        // Converts the path string to an actual path and reads the bytes from that file
        byte[] bytes = Files.readAllBytes(Paths.get(path));

        // convert byte data into string and then run it
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

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

    private static void run(String source) {
        // scan source text to create tokens
        Scanner scanner    = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        
        // scan tokens to create expressions
        Parser parser   = new Parser(tokens);
        Expr expression = parser.parse();
        
        // stop if there was a syntax error
        if(hadError) return;

        // evauluate expression using interpreter
        interpreter.interpret(expression);


        // print the scanned tokens
        System.out.println(new AstPrinter().print(expression));
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    public static void error(Token token, String message) {
        if(token.type == TokenType.EOF){ // specifically say at end of file as EOF isnt code
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + " ] Error" + where + ": " + message);
        hadError = true;
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + 
                           "\nline " + error.token.line + "]");
        hadRuntimeError = true;
    }
}