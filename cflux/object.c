#include "object.h"

#include "common.h"
#include "memory.h"
#include "value.h"
#include "vm.h"

// allocates space on the heap for the parent object
#define ALLOCATE_OBJ(type, objectType) \
    (type*)allocateObject(sizeof(type), objectType); // sizeof(type) so give space for fields

static Obj* allocateObject(size_t size, ObjType type) {
    Obj* object = (Obj*)reallocate(NULL, 0, size);
    object->type = type;
    return object;
}

static ObjString* allocateString(char* chars, int length) {
    ObjString* string = ALLOCATE_OBJ(ObjString, OBJ_STRING);
    string->length = length;
    string->chars = chars;
    
    return string;
}
// takes ownership of already allocated string
ObjString* takeString(char* chars, int length) {
    return allocateString(chars, length);
}
// take a string it doesnt own and makes a copy
ObjString* copyString(const char* chars, int length) {
    char* heapChars = ALLOCATE(char, length + 1); // + 1 for '\0'
    memcpy(heapChars, chars, length); // copy from text to our heapChars var
    heapChars[length] = '\0';

    return allocateString(heapChars, length);
}

void printObject(Value value) {
    switch(OBJ_TYPE(value)) {
        case OBJ_STRING:
            printf("%s", AS_CSTRING(value));    
            break;
    }
}