#include "common.h"
#include "chunk.h"
#include "debug.h"
#include "vm.h"

int main(int argc, char *argv[]) {
    initVM();

    Chunk chunk;
    initChunk(&chunk);

    for(int i = 0; i < 1000; i++) {
        writeConstant(&chunk, i/3, i);
    }
    writeChunk(&chunk, OP_RETURN, 3);

    //dissasembleChunk(&chunk, "test chunk");

    interpret(&chunk);
    freeVM();

    freeChunk(&chunk);
    return 0;
}
