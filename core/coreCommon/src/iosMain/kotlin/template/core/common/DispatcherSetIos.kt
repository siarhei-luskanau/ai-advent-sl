package template.core.common

import kotlinx.coroutines.Dispatchers

internal class DispatcherSetIos : DispatcherSet {
    override fun defaultDispatcher() = Dispatchers.Default

    override fun ioDispatcher() = Dispatchers.Default

    override fun mainDispatcher() = Dispatchers.Main
}
