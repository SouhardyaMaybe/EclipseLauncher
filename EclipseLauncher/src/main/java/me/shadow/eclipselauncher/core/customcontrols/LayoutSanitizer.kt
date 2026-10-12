package me.shadow.eclipselauncher.core.customcontrols

class LayoutSanitizer {

    companion object {
        // Maybe add more conditions here later?
        private fun isInvalidFormula(formula: String): Boolean {
            return formula.contains("Infinity")
        }

        private fun isSaneData(controlData: ControlData): Boolean {
            if (controlData.getWidth() == 0f || controlData.getHeight() == 0f) return false
            if (isInvalidFormula(controlData.dynamicX) || isInvalidFormula(controlData.dynamicY)) return false
            return true
        }

        private fun getControlData(dataEntry: Any?): ControlData {
            if (dataEntry is ControlData) {
                return dataEntry
            } else if (dataEntry is ControlDrawerData) {
                return dataEntry.properties
            } else throw RuntimeException("Encountered wrong type during ControlData sanitization")
        }

        private fun sanitizeList(controlDataList: MutableList<*>): Boolean {
            var madeChanges = false
            val iterator: MutableIterator<*> = controlDataList.iterator()
            while (iterator.hasNext()) {
                val controlData = getControlData(iterator.next())
                if (!isSaneData(controlData)) {
                    madeChanges = true
                    iterator.remove()
                }
            }
            return madeChanges
        }

        /**
         * Check all buttons in a control layout and ensure they're sane (contain values valid enough
         * to be displayed properly). Removes any buttons deemed not sane.
         * @param controls the original control layout.
         * @return whether the sanitization process made any changes to the layout
         */
        @JvmStatic
        fun sanitizeLayout(controls: CustomControls): Boolean {
            var madeChanges = sanitizeList(controls.mControlDataList)
            if (sanitizeList(controls.mDrawerDataList)) madeChanges = true
            if (sanitizeList(controls.mJoystickDataList)) madeChanges = true
            return madeChanges
        }
    }
}
