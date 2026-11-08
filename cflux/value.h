#pragma once

#include "common.h"

// define here so no circular reference
typedef struct Obj Obj;
typedef struct ObjString ObjString;

// TODO make bools just go straight on the stack no value type
typedef enum {
    VAL_BOOL,
    VAL_NULL,
    VAL_NUMBER,
    VAL_OBJ // anything that lives on Heap
} ValueType;

typedef  struct {
    ValueType type;
    union{
        bool boolean;
        double number;
        Obj* obj;
    } as;
} Value;
// check type easily
#define IS_BOOL(value)      ((value).type == VAL_BOOL)
#define IS_NUMBER(value)    ((value).type == VAL_NUMBER)
#define IS_NULL(value)      ((value).type == VAL_NULL)
#define IS_OBJ(value)       ((value).type == VAL_OBJ)
// unwraps value object to its content
#define AS_OBJ(value)       ((value).as.obj)
#define AS_BOOL(value)      ((value).as.boolean)
#define AS_NUMBER(value)    ((value).as.number)
// wrap content to a value object
#define OBJ_VAL(object)      ((Value){VAL_OBJ, {.obj = (Obj*)(object)}})
#define BOOL_VAL(value)     ((Value){VAL_BOOL, {.boolean = value}})
#define NUMBER_VAL(value)   ((Value){VAL_NUMBER, {.number = value}})
#define NULL_VAL            ((Value){VAL_NULL, {.number = 0}})

typedef struct {
    int count; 
    int capacity;
    Value* values; 
} ValueArray;

bool valuesEqual(Value a, Value b);
void initValueArray(ValueArray* array);
void writeValueArray(ValueArray* array, Value value);
void freeValueArray(ValueArray* array);
void printValue(Value value);