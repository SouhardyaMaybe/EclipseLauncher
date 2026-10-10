package me.shadow.eclipselauncher.core.utils

object MathUtils {

    //Ported from https://www.arduino.cc/reference/en/language/functions/math/map/
    @JvmStatic
    fun map(x: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    /** Returns the distance between two points. */
    @JvmStatic
    fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = (x2 - x1)
        val y = (y2 - y1)
        return Math.hypot(x.toDouble(), y.toDouble()).toFloat()
    }

    /**
     * Find the object T with the closest (or higher) value compared to targetValue
     * @param targetValue the target value
     * @param objects the list of objects that the search will be performed on
     * @param valueProvider the provider for each values
     * @return the RankedValue that wraps the object which has the closest value to targetValue, or null if values of all
     *         objects are less than targetValue
     */
    @JvmStatic
    fun <T> findNearestPositive(targetValue: Int, objects: List<T>, valueProvider: ValueProvider<T>): RankedValue<T>? {
        var delta = Int.MAX_VALUE
        var selectedObject: T? = null
        for (obj in objects) {
            val objectValue = valueProvider.getValue(obj)
            if (objectValue < targetValue) continue

            val currentDelta = objectValue - targetValue
            if (currentDelta == 0) return RankedValue(obj, 0)
            if (currentDelta >= delta) continue

            selectedObject = obj
            delta = currentDelta
        }
        return selectedObject?.let { RankedValue(it, delta) }
    }

    fun interface ValueProvider<T> {
        fun getValue(`object`: T): Int
    }

    class RankedValue<T>(@JvmField val value: T, @JvmField val rank: Int)

    /**
     * Out of two objects, select one with the lowest value.
     * @param object1 Object 1 for comparsion
     * @param object2 Object 2 for comparsion
     * @param valueProvider Value provider for the objects
     * @return If value of object 1 is lower than or equal to object 2, returns object 1
     *         Otherwise, returns object 2
     */
    @JvmStatic
    fun <T> objectMin(object1: T?, object2: T?, valueProvider: ValueProvider<T>): T? {
        if (object1 == null) return object2
        if (object2 == null) return object1
        val value1 = valueProvider.getValue(object1)
        val value2 = valueProvider.getValue(object2)
        return if (value1 <= value2) {
            object1
        } else {
            object2
        }
    }
}
