#include "vm.h"
#include "common.h"
#include "debug.h"
#include "compiler.h"

VM vm;

static void resetStack() {
    vm.stackTop = vm.stack;
}

void initVM() {
    resetStack();
}

void freeVM() {

}

void push(Value value) {
    *vm.stackTop = value;
    vm.stackTop++;
}

Value pop() {
    vm.stackTop--;
    return *vm.stackTop;
}

static InterpretResult run() {
#define READ_BYTE() (*vm.pc++)
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
#define READ_CONSTANT_LONG()   (vm.chunk->constants.values[ READ_BYTE()<<16 | READ_BYTE()<<8 | READ_BYTE() ])

// "do" will force semicolon and safe expansion in all statement contexts
#define BINARY_OP(op) \
    do { \
        double b = pop(); \
        double a = pop(); \
        push(a op b); \
    } while (false)
    
      
    for(;;) {
#ifdef DEBUG_TRACE_EXCECUTION
        printf("        ");
        // increments stack values
        for (Value* item = vm.stack; item < vm.stackTop; item++) {
            printf("[ ");
            printValue(*item);
            printf(" ]");
        }
        printf("\n");
        dissasembleInstruction(vm.chunk, (int)(vm.pc - vm.chunk->code));
#endif
        uint8_t instruction;
        switch(instruction = READ_BYTE()) {
            case OP_CONSTANT: {
                Value constant = READ_CONSTANT();
                push(constant);
                break;
            }
            case OP_CONSTANT_LONG: {
                Value constant = READ_CONSTANT_LONG();
                push(constant);
                break;
            }
            case OP_ADD:        BINARY_OP(+); break; 
            case OP_SUBTRACT:   BINARY_OP(-); break; 
            case OP_MULTIPLY:   BINARY_OP(*); break; 
            case OP_DIVIDE:     BINARY_OP(/); break; 
            case OP_NEGATE:     push(-pop()); break;
            case OP_RETURN: {
                printValue(pop());
                printf("\n");
                return INTERPRET_OK;
            }
        }
    }
#undef READ_BYTE
#undef READ_CONSTANT
#undef READ_CONSTANT_LONG
#undef BINARY_OP
}

InterpretResult interpret(const char* source) {
    Chunk chunk;
    initChunk(&chunk);

    if(!compile(source, &chunk)) {
        freeChunk(&chunk);
        return INTERPRET_COMPILE_ERROR;
    }

    vm.chunk = &chunk;
    vm.pc    = vm.chunk->code;

    InterpretResult result = run();

    freeChunk(&chunk);
    return result;
}

