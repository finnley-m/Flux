package jflux.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
    if (args.length != 1) {
        System.err.println("Usage: generate_ast <output directory>");
        System.exit(64);
    }
    String outputDir = args[0];
    defineAst(outputDir, "Expr", Arrays.asList(
    "Binary : Expr left, Token operator, Expr right",
        "Grouping : Expr expression",
        "Literal : Object value",
        "Unary : Token operator, Expr right"
    ));
    defineAst(outputDir, "Stmt", Arrays.asList(
  "Expression : Expr expression",
        "Print : Expr expression"
    ));
    } 

    private static void defineAst(String outputDir,
                              String baseName,  // name of the base class being generated
                              List<String> types) throws IOException {

        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package jflux;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        // AST classes :
        for (String type : types) {
            String className = type.split(":")[0].trim(); // name of the specific
            String fields    = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        // the base accept() method we will override
        writer.println();
        writer.println("\tabstract<R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }
    
    private static void defineType(PrintWriter writer,
                                   String baseName,
                                   String className,
                                   String fieldList ) {
        
        writer.println("\tstatic class " + className + " extends " + baseName + " {");

        // constructor
        writer.println("\t" + className + "(" + fieldList + ") {");

        // store parameters in fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("\t\tthis." + name + " = " + name + ";");
        }
        writer.println("\t\t}");

        // visitor pattern
        // each subclass implements that and calls the right visit method for its own type
        writer.println();
        writer.println("\t\t@Override");
        // generic return type R
        writer.println("\t\t<R> R accept(Visitor<R> visitor) {");
        // e.g. return visitor.visitBinaryExpr(this)
        // this will pass the instance of the class Binary with all its relevant fields (left, operator, right)
        // into the visitor, e.g. Interpreter which will evaluate the result and return it
        writer.println("\t\t\treturn visitor.visit" + className + baseName + "(this);");
        writer.println("\t\t}");

        // fields
        writer.println();
        for (String field : fields) {
            writer.println("\t\tfinal " + field + ";");
        }

        writer.println("\t}");
    }

    /* Binary Example:
    Parser builds a Binary node
    Interpreter calls expr.accept(this)
    Binary.accept calls visitor.visitBinary(this)
    Interpreter.visitBinary evaluates left and right sides and applies the evaluation
    */
    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        // <R> means the visitor can return any type. R stands for return type
        writer.println("\tinterface Visitor<R> {");

        // Here, we iterate through all of the subclasses and declare a visit method for each one. 
        // When we define new expression types later, this will automatically include them.
        for(String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("\t\tR visit" + typeName + baseName + "("
                           + typeName + " " + baseName.toLowerCase() + ");" 
            );
        }

        writer.println("\t}");
    }
}
