package gr.souperk.pycharm.sqlalchemy

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.*
import com.jetbrains.python.psi.resolve.fromFoothold
import com.jetbrains.python.psi.resolve.resolveTopLevelMember
import com.jetbrains.python.psi.types.*


/**
 * Resolves properties with type hint sqlalchemy.Mapped[X] as X when
 *  an instance of the class is used, or as sqlalchemy.ColumnElement[X] when
 *  the class used.
 */
class SqlAlchemyTypeProvider : PyTypeProviderBase() {

    override fun getReferenceType(referenceTarget: PsiElement, context: TypeEvalContext, anchor: PsiElement?): Ref<PyType>? {
        if (referenceTarget is PyTargetExpression) {
            // assignment := [target-expression] = [expression]
            println("target expression: resolving reference '${referenceTarget.text}' for '${referenceTarget.parent.text}'.")
            // [target-expression] := [expression].[identifier]
            // Keep in mind that reference may be a complex type...
            val referenceInner = referenceTarget.firstChild as? PyExpression ?: return null
            val attributeName = referenceTarget.name ?: return null
            val columnType = getAttributeTypeForReference(referenceInner, attributeName, context)
                    ?: return null
            return Ref.create(columnType)
        }

        return super.getReferenceType(referenceTarget, context, anchor)
    }

    override fun getReferenceExpressionType(reference: PyReferenceExpression, context: TypeEvalContext): PyType? {
        // [reference] := [express].[identifier]
        val referenceInner = reference.firstChild as? PyExpression ?: return null
        val attributeName = reference.name ?: return null
        return getAttributeTypeForReference(referenceInner, attributeName, context)
    }
}

public fun getAttributeTypeForReference(expression: PyExpression, attributeName: String, context: TypeEvalContext): PyClassType? {
    // reference := [source].[name]
    val expressionType = context.getType(expression) as? PyClassType ?: return null
    val expressionClass = expressionType.pyClass
            .takeIf { isSubClassOfDeclarativeBase(it, context) } ?: return null
    println("reference: resolved declarative model '${expressionClass.qualifiedName}'.")
    val attribute = pyClassGetMappedAttributeDeclaration(expressionClass, attributeName, context)
            ?: return null
    println("reference: resolved attribute declaration '${attribute.text}' for '${attributeName}'.")
    // assignment := [target-expression]: [annotation] = [expression]
    val annotation = attribute.annotation ?: return null

    val columnType = unwrapMappedAnnotation(annotation, context, expressionType.isDefinition) ?: return null
    println("reference: resolved type is '${columnType.name}'.")
    return columnType
}

/**
 * Unwraps `Mapped` annotations, if the annotation does not match `sqlalchemy.orm.base.Mapped`
 *  null will be returned.
 */
public fun unwrapMappedAnnotation(annotation: PyAnnotation, context: TypeEvalContext, definition: Boolean = false): PyClassType? {
    // annotation := : [subscription]
    // subscription := [expression] [ [expression] ]
    val subscription = annotation.value as? PySubscriptionExpression ?: return null
    val operandType = context.getType(subscription.operand) as? PyClassType
    if (operandType?.classQName != "sqlalchemy.orm.base.Mapped") {
        return null
    }
    val index = subscription.indexExpression ?: return null
    if (definition) {
        return getInstrumentedAttributeType(index, context)
    }
    val indexType = context.getType(index) as? PyClassType ?: return null
    return indexType.toInstance() as? PyClassType
}


fun getInstrumentedAttributeType(expression: PyExpression, context: TypeEvalContext): PyClassType? {
    val instrumentedAttributeClassDefinition = resolveTopLevelMember(
            QualifiedName.fromDottedString("sqlalchemy.orm.attributes.InstrumentedAttribute"),
            fromFoothold(expression),
    ) as? PyClass ?: return null
    val instrumentedAttributeType = context.getType(instrumentedAttributeClassDefinition) as? PyClassType ?: return null
    val expressionType = context.getType(expression) as? PyClassType ?: return null
    return PyTypeChecker.parameterizeType(
            instrumentedAttributeType,
            listOf(expressionType.toInstance()),
            context
    ) as? PyClassType
}