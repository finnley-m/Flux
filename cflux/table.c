#include "table.h"

#include "memory.h"
#include "object.h"
#include "value.h"

#define TABLE_MAX_LOAD 0.75 // tune later maybe :p

void initTable(Table* table) {
    table->count = 0;
    table->capacity = 0;
    table->entries = NULL;
}

void freeTable(Table* table) {
    FREE_ARRAY(Entry, table->entries, table->capacity);
    initTable(table);
}

// returns the position of the entry.
// returns empty space with key NULL if not found
static Entry* findEntry(Entry* entries, int capacity, ObjString* key) {
    uint32_t index   = key->hash % capacity;
    Entry* tombstone = NULL;

    for(;;) {
        Entry* entry = &entries[index];

        if(entry->key == NULL) {
            if(IS_NULL(entry->value)) {
                // empty entry -> return the tombstone location if found as it gets rid of the tombstone
                //                means that array capacity is wasted less of tombstones as we automatically
                //                use them. else just return the empty cell found.
                return tombstone != NULL ? tombstone : entry;
            }
            else {
                // tombstone found
                if(tombstone == NULL) tombstone = entry;
            } 
        } else if(entry->key == key) {
            // correct key found
            return entry;
        }

        index = (index + 1) % capacity;
    }
}

// return wether we found the value associated with the key or not
bool tableGet(Table* table, ObjString* key, Value* value) {
    if(table->count == 0) return false;

    Entry* entry = findEntry(table->entries, table->capacity, key);
    if(entry->key == NULL) return false;
    
    *value = entry->value;
    return true;
}

static void adjustCapacity(Table* table, int capacity) { // this capacity is target capacity
    Entry* entries = ALLOCATE(Entry, capacity);
    for(int i = 0; i < capacity; i++) {
        // initialize new table
        entries[i].key   = NULL;
        entries[i].value = NULL_VAL;
    }
    // recount as we discard tombstone entries when resizing 
    table->count = 0;
    for(int i = 0; i < table->capacity; i++) {
        Entry* entry = &table->entries[i];
        if(entry->key == NULL) continue;

        Entry* dest = findEntry(entries, capacity, entry->key);
        dest->key   = entry->key;
        dest->value = entry->value;
        table->count++;
    }

    FREE_ARRAY(Entry, table->entries, table->capacity);
    table->entries  = entries;
    table->capacity = capacity;
}

// adds a given key value pair to a table
// if that key already exists, we overwrite and return false
// if the key is new and we return true as new entry added
// used to check if variable declaration was done before assigning
bool tableSet(Table* table, ObjString* key, Value value) {
    // allocate space for the array and resive if hits max load
    if(table->count+1 > table->capacity * TABLE_MAX_LOAD) {
        int capacity = GROW_CAPACITY(table->capacity);
        adjustCapacity(table, capacity);
    }

    // find entry that corresponds with a certain key
    Entry* entry = findEntry(table->entries, table->capacity, key);
    
    // if the entry does not have a key in it, we are creating a new key for the table
    bool isNewKey = entry->key == NULL;
    if (isNewKey && IS_NULL(entry->value)) table->count++; 
    // * null check is so we dont increment when replacing tombstone

    entry->key = key;
    entry->value = value;
    return isNewKey;
}

// return wether we successfully deleted
bool tableDelete(Table* table, ObjString* key) {
    if(table->count == 0) return false;

    // find entry
    Entry* entry = findEntry(table->entries, table->capacity, key);
    if(entry->key == NULL) return false;

    // we define a tombstone as having a null key and true value (arbritary value)
    // place tombstone at entry
    entry->key   = NULL;
    entry->value = BOOL_VAL(true);
    return true;
}

void tableAddAll(Table* from, Table* to) {
    for(int i = 0; i < from->capacity; i++) {
        Entry* entry = &from->entries[i];
        if(entry->key != NULL) {
            tableSet(to, entry->key, entry->value);
        }
    }
}

// look up given string and return reference
ObjString* tableFindString(Table* table, const char* chars, int length, uint32_t hash) {
    if(table->count == 0) return NULL;

    uint32_t index = hash % table->capacity;

    for(;;) {
        Entry* entry = &table->entries[index];

        if(entry->key == NULL) {
            // stop if we find non tombstone entry
            if(IS_NULL(entry->value)) return NULL;
        } else if (entry->key->length == length && 
                   entry->key->hash == hash && 
                   memcmp(entry->key->chars, chars, length) == 0) {
            return entry->key;
        }

        index = (index + 1) % table->capacity;
    }
}