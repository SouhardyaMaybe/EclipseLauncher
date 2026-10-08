package me.shadow.eclipselauncher.task

/**
 * Listener for the various phases of task execution
 */
interface TaskExecutionPhaseListener {
    fun onBeforeStart() {}
    fun execute() {}
    fun onEnded() {}
    fun onFinally() {}
    /**
     * What to run after an exception is thrown during task execution
     * @param throwable the thrown exception
     */
    fun onThrowable(throwable: Throwable) {}
}