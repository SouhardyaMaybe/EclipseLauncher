package me.shadow.eclipselauncher.core.utils

import java.util.ArrayList

/**
 * Provide a "mostly immutable" view to a "mother" list, by reference.
 * The difference from List.sublist() is:
 *  - the ability to apply a FILTER on listGeneration
 *  - "immutability", you can't add elements to the list from here, but it is backed by the real list.
 */
class FilteredSubList<E>(motherList: Array<E>, filter: BasicPredicate<E>) : java.util.AbstractList<E>() {

    private val mArrayList = ArrayList<E>()

    init {
        refresh(motherList, filter)
    }

    fun refresh(motherArray: Array<E>, filter: BasicPredicate<E>) {
        if (!mArrayList.isEmpty()) mArrayList.clear()

        for (item in motherArray) {
            if (filter.test(item)) {
                mArrayList.add(item)
            }
        }
        // Should we trim ?
        mArrayList.trimToSize()
    }

    override val size: Int
        get() = mArrayList.size

    override fun iterator(): MutableIterator<E> = mArrayList.iterator()

    override fun remove(item: E?): Boolean = mArrayList.remove(item as Any?)

    override fun removeAll(c: Collection<E>): Boolean = mArrayList.removeAll(c)

    override fun retainAll(c: Collection<E>): Boolean = mArrayList.retainAll(c)

    override fun clear() = mArrayList.clear()

    override fun get(index: Int): E = mArrayList[index]

    override fun removeAt(index: Int): E = mArrayList.removeAt(index)

    override fun listIterator(): MutableListIterator<E> = mArrayList.listIterator()

    override fun listIterator(index: Int): MutableListIterator<E> = mArrayList.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = mArrayList.subList(fromIndex, toIndex)


    // Predicate is API 24+, so micro backport
    fun interface BasicPredicate<E> {
        fun test(item: E): Boolean
    }
}
