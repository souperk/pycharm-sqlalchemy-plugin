package gr.souperk.pycharm.sqlalchemy

import com.jetbrains.python.psi.PyAssignmentStatement
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.types.TypeEvalContext

const val DECLARATIVE_BASE_Q_NAME = "sqlalchemy.orm.decl_api.DeclarativeBase"

internal fun isSubClassOfDeclarativeBase(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(DECLARATIVE_BASE_Q_NAME, context)
}

fun pyClassGetMappedAttributeDeclaration(pyClass: PyClass, attributeName: String, context: TypeEvalContext): PyAssignmentStatement? {
    return pyClass.findClassAttribute(attributeName, true, context)?.parent as? PyAssignmentStatement
}

