#pragma once

#include "common.h"
#include "value.h"

#define OBJ_TYPE(object) (AS_OBJ(object)->type)

#define IS_STRING(value) isObjType(value, OBJ_STRING)

#define AS_STRING(value)    ((ObjString*)AS_OBJ(value))
#define AS_CSTRING(value)   (((ObjString*)AS_OBJ(value))->chars)

typedef enum {
    OBJ_STRING,
} ObjType;

struct Obj {
    ObjType type;
};

struct ObjString {
    Obj obj; // struct expands inside in order so they have the same location
    int length;
    char* chars;
};

ObjString* takeString(char* chars, int length);
ObjString* copyString(const char* chars, int length);
void printObject(Value value);

static inline bool isObjType(Value value, ObjType objType) {
    return IS_OBJ(value) && OBJ_TYPE(value) == objType;
}