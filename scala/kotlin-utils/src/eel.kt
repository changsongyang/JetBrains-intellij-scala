@file:JvmName("EelUtil")
@file:Suppress("UnstableApiUsage")

package org.jetbrains.plugins.scala.kotlin.util

import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.platform.eel.provider.asEelPath
import com.intellij.platform.eel.provider.getEelDescriptor
import com.intellij.platform.eel.spawnProcess
import com.intellij.util.containers.tail
import io.ktor.util.normalizeAndRelativize
import java.io.File

fun runProcess(exec: File, workingDirectory: File, passParentEnvironment: Boolean, environment: Map<String, String>, commands: List<String>): Process =
  runBlockingMaybeCancellable {
    val execPath = exec.toPath().toAbsolutePath().normalize()
    val eelDescriptor = execPath.getEelDescriptor()
    val eel = eelDescriptor.toEelApi()

    val parentEnv =
      if (passParentEnvironment) eel.exec.fetchLoginShellEnvVariables()
      else emptyMap()

    val fullEnv = parentEnv + environment

    val workingDirectoryPath = workingDirectory.toPath().toAbsolutePath().normalize().asEelPath()

    val eelProcess = eel.exec.spawnProcess(commands[0])
      .args(commands.tail())
      .workingDirectory(workingDirectoryPath)
      .env(fullEnv)
      .eelIt()
    eelProcess.convertToJavaProcess()
  }
