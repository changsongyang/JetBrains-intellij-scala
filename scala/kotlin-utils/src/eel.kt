@file:JvmName("EelUtil")

package org.jetbrains.plugins.scala.kotlin.util

import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.platform.eel.provider.asEelPath
import com.intellij.platform.eel.provider.getEelDescriptor
import com.intellij.platform.eel.spawnProcess
import com.intellij.util.containers.tail
import java.io.File

fun runProcess(exec: File, workingDirectory: File, passParentEnvironment: Boolean, environment: Map<String, String>, commands: List<String>): Process = runBlockingCancellable {
  val execPath = exec.toPath()
  val eelDescriptor = execPath.getEelDescriptor()
  val eel = eelDescriptor.toEelApi()

  val parentEnv =
    if (passParentEnvironment) eel.exec.fetchLoginShellEnvVariables()
    else emptyMap()

  val fullEnv = parentEnv + environment

  val workingDirectoryPath = workingDirectory.toPath().asEelPath()

  val eelProcess = eel.exec.spawnProcess(commands[0])
    .args(commands.tail())
    .workingDirectory(workingDirectoryPath)
    .env(fullEnv)
    .eelIt()
  eelProcess.convertToJavaProcess()
}
