package hu.mrolcsi.muzik.ui.common

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController

typealias NavCommand = NavController.() -> Unit

fun once(command: NavCommand): NavCommand {
  var active = true
  return {
    if (active) {
      active = false
      command(this)
    }
  }
}

interface NavCommandSource {
  val navCommand: LiveData<NavCommand>

  fun sendNavCommand(command: NavCommand)
}

class ExecuteOnceNavCommandSource : NavCommandSource {
  override val navCommand = object : MutableLiveData<NavCommand>() {
    override fun postValue(value: NavCommand) {
      super.postValue(once(value))
    }

    override fun setValue(value: NavCommand) {
      super.setValue(once(value))
    }
  }

  override fun sendNavCommand(command: NavCommand) {
    navCommand.value = command
  }
}

fun NavController.observeAndRunNavCommands(lifecycleOwner: LifecycleOwner, source: NavCommandSource) =
  source.navCommand.observe(lifecycleOwner, Observer { it?.invoke(this) })