package org.jetbrains.plugins.scala.lang.psi.api.expr

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.plugins.scala.extensions.ObjectExt
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ExpectedTypes.ParameterType
import org.jetbrains.plugins.scala.lang.psi.types.Compatibility.Expression
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.FunctionType
import org.jetbrains.plugins.scala.util.UnloadableThreadLocal

import java.util
import scala.annotation.tailrec

trait ExpectedTypes {
  def expectedExprType(expr: ScExpression, fromUnderscore: Boolean = true): Option[ParameterType]

  def expectedExprTypes(
    expr:                 ScExpression,
    withResolvedFunction: Boolean = true,
    fromUnderscore:       Boolean = true
  ): Array[ParameterType]
}

object ExpectedTypes {
  type ParameterType = (ScType, Option[ScTypeElement])

  def instance(): ExpectedTypes = ApplicationManager.getApplication.getService(classOf[ExpectedTypes])

  @tailrec
  private def checkIsUnderscore(expr: ScExpression): Boolean = {
    expr match {
      case p: ScParenthesisedExpr =>
        p.innerElement match {
          case Some(e) => checkIsUnderscore(e)
          case _ => false
        }
      case _ => ScUnderScoreSectionUtil.underscores(expr).nonEmpty
    }
  }

  def unwrapIfUnderscoreFunction(
    expr:           ScExpression,
    parameterType:  ParameterType,
    fromUnderscore: Boolean
  ): ParameterType =
    if (fromUnderscore && checkIsUnderscore(expr)) {
      parameterType._1 match {
        case FunctionType(rt: ScType, _) => (rt, None)
        case _                           => parameterType
      }
    } else parameterType

  import java.{util => ju}
  private case class KnownExpectedType(
    parameterTypes: Array[ParameterType],
    canCache:       Boolean
  )

  private val knownExpectedTypes =
    new UnloadableThreadLocal[ju.Map[Expression, KnownExpectedType]](new util.HashMap[Expression, KnownExpectedType]())

  def knownExpectedTypesFor(expr: Expression): Option[Array[ParameterType]] = {
    val cache = knownExpectedTypes.value

    if (cache.containsKey(expr)) cache.get(expr).parameterTypes.toOption
    else                         None
  }

  def withKnownExpectedTypes[T](
    expr:     Expression,
    pts:      Array[ParameterType],
    canCache: Boolean
  )(fn: =>T): T =
    try {
      knownExpectedTypes.value.put(expr, KnownExpectedType(pts, canCache))
      fn
    } finally {
      knownExpectedTypes.value.remove(expr)
    }

  def canCacheTypeFor(expr: Expression): Boolean = {
    val cache = knownExpectedTypes.value

    if (cache.containsKey(expr)) cache.get(expr).canCache
    else                         true
  }
}

