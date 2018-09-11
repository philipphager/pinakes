# Pinakes
Fast Kotlin-based file index to enable quick random access in the filesystem. The index is currently held in RAM, but future versions will also allow disk persistence.

## Usage
Pinakes allows you to decide on what file attribute to index the files. Here is an example to enable quick access to files by their name:
```kotlin
// Build index
val fileIndexer = FileIndexer<String>(File("/path/to/directory"))
fileIndexer.index(extractKey = { it.name })

// Use index to get a file by name
val file = fileIndexer.get("name")
```

If your keys are not unique to all files in the filesystem, collisions can happen. Pinakes allows you to define how to handle key collisions.
The following example shows how to allow indexing multiple files with the same key:
```kotlin
// Build index
val fileIndexer = FileIndexer<String>(File("/path/to/directory", CollisionStrategy.ALLOW_DUPLICATES)
fileIndexer.index(extractKey = { it.name })

// Retrieve all files with the key
val file = fileIndexer.getAll("name")

// In the case of multiple files per key, get will return the first indexed file
val file = fileIndexer.get("name")
```

Indexing will use a thread pool by default, sized to the number of available processors. To manually set the pool size:
```kotlin
// Set pool size
val fileIndexer = FileIndexer<String>(File("/path/to/directory"), threads = 1)
```



 