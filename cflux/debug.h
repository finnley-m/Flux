#pragma once

#include "chunk.h"
#include "common.h"
#include "value.h"

void dissasembleChunk(Chunk* chunk, const char* name);
int dissasembleInstruction(Chunk* chunk, int offset);
void printValue(Value value);