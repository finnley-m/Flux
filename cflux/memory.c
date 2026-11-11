#include "memory.h"
#include "vm.h"
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

static void freeObject(Obj* object) {
    switch (object->type)
    {
    case OBJ_STRING: {
        ObjString* string = (ObjString*)object;
        FREE_ARRAY(char, string->chars, string->length + 1); // +1 \0
        FREE(ObjString, object);
        break;
    }
    default:
        break; // unreachable
    }
}

void freeObjects() {
    Obj* object = vm.objects;
    while(object->next != NULL) {
        Obj* next = object->next;
        freeObject(object);
        object = next;
    }
}