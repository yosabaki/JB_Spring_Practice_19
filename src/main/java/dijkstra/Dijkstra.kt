package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.concurrent.thread

val NODE_DISTANCE_COMPARATOR = Comparator<Pair<Node, Int>> { o1, o2 -> Integer.compare(o1!!.second, o2!!.second) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathSequential(start: Node, destination: Node): Int {
    start.distance.set(0)
    val q = PriorityQueue<Pair<Node, Int>>(NODE_DISTANCE_COMPARATOR)
    q.add(Pair(start, 0))
    while (q.isNotEmpty()) {
        val cur = q.poll()
        if (cur.first.distance.get() != cur.second) continue
        for (e in cur.first.outgoingEdges) {
            if (!e.to.marked) {
                if (e.to.distance.get() > cur.first.distance.get() + e.weight) {
                    e.to.distance.set(cur.first.distance.get() + e.weight)
                    q.add(Pair(e.to, e.to.distance.get()))
                }
            }
        }
        cur.first.marked = true
    }
    return destination.distance.get()
}

// Returns `Integer.MAX_VALUE` if a path has not been found.
// Works faster on big graphs with a lot of edges
// On small graphs and trees works a lot slower
fun shortestPathParallel(start: Node, destination: Node): Int {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance.set(0)
    val q = PriorityQueue<Pair<Node, Int>>(workers, NODE_DISTANCE_COMPARATOR)
    q.add(Pair(start, 0))
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    val workersCounter = AtomicInteger(0)
    val lock = ReentrantLock()
    repeat(workers) {
        thread {
            var isWorking = false
            while (true) {
                val curr: Node? = synchronized(q) {
                    // skip nodes with outdated distance
                    while (q.isNotEmpty() && q.peek().first.distance.get() != q.peek().second) {
                        q.poll()
                    }
                    q.poll()?.first
                }
                lock.lock() // prevent race condition of countWorkers variable
                try {
                    if (curr == null) {
                        synchronized(workersCounter) {
                            if (isWorking) {
                                workersCounter.decrementAndGet()
                                isWorking = false
                            }
                        }
                    } else {
                        if (!isWorking) {
                            workersCounter.incrementAndGet()
                            isWorking = true
                        }
                    }
                } finally {
                    lock.unlock()
                }
                if (curr == null) {
                    // terminate process when all workers finished
                    if (workersCounter.get() == 0) break else continue
                }
                for (e in curr.outgoingEdges) {
                    while (true) {
                        val currValue = curr.distance.get()
                        val toValue = e.to.distance.get()
                        if (toValue > currValue + e.weight) {
                            if (!e.to.distance.compareAndSet(toValue, currValue + e.weight)) {
                                continue // repeat if any worker interrupted
                            }
                            synchronized(q) {
                                q.add(Pair(e.to, currValue + e.weight))
                            }
                        }
                        break
                    }
                }
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
    return destination.distance.get()
}
