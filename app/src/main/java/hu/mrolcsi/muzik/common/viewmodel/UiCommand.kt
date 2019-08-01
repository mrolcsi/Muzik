package hu.mrolcsi.muzik.common.viewmodel

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import javax.inject.Inject

typealias UiCommand = Context.() -> Unit

fun once(command: UiCommand): UiCommand {
  var active = true
  return {
    if (active) {
      active = false
      command(this)
    }
  }
}

interface UiCommandSource {
  val uiCommand: LiveData<UiCommand>

  fun sendUiCommand(command: UiCommand)
}

class ExecuteOnceUiCommandSource @Inject constructor() : UiCommandSource {
  override val uiCommand = object : MutableLiveData<UiCommand>() {
    override fun postValue(value: UiCommand) {
      super.postValue(once(value))
    }

    override fun setValue(value: UiCommand) {
      super.setValue(once(value))
    }
  }

  override fun sendUiCommand(command: UiCommand) {
    uiCommand.value = command
  }
}

fun Fragment.observeAndRunUiCommands(commands: LiveData<UiCommand>) =
  commands.observe(viewLifecycleOwner, Observer { it?.invoke(requireContext()) })

fun FragmentActivity.observeAndRunUiCommands(commands: LiveData<UiCommand>) =
  commands.observe(this, Observer { it?.invoke(this) })