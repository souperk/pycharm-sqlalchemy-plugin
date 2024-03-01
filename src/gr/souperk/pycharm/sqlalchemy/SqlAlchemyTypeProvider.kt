package gr.souperk.pycharm.sqlalchemy

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.*
import com.jetbrains.python.psi.types.*


/**
 * Resolves properties with type hint `sqlalchemy.orm.Mapped[X]` as `X` when
 *  an instance of the class is used, or as `sqlalchemy.orm.attributes.InstrumentedAttribute[X]`
 *  when the class is used.
 */
class SqlAlchemyTypeProvider : PyTypeProviderBase() {

    override fun getReferenceType(
        referenceTarget: PsiElement,
        context: TypeEvalContext,
        anchor: PsiElement?
    ): Ref<PyType>? {
        if (referenceTarget is PyTargetExpression) {
            // assignment := [target-expression] = [expression]
            // println("target expression: resolving reference '${referenceTarget.text}' for '${referenceTarget.parent.text}'.")
            // [target-expression] := [expression].[identifier]
            // Keep in mind that reference may be a complex type...
            val referenceInner = referenceTarget.firstChild as? PyExpression ?: return null
            val propertyName = referenceTarget.name ?: return null
            val propertyType = getTypeForReference(referenceInner, propertyName, context)
                ?: return null
            return Ref.create(propertyType)
        }

        return super.getReferenceType(referenceTarget, context, anchor)
    }

    override fun getReferenceExpressionType(reference: PyReferenceExpression, context: TypeEvalContext): PyType? {
        // [reference] := [express].[identifier]
        val referenceInner = reference.firstChild as? PyExpression ?: return null
        val propertyName = reference.name ?: return null
        return getTypeForReference(referenceInner, propertyName, context)
    }
}


