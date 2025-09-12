package org.jetbrains.plugins.scala.kotlin.util

import org.junit.Assert.fail
import org.junit.Test

class FailingTest {
  @Test
  fun failing() {
    fail("This test must fail")
  }
}
