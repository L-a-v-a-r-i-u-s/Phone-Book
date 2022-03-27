package phonebook

import java.io.File
import java.util.regex.Pattern
import kotlin.math.sqrt

val spacePattern: Pattern = Pattern.compile(" ")

const val directoryPath = "/Users/lavarius/IdeaProjects/Phone Book/Phone Book/task/src/phonebook/directory.txt"
const val queriesPath = "/Users/lavarius/IdeaProjects/Phone Book/Phone Book/task/src/phonebook/find.txt"

data class Entry(val phone: String, val name: String)
data class SearchInfo(val found: Int, val sorting: Long? = null, val searching: Long? = null,
                      val hashing: Long? = null, val aborted: Boolean = false)

val unsortedArrayDirectory: Array<Entry> = loadDictionary()
val unsortedArrayQueries = loadQueries()

fun main() {

    val linearSearchElapsed = timing("linear search", unsortedArrayQueries.size) {
        val foundEntries = doSearch(unsortedArrayDirectory, unsortedArrayQueries, ::linearSearchLibrary)
        SearchInfo(foundEntries)
    }

    timing("bubble sort + jump search", unsortedArrayQueries.size) {
        val (sortedDirectory, sortTime) = doAndGetTiming {
            bubbleSortWithBreak(unsortedArrayDirectory, linearSearchElapsed)
        }

        val (foundEntries, searchTime) = doAndGetTiming {
            if (null != sortedDirectory) {
                doSearch(sortedDirectory, unsortedArrayQueries, ::jumpSearch)
            } else {
                doSearch(unsortedArrayDirectory, unsortedArrayQueries, ::linearSearchLibrary)
            }
        }
        SearchInfo(foundEntries, sortTime, searchTime, aborted = null == sortedDirectory)
    }

    timing("quick sort + binary search", unsortedArrayQueries.size) {
        val (sortedDirectory, sortTime) = doAndGetTiming {
            quicksort(unsortedArrayDirectory)
        }

        val (foundEntries, searchTime) = doAndGetTiming {
            doSearch(sortedDirectory, unsortedArrayQueries, ::binarySearch)
        }
        SearchInfo(foundEntries, sortTime, searchTime)
    }

    timing("hash table", unsortedArrayQueries.size) {
        val (hashMapDirectory, createHashMapTime) = doAndGetTiming {
            createHashMap(unsortedArrayDirectory)
        }

        val (foundEntries, searchTime) = doAndGetTiming {
            doSearch(hashMapDirectory, unsortedArrayQueries, ::lookupHashMap)
        }
        SearchInfo(foundEntries, hashing = createHashMapTime, searching = searchTime)
    }
}

private fun loadDictionary(): Array<Entry> {
    val file = File(directoryPath)

    return file.readLines().map { line ->
        val split = line.split(spacePattern, 2)
        Entry(split[0], split[1])
    }.toTypedArray()
}

private fun loadQueries(): Array<String> {
    val file = File(queriesPath)
    return file.readLines().toTypedArray()
}

private fun doSearch(directory: Array<Entry>, queries: Array<String>,
                     searchMethod: (haystack: Array<Entry>, query: String) -> Entry?): Int {
    var foundEntries = 0
    for (query in queries) {
        val maybeEntry = searchMethod(directory, query)
        if (null != maybeEntry) foundEntries++
    }
    return foundEntries
}

private fun doSearch(directory: Map<String, Entry>, queries: Array<String>,
                     searchMethod: (map: Map<String, Entry>, query: String) -> Entry?): Int {
    var foundEntries = 0
    for (query in queries) {
        val maybeEntry = searchMethod(directory, query)
        if (null != maybeEntry) foundEntries++
    }
    return foundEntries
}

private fun linearSearchLibrary(haystack: Array<Entry>, query: String): Entry? {
    return haystack.find { entry -> entry.name == query }
}

private fun jumpSearch(haystack: Array<Entry>, query: String): Entry? {
    val jumpSize = sqrt(haystack.size.toDouble()).toInt()

    var jumpIndex = 0
    var previousJumpIndex = 0

    do {
        val considered = haystack[jumpIndex]
        when {
            query == considered.name -> return considered
            query > considered.name -> {
                previousJumpIndex = jumpIndex
                jumpIndex = (jumpIndex + jumpSize).coerceAtMost(haystack.size - 1)
            }
            query < considered.name -> {
                val subArray = haystack.copyOfRange(previousJumpIndex, jumpIndex)
                return linearSearchLibrary(subArray, query)
            }
        }
    } while (jumpIndex != previousJumpIndex)

    return null
}

private fun bubbleSortWithBreak(source: Array<Entry>, linearSearchElapsed: Long): Array<Entry>? {

    var unsortedElements = source.size
    val result = source.copyOf()

    var totalElapsed = 0L
    do {
        var swapped = false
        val elapsed = kotlin.system.measureTimeMillis {
            for (i in 1 until unsortedElements) {
                if (result[i - 1].name > result[i].name) {
                    swap(result, i, i - 1)
                    swapped = true
                }
            }
            unsortedElements--
        }

        totalElapsed += elapsed
        if (shouldBreakSort(totalElapsed, linearSearchElapsed)) return null
    } while (swapped)

    return result
}

private fun shouldBreakSort(elapsed: Long, linearSortElapsed: Long): Boolean {
    return elapsed > linearSortElapsed * 10
}

private fun quicksort(source: Array<Entry>): Array<Entry> {
    val result = source.copyOf()
    quicksortInPlace(result, 0, source.size - 1)
    return result
}

private fun quicksortInPlace(array: Array<Entry>, start: Int, end: Int) {
    if (start < end) {
        val pivot = quicksortPartition(array, start, end)
        quicksortInPlace(array, start, pivot - 1)
        quicksortInPlace(array, pivot + 1, end)
    }
}

private fun quicksortPartition(array: Array<Entry>, start: Int, end: Int): Int {
    val pivotValue = array[end]
    var pivotTargetIndex = start
    for (index in start until end) {
        if (array[index].name < pivotValue.name) {
            swap(array, index, pivotTargetIndex)
            pivotTargetIndex++
        }
    }
    // Put the pivot in the right place, from the end (where we have put it before the loop)
    swap(array, end, pivotTargetIndex)
    return pivotTargetIndex
}

private fun binarySearch(array: Array<Entry>, query: String): Entry {
    return binarySearch(array, 0, array.size - 1, query)
}

private fun binarySearch(array: Array<Entry>, start: Int, end: Int, query: String): Entry {
    val middle = start + (end - start) / 2
    val middleName = array[middle].name
    return when {
        query == middleName -> array[middle]
        query < middleName -> binarySearch(array, start, middle - 1, query)
        else -> binarySearch(array, middle + 1, end, query)
    }
}

private fun createHashMap(array: Array<Entry>): Map<String, Entry> {
    val map = HashMap<String, Entry>(array.size)
    array.forEach {
        map[it.name] = it
    }
    return map
}

private fun lookupHashMap(map: Map<String, Entry>, query: String): Entry? {
    return map[query]
}

private fun timing(searchType: String, numberOfSearches: Int, block: () -> SearchInfo): Long {
    println("Start searching ($searchType)...")

    val (searchInfo, elapsed) = doAndGetTiming(block)
    val elapsedString = formatElapsedTime(elapsed)

    println("Found ${searchInfo.found} / $numberOfSearches entries. Time taken: $elapsedString")
    if (null != searchInfo.hashing) {
        printHashingTime(searchInfo.hashing)
    }
    if (null != searchInfo.sorting) {
        printSortingTime(searchInfo.sorting, searchInfo.aborted)
    }
    if (null != searchInfo.searching) {
        printSearchingTime(searchInfo.searching)
    }
    println()
    return elapsed
}

private fun printSortingTime(elapsed: Long, aborted: Boolean) {
    val elapsedString = formatElapsedTime(elapsed)
    print("Sorting time: $elapsedString")
    if (aborted) {
        print(" - STOPPED, moved to linear search")
    }
    println()
}

private fun printSearchingTime(elapsed: Long) {
    val elapsedString = formatElapsedTime(elapsed)
    println("Searching time: $elapsedString")
}

private fun printHashingTime(elapsed: Long) {
    val elapsedString = formatElapsedTime(elapsed)
    println("Creating time: $elapsedString")
}

private fun <T> doAndGetTiming(block: () -> T): Pair<T, Long> {

    val start = System.currentTimeMillis()
    val returnValue: T = block()
    val elapsed = System.currentTimeMillis() - start

    return Pair(returnValue, elapsed)
}

private fun formatElapsedTime(elapsed: Long): String {
    return String.format("%1\$tM min. %1\$tS sec. %1\$tL ms.", elapsed)
}

private fun swap(array: Array<Entry>, index1: Int, index2: Int) {
    val tmp = array[index1]
    array[index1] = array[index2]
    array[index2] = tmp
}