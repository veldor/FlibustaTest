package net.veldor.flibusta_test.model.delegate

interface SomeActionDelegate {
    fun actionDone()
    fun actionDone(item: Any, target: Any)
}