#include "value.h"
#include "memory.h"
#include "common.h"
#include "object.h"

void initValueArray(ValueArray* array) {
    array->count = 0;
    array->capacity = 0;
    array->values = NULL;
}

void writeValueArray(ValueArray* array, Value value) {
    if(array->capacity < array->count + 1) {
        int oldCapacity = array->capacity;
        array->capacity = GROW_CAPACITY(oldCapacity);
        array->values = GROW_ARRAY(Value, array->values, oldCapacity, array->capacity);
    }
    // allocates value
    array->values[array->count] = value;
    array->count++;
}

void freeValueArray(ValueArray* array) {
    FREE_ARRAY(Value, array->values, array->capacity);
    initValueArray(array);
}

void printValue(Value value) {
    switch(value.type) {
        case VAL_BOOL:      printf(AS_BOOL(value) ? "true" : "false"); break;
        case VAL_NUMBER:    printf("%.14g", AS_NUMBER(value)); break;
        case VAL_NULL:      printf("null"); break;
        case VAL_OBJ:       printObject(value); break;
    }
}

// STRICT EQUALITY -- types have to be equal
bool valuesEqual(Value a, Value b){
    if(a.type != b.type) return false;

    switch(a.type) {
        case VAL_BOOL: return AS_BOOL(a) == AS_BOOL(b);
        case VAL_NUMBER: return AS_NUMBER(a) == AS_NUMBER(b);
        case VAL_NULL: return true; // both have to be null to get here so automatically true
        case VAL_OBJ: return AS_OBJ(a) == AS_OBJ(b);
        default:
            return false; // Unreachable
    }
}