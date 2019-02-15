package dijkstra

import org.junit.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.test.assertEquals

class ConcurrentStressTest {

    @Test
    fun `test on trees`() {
        testOnRandomGraphs(100, 99)
    }

    @Test
    fun `test on big trees`() {
        testOnRandomGraphs(10000, 9999)
    }


    @Test
    fun `test on very small graphs`() {
        testOnRandomGraphs(16, 25)
    }

    @Test
    fun `test on small graphs`() {
        testOnRandomGraphs(100, 1000)
    }

    @Test
    fun `test on big graphs`() {
        testOnRandomGraphs(10000, 100000)
    }

    @Test
    fun `test on big graphs with a lot of edges`() {
        testOnRandomGraphs(10000, 1000000, 20)
    }

    @Test
    fun `test on very big graphs`() {
        testOnRandomGraphs(100000, 1000000, 5)
    }

    @Test
    fun `test on complete graphs`() {
        testOnCompleteGraphs(1000, 100)
    }

    private fun testOnCompleteGraphs(nodes: Int, searches: Int = 100) {
        testOnRandomGraphs(nodes, (nodes*(nodes-1))/2, searches, true)
    }

    private fun testOnRandomGraphs(nodes: Int, edges: Int, searches: Int = 100, isComplete: Boolean = false) {
        val r = Random()
        var sequentialTime = 0L
        var parallelTime = 0L
        repeat(GRAPHS) {

            val nodesList = if (isComplete) {
                completeGraph(nodes)
            } else {
                randomConnectedGraph(nodes, edges)
            }
            repeat(searches) {
                val from = nodesList[r.nextInt(nodes)]
                val to = nodesList[r.nextInt(nodes)]
                var begin = LocalDateTime.now()
                val seqRes = shortestPathSequential(from, to)
                sequentialTime += begin.until(LocalDateTime.now(), ChronoUnit.MILLIS)
                clearNodes(nodesList)
                begin = LocalDateTime.now()
                val parRes = shortestPathParallel(from, to)
                parallelTime += begin.until(LocalDateTime.now(), ChronoUnit.MILLIS)
                clearNodes(nodesList)
                assertEquals(seqRes, parRes)
            }
        }
        println("nodes: $nodes, edges: $edges")
        println("sequential - $sequentialTime")
        println("parallel - $parallelTime")
        println("==============================")
    }

}

private const val GRAPHS = 10
