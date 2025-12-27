package me.spica.spicaweather3.utils

import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.baidu.location.LocationClient
import me.spica.spicaweather3.App


fun doOnMainThreadIdle(
  action: () -> Unit,
  timeout: Long? = null,
) {
  // 使用 kotlin 契约来告知编译器 action 函数体中的代码是否会且只会执行一次
  // 调用方在调用该方法后，必须传入一个 action 函数体，并在内部执行。
  val handler = Handler(Looper.getMainLooper())

  val idleHandler = MessageQueue.IdleHandler {
    handler.removeCallbacksAndMessages(null)
    try {
      action()
    } catch (_: Exception) {
    }

    return@IdleHandler false
  }

  fun setupIdleHandler(queue: MessageQueue) {
    if (timeout != null) {
      handler.postDelayed({
        queue.removeIdleHandler(idleHandler)
        try {
          action()
        } catch (_: Exception) {
        }
      }, timeout)
    }
    queue.addIdleHandler(idleHandler)
  }
  if (Looper.getMainLooper() == Looper.myLooper()) {
    setupIdleHandler(Looper.myQueue())
  } else {
    setupIdleHandler(Looper.getMainLooper().queue)
  }
}

// Source - https://stackoverflow.com/questions/66703448/how-to-disable-ripple-effect-when-clicking-in-jetpack-compose
// Posted by WhoisAbel
// Retrieved 2025-11-06, License - CC BY-SA 4.0

fun Modifier.noRippleClickable(
  onClick: () -> Unit
): Modifier = composed {
  clickable(
    indication = null,
    interactionSource = remember { MutableInteractionSource() }) {
    onClick()
  }
}


