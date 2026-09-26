#include "memory.h"

/*
    oldSize     newSize     Operation
    0           Non-Zero    Allocate New Block
    Non-Zero    0           Free Allocation
    Non-Zero    smaller     Shrink
    Non-Zero    larger      Grow
*/
void* reallocate(void* pointer, size_t oldSize, size_t newSize) {
    if (newSize == 0) {
        free(pointer);
        return NULL;
    }

    void* result = realloc(pointer, newSize);
    if (result == NULL) exit(1);
    return result;
}