package gr.souperk.pycharm.sqlalchemy

import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.QualifiedName

import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.fromModule
import com.jetbrains.python.psi.resolve.resolveTopLevelMember
import com.jetbrains.python.psi.types.*
import kotlin.math.exp

const val DECLARATIVE_BASE_Q_NAME = "sqlalchemy.orm.decl_api.DeclarativeBase"
const val MAPPED_Q_NAME = "sqlalchemy.orm.base.Mapped"
const val INSTRUMENTED_ATTRIBUTE_Q_NAME = "sqlalchemy.orm.attributes.InstrumentedAttribute"
const val DECLARED_ATTR_Q_NAME = "sqlalchemy.orm.decl_api.declared_attr"
const val HYBRID_PROPERTY_Q_NAME = "sqlalchemy.ext.hybrid.hybrid_property"

fun pyClassIsSubClassOfDeclarativeBase(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(DECLARATIVE_BASE_Q_NAME, context)
}

fun pyDecoratorMatchesQName(decorator: PyDecorator, qName: String, context: TypeEvalContext): Boolean {
    val expression = decorator.expression ?: return false
    val expressionType = context.getType(expression) as? PyClassType ?: return false
    return expressionType.classQName == qName
}

fun PyFunction.hasDecoratorOfQName(qName: String, context: TypeEvalContext): Boolean {
    return decoratorList?.decorators?.any {
        pyDecoratorMatchesQName(it, qName, context)
    } ?: return false
}

fun PyClass.getMappedAnnotationForProperty(
    propertyName: String,
    context: TypeEvalContext
): PyAnnotation? {
    this.findClassAttribute(propertyName, true, context)?.let { attribute ->
        return (attribute.parent as? PyAssignmentStatement)?.annotation
    }

    this.findMethodByName(propertyName, true, context)?.let { method ->
        if (method.hasDecoratorOfQName(DECLARED_ATTR_Q_NAME, context)) {
            return method.annotation
        }

        return null
    }

    return null
}

fun PyClass.getHybridPropertyFor(propertyName: String, isDefinition: Boolean, context: TypeEvalContext): PyFunction? {
    val property = methods.find {
        it.name == propertyName && it.hasDecoratorOfQName(HYBRID_PROPERTY_Q_NAME, context)
    } ?: return null
    if (!isDefinition) {
        return property
    }

    return methods.find {
        it.isHybridPropertyExpressionOverrideOf(propertyName)
    } ?: return property
}

fun PyElement.getContainingFunction(): PyFunction? {
    var currentElement: PsiElement? = this
    while (currentElement != null) {
        if (currentElement is PyFunction) {
            return currentElement
        }
        currentElement = currentElement.parent
    }
    return null
}

fun PyFunction.isHybridPropertyExpressionOverrideOf(propertyName: String): Boolean {
    return this.decoratorList?.decorators?.any {
        it.expression?.text == "$propertyName.expression"
                || it.expression?.text == "$propertyName.inplace.expression"
    } ?: return false
}

fun PyFunction.isHybridPropertyExpressionOverride(): Boolean {
    if (name == null) {
        return false
    }
    if (containingClass == null) {
        return false
    }

    return containingClass!!.methods.any {
        isHybridPropertyExpressionOverrideOf(it.name!!)
    }
}


/**
 * Attempts to resolve the type of the reference, if the reference source
 *  is an SqlAlchemy model. Currently, only the following are supported:
 *  1. Property assignment using the Mapped annotation.
 *  2. Function definitions with the @declared_attr decorator (Mapped annotation required).
 */
fun getTypeForReference(
    expression: PyExpression,
    propertyName: String,
    context: TypeEvalContext
): PyType? {
    // reference := [expression].[name]
    val expressionType = context.getType(expression) as? PyClassType ?: return null
    var isDefinition = expressionType.isDefinition
    if (!pyClassIsSubClassOfDeclarativeBase(expressionType.pyClass, context)) {
        return null
    }
    val containingFunction = expression.getContainingFunction()
    if (containingFunction != null
        // Must be a named function.
        && containingFunction.name != null
        && containingFunction.containingClass == expressionType.pyClass
    ) {
        // Expression is within a @hybrid_property, in order to properly resolve the
        //  type we need to determine whether there is an expression override or not.
        if (containingFunction.hasDecoratorOfQName(HYBRID_PROPERTY_Q_NAME, context)) {
            val hasExpressionOverride = expressionType.pyClass.methods.any {
                containingFunction != it && it.isHybridPropertyExpressionOverrideOf(
                    containingFunction.name!!
                )
            }

            if (!hasExpressionOverride) {
                isDefinition = true
            }
        } else {
            val isExpressionOverride = containingFunction.isHybridPropertyExpressionOverride()
            if (isExpressionOverride) {
                isDefinition = true
            }
        }

    }

    val annotation = expressionType.pyClass.getMappedAnnotationForProperty(propertyName, context)
    if (annotation != null) {
        return unwrapMappedAnnotation(annotation, context, isDefinition)
    }

    val hybridProperty = expressionType.pyClass.getHybridPropertyFor(propertyName, isDefinition, context)
    if (hybridProperty != null) {
        return unwrapHybridProperty(hybridProperty, context)
    }

    return null
}

/**
 * Unwraps `sqlalchemy.orm.Mapped` annotations, if the annotation does not match `sqlalchemy.orm.Mapped`
 *  `null` will be returned instead.
 */
fun unwrapMappedAnnotation(
    annotation: PyAnnotation,
    context: TypeEvalContext,
    definition: Boolean = false
): PyClassLikeType? {
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

fun unwrapHybridProperty(function: PyFunction, context: TypeEvalContext): PyType? {
    if (function.annotation != null) {
        val annotationExpression = function.annotation?.value ?: return null
        return (context.getType(annotationExpression) as? PyClassLikeType)?.toInstance()
    }

    return context.getReturnType(function)
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
