/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package hu.mrolcsi.muzik.extensions

import android.app.Activity
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.ColorUtils

fun AppCompatActivity.isPermissionGranted(permission: String) =
  ActivityCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED

fun AppCompatActivity.shouldShowPermissionRationale(permission: String) =
  ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

fun AppCompatActivity.requestPermission(permission: String, requestId: Int) =
  ActivityCompat.requestPermissions(this, arrayOf(permission), requestId)

fun Activity.applyColorToStatusBarIcons(backgroundColor: Int) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    window?.decorView?.apply {
      val flags = systemUiVisibility
      systemUiVisibility =
        if (ColorUtils.calculateLuminance(backgroundColor) < 0.5) {
          // Clear flag (white icon)
          flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
          // Set flag (gray icons)
          flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
  }
}

fun Activity.applyColorToNavigationBarIcons(backgroundColor: Int) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    window?.decorView?.apply {
      val flags = systemUiVisibility
      systemUiVisibility =
        if (ColorUtils.calculateLuminance(backgroundColor) < 0.5) {
          // Clear flag (white icons)
          flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        } else {
          // Set flag (gray icons)
          flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
  }
}

var Activity.mediaControllerCompat: MediaControllerCompat?
  get() = MediaControllerCompat.getMediaController(this)
  set(controller) = MediaControllerCompat.setMediaController(this, controller)
