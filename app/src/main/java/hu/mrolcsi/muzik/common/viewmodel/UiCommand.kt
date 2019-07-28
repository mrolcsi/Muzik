package hu.mrolcsi.muzik.common.viewmodel

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject

typealias UiCommand = FragmentActivity.() -> Unit

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
}