#include "chunk.h"

// initializes the chunks fields
void initChunk(Chunk* chunk) {
    chunk->count = 0;
    chunk->capacity = 0;
    chunk->code = NULL;
    chunk->lineCount = 0;
    chunk->lineCapacity = 0;
    chunk->lines = NULL;
    initValueArray(&chunk->constants);
}
// writes the byte to the chunks code
void writeChunk(Chunk* chunk, uint8_t byte, int line) {
    if(chunk->capacity < chunk->count+1) {
        int oldCapacity = chunk->capacity;
        chunk->capacity = GROW_CAPACITY(oldCapacity);
        chunk->code     = GROW_ARRAY(uint8_t, chunk->code, oldCapacity, chunk->capacity);
    } 
    // check if old run or new run and add line info accordingly
    if(chunk->lineCount > 0 && chunk->lines[chunk->lineCount-2] == line) {
        // same run
        chunk->lines[chunk->lineCount-1]++;
    }
    // new run
    else {
        // check for array sizing as we are adding
        // + 2 as we need to add the run length aswell
        if(chunk->lineCapacity < chunk->lineCount+2) {
            int oldCapacity = chunk->lineCapacity;
            chunk->lineCapacity = GROW_CAPACITY(oldCapacity);
            chunk->lines    = GROW_ARRAY(int, chunk->lines, oldCapacity, chunk->lineCapacity);
        }
        
        chunk->lines[chunk->lineCount] = line;
        chunk->lines[chunk->lineCount+1] = 1;
        chunk->lineCount+=2;
    }

    // add bytecode to the chunk
    chunk->code[chunk->count] = byte;
    chunk->count++;
}

// frees up the chunks memory and sets its values back to default
void freeChunk(Chunk* chunk) {
    FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
    FREE_ARRAY(int, chunk->lines, chunk->lineCapacity);
    freeValueArray(&chunk->constants);
    initChunk(chunk);
}

void writeConstant(Chunk* chunk, Value value, int line) {
    // write constant to the value array and save its address
    int constantIndex = addConstant(chunk, value);

    if(constantIndex <= 255) { // can use OP_CONSTANT
        writeChunk(chunk, OP_CONSTANT, line);
        writeChunk(chunk, constantIndex, line);
    }
     else{ // use OP_CONSTANT_LONG
        uint8_t b1 = constantIndex >> 16;
        uint8_t b2 = constantIndex >> 8;
        uint8_t b3 = constantIndex;       
        writeChunk(chunk, OP_CONSTANT_LONG, line);
        writeChunk(chunk, b1, line);
        writeChunk(chunk, b2, line);
        writeChunk(chunk, b3, line);
    }
}

/*
    Writes a constant to the chunks value array
    returns the index of the constant
*/
int addConstant(Chunk* chunk, Value value) {
    writeValueArray(&chunk->constants, value);
    return chunk->constants.count - 1;
}

int getLineNumber(Chunk* chunk, int offset) {
    // safety check to make sure we dont overflow the array or give a negative bound
    if(offset > chunk->count-1 || offset < 0) return -1;

    int linesCounted = 0;
    int lineNum   = 0;
    int count     = 0;
    while(linesCounted <= offset) {
        lineNum    = chunk->lines[count];
        linesCounted += chunk->lines[count+1];
        count+=2;
    }
    return lineNum;
}