package dijkstra

import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList

class Node {
    private val _outgoingEdges = arrayListOf<Edge>()
    val outgoingEdges: List<Edge> = _outgoingEdges

    var marked = false;

    val distance = AtomicInteger(Integer.MAX_VALUE) // USE ME FOR THE DIJKSTRA ALGORITHM!

    fun addEdge(edge: Edge) {
        _outgoingEdges.add(edge)
    }
}

data class Edge(val to: Node, val weight: Int)

fun randomConnectedGraph(nodes: Int, edges: Int, maxWeight: Int = 100): List<Node> {
    require(edges >= nodes - 1)
    val r = Random()
    val nodesList = List(nodes) { Node() }
    // generate a random connected graph with `nodes-1` edges
    val s = ArrayList(nodesList)
    var cur = s.removeAt(r.nextInt(s.size))
    val visited = mutableSetOf<Node>(cur)
    while (s.isNotEmpty()) {
        val neighbor = s.removeAt(r.nextInt(s.size))
        if (visited.add(neighbor)) {
            val weight = r.nextInt(maxWeight)
            cur.addEdge(Edge(neighbor, weight))
            neighbor.addEdge(Edge(cur, weight))
        }
        cur = neighbor
    }
    // add `edges - nodes + 1` random edges
    repeat(edges - nodes + 1) {
        while (true) {
            val first = nodesList[r.nextInt(nodes)]
            val second = nodesList[r.nextInt(nodes)]
            if (first == second) continue
            if (first.outgoingEdges.any { e -> e.to == second }) continue
            val weight = r.nextInt(maxWeight)
            first.addEdge(Edge(second, weight))
            second.addEdge(Edge(first, weight))
            break
        }
    }
    return nodesList
}

fun completeGraph(nodes: Int, maxWeight: Int = 100): List<Node> {
    val r = Random()
    val nodesList = List(nodes) { Node() }
    // generate a random connected graph with `nodes-1` edges
    val s = ArrayList(nodesList)
    var cur = s.removeAt(s.size - 1)
    while (s.isNotEmpty()) {
        for (v in s) {
            val weight = r.nextInt(maxWeight)
            cur.addEdge(Edge(v, weight))
            v.addEdge(Edge(cur, weight))
        }
        cur = s.removeAt(s.size - 1)
    }
    return nodesList
}

fun clearNodes(nodes: List<Node>) {
    nodes.forEach {
        it.distance.set(Int.MAX_VALUE)
        it.marked = false
    }
}
