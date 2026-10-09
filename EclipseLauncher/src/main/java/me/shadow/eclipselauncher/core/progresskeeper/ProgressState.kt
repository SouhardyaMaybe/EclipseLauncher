package me.shadow.eclipselauncher.core.progresskeeper

/**
 * Immutable snapshot of a single progress record.
 *
 * @param progress the progress percentage reported for the record
 * @param resid the string resource used to format the progress message
 * @param args the format arguments of the progress message
 * @param session monotonically increasing token; a new token is minted every time progress for
 * this record starts anew, so observers can tell "a new task started" apart from "the same task
 * updated" without keeping mutable state
 */
data class ProgressState(
    val progress: Int,
    val resid: Int,
    val args: List<Any?>,
    val session: Long
)
