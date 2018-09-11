import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class FileIndexer<T>(private val rootDir: File,
                     private val collisionStrategy: CollisionStrategy = CollisionStrategy.NO_DUPLICATES,
                     threads: Int = Runtime.getRuntime().availableProcessors()) {
    private val index = ConcurrentHashMap<T, MutableList<File>>()
    private val poolContext = newFixedThreadPoolContext(threads, "pool")
    private val handleCollision = when (collisionStrategy) {
        CollisionStrategy.REPLACE -> { file: File, indexedFiles: MutableList<File> ->
            indexedFiles.replaceAll { file }
        }
        CollisionStrategy.ALLOW_DUPLICATES -> { file: File, indexedFiles: MutableList<File> ->
            indexedFiles.add(file)
        }
        else -> { file: File, _: MutableList<File> ->
            throw IllegalStateException("""
                File $file produced an already existing key.
                Collision strategy $collisionStrategy does not allow duplicated keys.
            """)
        }
    }

    fun index(shouldIndex: (File) -> Boolean = { true }, extractKey: (File) -> T) = runBlocking {
        val jobs = mutableListOf<Deferred<Unit>>()
        val counter = AtomicLong(0)

        for (file in rootDir.walkTopDown()) {
            jobs += async(poolContext) {
                if (shouldIndex(file)) {
                    val key = extractKey(file)
                    add(key, file)
                    counter.getAndIncrement()
                }
            }
        }

        jobs.forEach { it.await() }
        println("Indexed ${counter.get()} files")
    }

    fun add(key: T, file: File) {
        val indexedFiles = index.getOrPut(key) { mutableListOf() }

        if (indexedFiles.isNotEmpty()) {
            handleCollision(file, indexedFiles)
        } else {
            indexedFiles.add(file)
        }
    }

    fun get(key: T): File? = getAll(key).let {
        return if (it.isNotEmpty()) {
            it.first()
        } else {
            null
        }
    }

    fun getAll(key: T): List<File> = index[key]?.toList() ?: emptyList()
}

enum class CollisionStrategy {
    NO_DUPLICATES,
    REPLACE,
    ALLOW_DUPLICATES
}
