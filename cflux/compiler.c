#include "common.h"
#include "compiler.h"
#include "scanner.h"

#ifdef DEBUG_PRINT_CODE
    #include "debug.h"
#endif

typedef struct {
    Token current;
    Token previous;
    bool hadError;
    bool panicMode;
} Parser;

typedef enum {
    PREC_NONE,
    PREC_ASSIGNMENT,    // =
    PREC_OR,            // or
    PREC_AND,           // and
    PREC_EQUALITY,      // == !=
    PREC_COMPARISON,    // > < >= <=
    PREC_TERM,          // + -
    PREC_FACTOR,        // * /
    PREC_UNARY,         // ! -
    PREC_CALL,          // . ()
    PREC_PRIMARY
} Precedence;

typedef void (*ParseFn)(bool canAssign); // function that takes no args and returns nothing

typedef struct {
    ParseFn prefix;
    ParseFn infix;
    Precedence precedence;
} ParseRule;

Parser parser;

Chunk* compilingChunk;

static void consume(TokenType type, const char* message);
static void emitByte(uint8_t byte);
static void emitBytes(uint8_t b1, uint8_t b2);
static int makeConstant(Value value);

static void expression();
static void statement();
static void declaration();
static ParseRule* getRule(TokenType type);
static void parsePrecedence(Precedence precedence);


static int identifierConstant(Token* name) {
    return makeConstant(OBJ_VAL(copyString(name->start, name->length)));
}

static int parseVariable(const char* errorMessage) {
    consume(TOKEN_IDENTIFIER, errorMessage);
    return identifierConstant(&parser.previous);
}

static void emitVariableOp(int index, uint8_t shortOp, uint8_t longOp) {
    if(index <= 255) {
        emitBytes(shortOp, index);
    } else {
        emitByte(longOp);
        emitByte((uint8_t)(index >> 16));
        emitByte((uint8_t)(index >> 8));
        emitByte((uint8_t)index);
    }
}

static void defineVariable(int global) {
    emitVariableOp(global, OP_DEFINE_GLOBAL, OP_DEFINE_GLOBAL_LONG);
}

static Chunk* currentChunk() {
    return compilingChunk;
}

static void errorAt(Token* token, const char* message) {
    if(parser.panicMode) return; // supress errors caused by the initial errors
    parser.panicMode = true;

    fprintf(stderr, "[line %d] Error", token->line);

    if(token->type == TOKEN_EOF) {
        fprintf(stderr, " at end");
    } else if(token->type == TOKEN_ERROR) {
        // Nothing as message describes it alr
    }
    else { 
        fprintf(stderr, " at '%.*s'", token->length, token->start);
    }

    fprintf(stderr, ": %s\n", message);
    parser.hadError = true;
}


static void error(const char* message) {
    errorAt(&parser.previous, message);
}

static void errorAtCurrent(const char* message) {
    errorAt(&parser.current, message);
}

// advances to next valid token
static void advance() {
    parser.previous = parser.current;

    for(;;) {
        parser.current = scanToken();
        if (parser.current.type != TOKEN_ERROR) break;

        errorAtCurrent(parser.current.start);
    }
}
// advances to next token and throws error if not of expected type
static void consume(TokenType type, const char* message) {
    if(parser.current.type == type) {
        advance();
        return;
    }

    error(message);
}

static bool check(TokenType type) {
    return parser.current.type == type;
}

static bool match(TokenType type) {
    if(!check(type)) return false;
    advance(); // if match given type, consume it otherwise leave it alone  
    return true;
}

// write bytecode to the chunk
static void emitByte(uint8_t byte) {
    writeChunk(currentChunk(), byte, parser.previous.line);
}

static void emitBytes(uint8_t b1, uint8_t b2) {
    emitByte(b1);
    emitByte(b2);
}

static void emitReturn() {
    emitByte(OP_RETURN);
}

static int makeConstant(Value value) {
    int constant = addConstant(currentChunk(), value);
    return constant;
}

static void emitConstant(Value value) {
    writeConstant(currentChunk(), value, parser.previous.line);
}

static void endCompiler() {
    emitReturn();// temporary to print result
#ifdef DEBUG_PRINT_CODE
    if(!parser.hadError) {
        dissasembleChunk(currentChunk(), "code");
    }
#endif
}


static void binary(bool canAssign) {
    // Remember operator type
    TokenType operatorType = parser.previous.type;

    // compile the right operand if binds tighter than current
    // -> +1 gives left assiciativity
    ParseRule* rule = getRule(operatorType);
    parsePrecedence((Precedence)(rule->precedence+1));

    // emit the operator instruction
    switch (operatorType) {
        // logic
        case TOKEN_BANG_EQUAL:      emitBytes(OP_EQUAL, OP_NOT); break;
        case TOKEN_EQUAL_EQUAL:     emitByte(OP_EQUAL); break;
        case TOKEN_GREATER:         emitByte(OP_GREATER); break;
        case TOKEN_GREATER_EQUAL:   emitBytes(OP_LESS, OP_NOT); break; // contrapositive
        case TOKEN_LESS:            emitByte(OP_LESS); break;
        case TOKEN_LESS_EQUAL:      emitBytes(OP_GREATER, OP_NOT); break; // contrapositive
        // arithmetic
        case TOKEN_PLUS:            emitByte(OP_ADD); break;
        case TOKEN_MINUS:           emitByte(OP_SUBTRACT); break;
        case TOKEN_STAR:            emitByte(OP_MULTIPLY); break;
        case TOKEN_SLASH:           emitByte(OP_DIVIDE); break;
        default:
            return; // Unreachable
    }
}

static void literal(bool canAssign) {
    switch(parser.previous.type) {
        case TOKEN_FALSE: emitByte(OP_FALSE); break;
        case TOKEN_TRUE: emitByte(OP_TRUE); break;
        case TOKEN_NULL: emitByte(OP_NULL); break;
        default: return;
    }
}

static void grouping(bool canAssign) {
    expression();
    consume(TOKEN_RIGHT_PAREN, "expect ')' after expression");
}
// if number just add it to the stack
static void number(bool canAssign) {
    double value = strtod(parser.previous.start, NULL); // string to double
    emitConstant(NUMBER_VAL(value));
}

static void string(bool canAssign) {
    // + 1 and -2 get rid of the quotations
    emitConstant(OBJ_VAL(copyString(parser.previous.start + 1, parser.previous.length - 2)));
}

static void namedVariable(Token name, bool canAssign) {
    int arg = identifierConstant(&name);

    if(canAssign && match(TOKEN_EQUAL)){
        expression(); // expression after equal
        emitVariableOp(arg, OP_SET_GLOBAL, OP_SET_GLOBAL_LONG);        
    } else {
        emitVariableOp(arg, OP_GET_GLOBAL, OP_GET_GLOBAL_LONG);
    }
}

static void variable(bool canAssign) {
    namedVariable(parser.previous, canAssign);
}

static void unary(bool canAssign) {
    TokenType operatorType = parser.previous.type;

    // compile operand
    parsePrecedence(PREC_UNARY);
    
    switch(operatorType) {
        case TOKEN_MINUS: emitByte(OP_NEGATE); break;
        case TOKEN_BANG:  emitByte(OP_NOT); break;
        default: return;
    }
}
// A lookup table mapping every token type
// to its prefix parse function, infix parse function, and precedence level.
ParseRule rules[] = {
    [TOKEN_LEFT_PAREN]      = {grouping, NULL, PREC_NONE},
    [TOKEN_RIGHT_PAREN]     = {NULL, NULL, PREC_NONE},
    [TOKEN_LEFT_BRACE]      = {NULL, NULL, PREC_NONE},
    [TOKEN_RIGHT_BRACE]     = {NULL, NULL, PREC_NONE},
    [TOKEN_COMMA]           = {NULL, NULL, PREC_NONE},
    [TOKEN_DOT]             = {NULL, NULL, PREC_NONE},
    [TOKEN_MINUS]           = {unary, binary, PREC_TERM},
    [TOKEN_PLUS]            = {NULL, binary, PREC_TERM},
    [TOKEN_SEMICOLON]       = {NULL, NULL, PREC_NONE},
    [TOKEN_SLASH]           = {NULL, binary, PREC_FACTOR},
    [TOKEN_STAR]            = {NULL, binary, PREC_FACTOR},
    [TOKEN_BANG]            = {unary, NULL, PREC_NONE},
    [TOKEN_BANG_EQUAL]      = {NULL, binary, PREC_EQUALITY},
    [TOKEN_EQUAL]           = {NULL, NULL, PREC_NONE},
    [TOKEN_EQUAL_EQUAL]     = {NULL, binary, PREC_EQUALITY},
    [TOKEN_GREATER]         = {NULL, binary, PREC_COMPARISON},
    [TOKEN_GREATER_EQUAL]   = {NULL, binary, PREC_COMPARISON},
    [TOKEN_LESS]            = {NULL, binary, PREC_COMPARISON},
    [TOKEN_LESS_EQUAL]      = {NULL, binary, PREC_COMPARISON},
    [TOKEN_IDENTIFIER]      = {variable, NULL, PREC_NONE},
    [TOKEN_STRING]          = {string, NULL, PREC_NONE},
    [TOKEN_NUMBER]          = {number, NULL, PREC_NONE},
    [TOKEN_AND]             = {NULL, NULL, PREC_NONE},
    [TOKEN_CLASS]           = {NULL, NULL, PREC_NONE},
    [TOKEN_ELSE]            = {NULL, NULL, PREC_NONE},
    [TOKEN_FALSE]           = {literal, NULL, PREC_NONE},
    [TOKEN_FOR]             = {NULL, NULL, PREC_NONE},
    [TOKEN_FUNC]            = {NULL, NULL, PREC_NONE},
    [TOKEN_IF]              = {NULL, NULL, PREC_NONE},
    [TOKEN_NULL]            = {literal, NULL, PREC_NONE},
    [TOKEN_OR]              = {NULL, NULL, PREC_NONE},
    [TOKEN_PRINT]           = {NULL, NULL, PREC_NONE},
    [TOKEN_RETURN]          = {NULL, NULL, PREC_NONE},
    [TOKEN_SUPER]           = {NULL, NULL, PREC_NONE},
    [TOKEN_THIS]            = {NULL, NULL, PREC_NONE},
    [TOKEN_TRUE]            = {literal, NULL, PREC_NONE},
    [TOKEN_VAR]             = {NULL, NULL, PREC_NONE},
    [TOKEN_WHILE]           = {NULL, NULL, PREC_NONE},
    [TOKEN_ERROR]           = {NULL, NULL, PREC_NONE},
    [TOKEN_EOF]             = {NULL, NULL, PREC_NONE},
};

// parse and compile any expression at or above the given precedence level
// because that is when that particular expression ends
static void parsePrecedence(Precedence precedence) {
    advance(); // read next token
    ParseFn prefixRule = getRule(parser.previous.type)->prefix; // find its prefix function
    if (prefixRule == NULL) {
        error("Expect expression.");
        return;
    }
    // makes sure u can only assign when legal
    bool canAssign = precedence <= PREC_ASSIGNMENT;
    prefixRule(canAssign); // start parsing the left side of the expression

    // keep consuming infix operators as long as they bind tighter than our current precedence  
    // if it is a lower precedence then that means you have parsed as much of the expression as you can
    while (precedence <= getRule(parser.current.type)->precedence) {
        advance();
        ParseFn infixRule = getRule(parser.previous.type)->infix;
        infixRule(canAssign);
    }

    // if = is still there unconosumed fire error
    if(canAssign && match(TOKEN_EQUAL)) {
        error("Invalid assignment target.");
    }
}
// return the whole rule record
static ParseRule* getRule(TokenType type) {
    return &rules[type];
}

static void expression() {
    parsePrecedence(PREC_ASSIGNMENT); // start at lowest precedence and let the operators precedence take over if higher
}

static void varDeclaration() {
    int global = parseVariable("Expext variable name.");

    if(match(TOKEN_EQUAL)) {
        expression();
    } else {
        emitByte(OP_NULL);
    }
    consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration");
    defineVariable(global);
}

static void expressionStatement() {
    expression();
    consume(TOKEN_SEMICOLON, "Expect ';' after expression.");
    emitByte(OP_POP); // pop as we have no use for result
}

static void printStatement() {
    expression();
    consume(TOKEN_SEMICOLON, "Expect ';' after value.");
    emitByte(OP_PRINT);
}

static void synchronize() {
    parser.panicMode = false;

    while(parser.current.type != TOKEN_EOF) {
        // return at end of current statement
        if(parser.previous.type == TOKEN_SEMICOLON) return;

        switch (parser.current.type)
        {
        // return at statement boundary
        case TOKEN_CLASS:
        case TOKEN_FUNC:
        case TOKEN_VAR:
        case TOKEN_FOR:
        case TOKEN_IF:
        case TOKEN_WHILE:
        case TOKEN_PRINT:
        case TOKEN_RETURN:
            return;

        default:
            // Do nothing.
            ;
        }

        advance();
    }
}

static void declaration() {
    if(match(TOKEN_VAR)) {
        varDeclaration();    
    } else {
        statement();
    }
    // if hit a compile error in previous statement, resynchronize here
    if(parser.panicMode) synchronize();
}

static void statement() {
    if(match(TOKEN_PRINT)) {
        printStatement();
    } else {
        expressionStatement();
    }
}

bool compile(const char* source, Chunk* chunk) {
    initScanner(source);
    compilingChunk = chunk;

    parser.hadError  = false;
    parser.panicMode = false;

    advance(); // primes the parser and loads first token

    while(!match(TOKEN_EOF)) {
        declaration();
    }

    consume(TOKEN_EOF, "Expect end of expression.");
    endCompiler();
    return !parser.hadError;
}