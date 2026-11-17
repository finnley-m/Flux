#pragma once
#include "chunk.h"
#include "value.h" 
#include "table.h"

#define STACK_MAX 256

typedef struct {
    Chunk* chunk;
    uint8_t* pc; // program counter
    Value stack[STACK_MAX];
    Value* stackTop;
    Table strings; // hash table to store all the vms strings
    Table globals; // store global variables

    Obj* objects; // linked list to store all allocated objects
} VM;

typedef enum {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_RUNTIME_ERROR
} InterpretResult;

extern VM vm;

void initVM();
void freeVM();
InterpretResult interpret(const char* source);
void push(Value value);
Value pop();