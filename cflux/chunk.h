/*
    A chunk refers to sequences of bytecode
*/

#pragma once

#include "common.h"
#include "memory.h"
#include "value.h"

typedef enum {
    OP_CONSTANT,
    OP_CONSTANT_LONG,
    OP_RETURN,
} OpCode;

typedef struct {
    int count;
    int capacity;
    uint8_t* code;
    int lineCount;
    int lineCapacity;
    int* lines;
    ValueArray constants; // store constants in a table rather than with instruction
} Chunk;

void initChunk(Chunk* chunk);
void freeChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, uint8_t byte, int line);
void writeConstant(Chunk* chunk, Value value, int line);
int addConstant(Chunk* chunk, Value value);
int getLineNumber(Chunk* chunk, int offset);