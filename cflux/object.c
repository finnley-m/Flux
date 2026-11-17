#include "object.h"
#include "table.h"
#include "common.h"
#include "memory.h"
#include "value.h"
#include "vm.h"

// allocates space on the heap for the parent object
#define ALLOCATE_OBJ(type, objectType) \
    (type*)allocateObject(sizeof(type), objectType); // sizeof(type) so give space for fields

#define ALLOCATE_EXTRA(type, extra, objectType) \
    (type*)allocateObject(sizeof(type) + extra, objectType);

static Obj* allocateObject(size_t size, ObjType type) {
    Obj* object = (Obj*)reallocate(NULL, 0, size);
    object->type = type;

    // allocated object points to first element in object list
    // list start then points to new object, adding the object to the list
    object->next = vm.objects;
    vm.objects   = object;

    return object;
}

// create and allocate a string object with the given data, and then returning it
static ObjString* allocateString(const char* chars, int length, uint32_t hash) {
    ObjString* string = ALLOCATE_EXTRA(ObjString, length + 1, OBJ_STRING);
    string->length = length;
    string->hash   = hash;
    memcpy(string->chars, chars, length);
    string->chars[length] = '\0';

    tableSet(&vm.strings, string, NULL_VAL); // null as were just using it to store the actual string for interning

    return string;
}

static uint32_t hashString(const char* key, int length) {
    uint32_t hash = 2166136261u;

    for(int i = 0; i < length; i++) {
        hash ^= key[i];
        hash *= 16777619;
    }

    return hash;
}

// takes ownership of already allocated string
// have to free in both branches as we take full ownership either way
ObjString* takeString(char* chars, int length) {
    uint32_t hash = hashString(chars, length);

    // look up string in table to see if its already made where we can just
    // use a referance to that string instead
    ObjString* interned = tableFindString(&vm.strings, chars, length, hash);
    if(interned != NULL) {
         FREE_ARRAY(char, chars, length + 1);
        return interned;
    }

    ObjString* result = allocateString(chars, length, hash);
    FREE_ARRAY(char, chars, length + 1);
    return result;
}
// take a string it doesnt own and makes a copy
ObjString* copyString(const char* chars, int length) {
    uint32_t hash = hashString(chars, length);

    ObjString* interned = tableFindString(&vm.strings, chars, length, hash);
    if(interned != NULL) {
        return interned;
    }

    return allocateString(chars, length, hash);
}

void printObject(Value value) {
    switch(OBJ_TYPE(value)) {
        case OBJ_STRING:
            printf("%s", AS_CSTRING(value));    
            break;
    }
}