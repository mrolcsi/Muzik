package hu.mrolcsi.muzik.common.viewmodel

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import javax.inject.Inject

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

class ExecuteOnceNavCommandSource @Inject constructor() : NavCommandSource {
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

fun Fragment.observeAndRunNavCommands(source: NavCommandSource) =
  source.navCommand.observe(this.viewLifecycleOwner, Observer { it?.invoke(this.findNavController()) })