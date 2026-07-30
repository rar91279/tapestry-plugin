package com.intellij.tapestry.intellij.view.actions

import com.intellij.openapi.actionSystem.ToggleAction
import icons.TapestryIcons

abstract class StartInBasePackageAction :
    ToggleAction("Show From Base Package", "Only Show Content From the Application Base Package", TapestryIcons.CompactBasePackage)
