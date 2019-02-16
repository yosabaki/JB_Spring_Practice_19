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
    val q = PriorityQueue(NODE_DISTANCE_COMPARATOR)
    q.add(Pair(start, 0))
    while (q.isNotEmpty()) {
        val (cur, distance) = q.poll()
        if (cur.distance.get() != distance) continue
        for (e in cur.outgoingEdges) {
            val to = e.to
            if (!to.marked) {
                if (to.distance.get() > cur.distance.get() + e.weight) {
                    to.distance.set(cur.distance.get() + e.weight)
                    q.add(Pair(to, to.distance.get()))
                }
            }
        }
        cur.marked = true
    }
    return destination.distance.get()
}

// Returns `Integer.MAX_VALUE` if a path has not been found.
// Works faster on big graphs with a lot of edges
// On small graphs and trees works a lot slower
fun shortestPathParallel(start: Node, destination: Node): Int {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance.set(0)
    val q = PriorityQueue(workers, NODE_DISTANCE_COMPARATOR)
    q.add(Pair(start, 0))
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    val workersCounter = AtomicInteger(0)
    val lock = ReentrantLock()
    var finished = false
    val notEmpty = lock.newCondition()
    repeat(workers) {
        thread {
            var isWorking = false
            while (true) {
                val curr: Node?
                // prevent race condition of countWorkers
                synchronized(workersCounter) {
                    curr = synchronized(q) {
                        // skip nodes with outdated distance
                        while (q.isNotEmpty() && q.peek().first.distance.get() != q.peek().second) {
                            q.poll()
                        }
                        q.poll()?.first
                    }
                    // change state of the current worker and update counter
                    if (curr == null) {
                        if (isWorking) {
                            workersCounter.decrementAndGet()
                            isWorking = false
                        }
                    } else {
                        if (!isWorking) {
                            workersCounter.incrementAndGet()
                            isWorking = true
                        }
                    }
                }
                if (curr == null) {
                    // terminate process when all workers finished
                    if (workersCounter.get() == 0) {
                        if (!finished) {
                            // wake up other threads waiting for elements
                            finished = true
                            lock.lock()
                            try {
                                notEmpty.signalAll()
                            } finally {
                                lock.unlock()
                            }
                        }
                        break
                    } else {
                        // condition variable is used to not to lock queue without need
                        // it creates a small boost for graphs with few edges per node and trees
                        // (work speed increased at about 15-20% for random trees with 10_000 nodes
                        // compared to solution without condition variable)
                        lock.lock()
                        try {
                            while (q.isEmpty() && workersCounter.get() > 0) {
                                notEmpty.await()
                            }
                        } finally {
                            lock.unlock()
                        }
                        continue
                    }
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
                            // notify thread waiting for element in queue
                            lock.lock()
                            try {
                                notEmpty.signal()
                            } finally {
                                lock.unlock()
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
