package dijkstra

import org.junit.Test
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

class ConcurrentStressTest {

    @Test
    fun `test on trees`() {
        testOnRandomGraphs(100, 99)
    }

    @Test
    fun `test on big trees`() {
        testOnRandomGraphs(10_000, 9999)
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
        testOnRandomGraphs(10_000, 100_000)
    }

    @Test
    fun `test on big graphs with a lot of edges`() {
        testOnRandomGraphs(10_000, 1_000_000, 50)
    }

    @Test
    fun `test on very big graphs`() {
        testOnRandomGraphs(100_000, 1_000_000, 10)
    }

    @Test
    fun `test on complete graphs`() {
        testOnCompleteGraphs(1000, 100)
    }

    private fun testOnCompleteGraphs(nodes: Int, searches: Int = 100) {
        testOnRandomGraphs(nodes, (nodes * (nodes - 1)) / 2, searches, true)
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
                var seqRes = 0
                sequentialTime += measureTimeMillis {
                    seqRes = shortestPathSequential(from, to)
                }
                clearNodes(nodesList)
                var parRes = 1
                parallelTime += measureTimeMillis {
                    parRes = shortestPathParallel(from, to)
                }
                clearNodes(nodesList)
                assertEquals(seqRes, parRes)
            }
        }
        println("""
            nodes: $nodes, edges: $edges
            sequential - $sequentialTime
            parallel - $parallelTime
            parallel to sequential - ${"%.3f".format(sequentialTime.toDouble() / parallelTime)}
            ==============================""".trimIndent())
    }

}

private const val GRAPHS = 10
