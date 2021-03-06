package net.fwbrasil.activate.util

import scala.collection.mutable.{ Set => MutableSet }
import scala.collection.mutable.{ Map => MutableMap }
import scala.collection.mutable.ListBuffer
import net.fwbrasil.activate.util.RichList._

object GraphUtil {

    class CyclicReferenceException extends IllegalStateException("Cyclic reference.")

    class DependencyTree[T: Manifest](values: Set[T]) {

        private[this] val nodeMap = MutableMap[T, Node[T]]()
        private[this] val leaves = MutableSet[Node[T]]()

        for (value <- values)
            nodeFor(value)

        def addDependency(a: T, b: T) = {
            val nodeA = nodeFor(a)
            val nodeB = nodeFor(b)
            leaves += nodeB
            nodeA.addEdge(nodeB)
        }

        def roots =
            nodeMap.values.toSet -- leaves

        private[this] def nodeFor(value: T) =
            nodeMap.getOrElseUpdate(value, Node(value))
        private[this] def throwCyclicReferenceException =
            throw new CyclicReferenceException

        def resolve = {
            val r = roots
            if (r.isEmpty && nodeMap.nonEmpty)
                throwCyclicReferenceException

            val resolved = ListBuffer[Node[T]]()
            val ordered = r.map(_.value).sortIfComparable.reverse
            for (nodeValue <- ordered)
                fDepResolve(nodeFor(nodeValue), resolved, MutableSet[Node[T]]())
            val result =
                for (node <- resolved)
                    yield node.value
            if (result.toSet != values)
                throwCyclicReferenceException
            result.toList.reverse
        }

        def fDepResolve(node: Node[T], resolved: ListBuffer[Node[T]], unresolved: MutableSet[Node[T]]): Unit = {
            unresolved += node
            for (edge <- node.edges)
                if (!resolved.contains(edge))
                    if (unresolved.contains(edge))
                        throwCyclicReferenceException
                    else
                        fDepResolve(edge, resolved, unresolved)
            resolved += node
            unresolved -= node
        }

    }

    private[GraphUtil] case class Node[T](val value: T) {
        val edgesSet = MutableSet[Node[T]]()
        def addEdge(other: Node[T]) =
            edgesSet += other
        def edges = edgesSet.toSet
    }

}