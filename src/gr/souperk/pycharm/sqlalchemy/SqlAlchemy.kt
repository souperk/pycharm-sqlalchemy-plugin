package gr.souperk.pycharm.sqlalchemy

import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.util.QualifiedName

import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.fromModule
import com.jetbrains.python.psi.resolve.resolveTopLevelMember
import com.jetbrains.python.psi.types.*

const val DECLARATIVE_BASE_Q_NAME = "sqlalchemy.orm.decl_api.DeclarativeBase"
const val MAPPED_Q_NAME = "sqlalchemy.orm.base.Mapped"
const val INSTRUMENTED_ATTRIBUTE_Q_NAME = "sqlalchemy.orm.attributes.InstrumentedAttribute"

fun pyClassIsSubClassOfDeclarativeBase(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(DECLARATIVE_BASE_Q_NAME, context)
}

fun pyClassGetMappedAttributeDefinition(pyClass: PyClass, attributeName: String, context: TypeEvalContext): PyAssignmentStatement? {
    return pyClass.findClassAttribute(attributeName, true, context)?.parent as? PyAssignmentStatement
}


fun getAttributeTypeForReference(expression: PyExpression, attributeName: String, context: TypeEvalContext): PyClassLikeType? {
    // reference := [expression].[name]
    val expressionType = context.getType(expression) as? PyClassType ?: return null
    if (!pyClassIsSubClassOfDeclarativeBase(expressionType.pyClass, context)) {
        return null
    }
    val attribute = pyClassGetMappedAttributeDefinition(expressionType.pyClass, attributeName, context)
            ?: return null

    // assignment := [target-expression]: [annotation] = [expression]
    val annotation = attribute.annotation ?: return null

    return unwrapMappedAnnotation(annotation, context, expressionType.isDefinition)
}

/**
 * Unwraps `sqlalchemy.orm.Mapped` annotations, if the annotation does not match `sqlalchemy.orm.Mapped`
 *  `null` will be returned instead.
 */
fun unwrapMappedAnnotation(annotation: PyAnnotation, context: TypeEvalContext, definition: Boolean = false): PyClassLikeType? {
    // annotation := : [subscription]
    // subscription := [expression] [ [expression] ]
    val subscription = annotation.value as? PySubscriptionExpression ?: return null
    val operandType = context.getType(subscription.operand) as? PyClassType
    if (operandType?.classQName != MAPPED_Q_NAME) {
        return null
    }
    val index = subscription.indexExpression ?: return null
    val indexType = context.getType(index) as? PyClassLikeType
            ?: return null
    if (definition) {
        return createInstrumentedAttributeType(indexType.toInstance(), context)
    }
    return indexType.toInstance()
}


/**
 *
 */
fun createInstrumentedAttributeType(type: PyClassLikeType, context: TypeEvalContext): PyClassType? {
    val module = ModuleUtil.findModuleForFile(context.origin) ?: return null
    val instrumentedAttributeClassDefinition = resolveTopLevelMember(
            QualifiedName.fromDottedString(INSTRUMENTED_ATTRIBUTE_Q_NAME),
            fromModule(module),
    ) as? PyClass ?: return null
    val instrumentedAttributeType = context.getType(instrumentedAttributeClassDefinition) as? PyClassType ?: return null
    // TODO: Implement without using internal api `PyTypeChecker.parameterizeType`.
    return PyTypeChecker.parameterizeType(
            instrumentedAttributeType,
            listOf(type.toInstance()),
            context
    ) as? PyClassType
}