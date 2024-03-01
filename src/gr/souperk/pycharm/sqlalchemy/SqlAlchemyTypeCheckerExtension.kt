package gr.souperk.pycharm.sqlalchemy

import java.util.Optional

import com.intellij.openapi.util.RecursionManager
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.types.*
import kotlinx.collections.immutable.immutableListOf
import kotlinx.collections.immutable.persistentListOf

val REVERSED_OPERATORS = persistentListOf("__eq__", "__ne__")

/**
 * Solves type checking issue when using "==" and "!=" with `sqlalchemy.ColumnElement`. This only
 *  works for EAP builds since the PyTypeCheckerExtension has not been released yet.
 *
 * This is not a correct implementation of PyTypeCheckerExtension as it's behavior is not
 *  consistent. Depending on context (whether a binary expression is being evaluated) returns
 *  different results for the same types.
 */
class SqlAlchemyTypeCheckerExtension : PyTypeCheckerExtension {

    override fun match(expected: PyType?, actual: PyType?, context: TypeEvalContext, substitutions: MutableMap<PyGenericType, PyType>): Optional<Boolean> {
        // Fix issue with type checking of binary expressions where comparisons are reversed
        //  even if there is an override by the left operand.
        if (!isSqlOperationClass(expected) || actual?.isBuiltin != true) {
            // Not sqlalchemy related, ignore.
            return Optional.empty()
        }

        val expression = getCurrentlyEvaluatingBinaryExpression() ?: return Optional.empty()
        if (expression.referencedName !in REVERSED_OPERATORS) {
            return Optional.empty()
        }

        val leftType = context.getType(expression.leftExpression)
        val matched = PyTypeChecker.match(expected, leftType, context, substitutions)
        return Optional.of(matched)
    }
}

fun isSqlOperationClass(type: PyType?): Boolean {
    return (type as? PyClassType)?.classQName == "sqlalchemy.sql.elements.SQLCoreOperations"
}

/**
 * Uses the RecursionManager call stack to check if a binary expression
 *  is currently being evaluated. If that's the case, that binary expression is returned.
 *
 * The implementation of this method is dependent on internals of the python plugin,
 *  it's possible it may break at any time. DO NOT DO THIS AT HOME
 */
fun getCurrentlyEvaluatingBinaryExpression(): PyBinaryExpression? {
    try {
        // When evaluating the type of binary expressions, before reaching
        //  the PyTypeCheckerExtension point two entries have been added to the
        //  RecursionManager call stack.
        //  1. (PyBinaryExpression, TypeEvaluationContext) - from TypeEvaluationContext.getType
        //  2. (PyType?, PyType?) - from PyTypeChecker.match
        //
        // Here we take advantage of this to check to retrieve the binary expression
        //  currently evaluated. While, this solution relies on the behavior of python plugin
        //  internals, since there is no extension point for the type evaluation of binary expression
        //  it's the only way to resolve the issue.
        val progressMap = RecursionManager::class.java.getDeclaredField("ourStack").let { field ->
            field.isAccessible = true
            (field.get(null) as? ThreadLocal<*>)?.get()
        }?.let { stack ->
            stack.javaClass.getDeclaredField("progressMap").let { field ->
                field.isAccessible = true
                field.get(stack) as? LinkedHashMap<*, *>
            }
        } ?: return null

        val keys = progressMap.keys.toList()
        return keys[keys.size - 2].let { frame ->
            frame.javaClass.getDeclaredField("userObject").let { field ->
                field.isAccessible = true
                field.get(frame)
            }
        }.let { pair ->
            pair.javaClass.getDeclaredField("first").get(pair)
        } as PyBinaryExpression
    } catch (e: Exception) {
        // Assume any exceptions are the result of the above reflection
        //  magic going rogue.
        return null
    }
}