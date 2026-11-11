#include "object.h"

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

static ObjString* allocateString(const char* chars, int length, uint32_t hash) {
    ObjString* string = ALLOCATE_EXTRA(ObjString, length + 1, OBJ_STRING);
    string->length = length;
    string->hash   = hash;
    memcpy(string->chars, chars, length);
    string->chars[length] = '\0';
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
ObjString* takeString(const char* chars, int length) {
    uint32_t hash = hashString(chars, length);

    return allocateString(chars, length, hash);
}
// take a string it doesnt own and makes a copy
ObjString* copyString(const char* chars, int length) {
    uint32_t hash = hashString(chars, length);

    return allocateString(chars, length, hash);
}

void printObject(Value value) {
    switch(OBJ_TYPE(value)) {
        case OBJ_STRING:
            printf("%s", AS_CSTRING(value));    
            break;
    }
}