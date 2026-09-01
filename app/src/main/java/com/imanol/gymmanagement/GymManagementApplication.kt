package com.imanol.gymmanagement

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp(Application::class)
class GymManagementApplication : Hilt_GymManagementApplication()
