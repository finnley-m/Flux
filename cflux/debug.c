#include "debug.h"

// forward declerations
static int simpleInstruction(const char* name, int offset); 
static int constantInstruction(const char* name, Chunk* chunk, int offset); 
static int longConstantInstruction(const char* name, Chunk* chunk, int offset); 

void dissasembleChunk(Chunk* chunk, const char* name) {
    printf("== %s ==\n", name);

    for(int offset = 0; offset < chunk->count;) {
        offset = dissasembleInstruction(chunk, offset);
    }
}

int dissasembleInstruction(Chunk* chunk, int offset) {
    // 04 means min 4 padding
    printf("%04d ", offset);
    if (offset > 0 && getLineNumber(chunk, offset) == getLineNumber(chunk, offset-1)) {
        printf("%4s ", "|"); 
    } else {
        printf("%4d ", getLineNumber(chunk, offset));
    }

    uint8_t instruction = chunk->code[offset];
    switch(instruction) {
        case OP_CONSTANT:
            return constantInstruction("OP_CONSTANT", chunk, offset);
        case OP_CONSTANT_LONG:
            return longConstantInstruction("OP_CONSTANT_LONG", chunk, offset);
        case OP_RETURN:
            return simpleInstruction("OP_RETURN", offset);
        default:
            printf("Unknown opcode %d\n", instruction);
            return offset + 1;
    }
}

static int simpleInstruction(const char* name, int offset) {
    printf("%s\n", name);
    return offset + 1;
}

static int constantInstruction(const char* name, Chunk* chunk, int offset) {
    uint8_t constant = chunk->code[offset + 1];
    printf("%-16s %4d '", name, constant);
    printValue(chunk->constants.values[constant]);
    printf("'\n");
    return offset + 2;
} 

static int longConstantInstruction(const char* name, Chunk* chunk, int offset) {
    uint8_t b1 = chunk->code[offset + 1];
    uint8_t b2 = chunk->code[offset + 2];
    uint8_t b3 = chunk->code[offset + 3];
    int constant = (b1 << 16) | (b2 << 8) | b3;
    printf("%-16s %4d '", name, constant);
    printValue(chunk->constants.values[constant]);
    printf("'\n");

    return offset + 4;
}

void printValue(Value value) {
    printf("%g", value);
}