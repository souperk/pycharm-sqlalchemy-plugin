from sqlalchemy import ColumnElement
from sqlalchemy.ext.hybrid import hybrid_property
from sqlalchemy.orm import declared_attr, Mapped, mapped_column, DeclarativeBase


class TestMixin:
    @declared_attr
    def s_mixin(self) -> Mapped[str]:
        return mapped_column()

    @declared_attr
    def i_mixin(self) -> Mapped[int]:
        return mapped_column()

    @declared_attr
    def b_mixin(self) -> Mapped[bool]:
        return mapped_column()


class TestEntity(TestMixin, DeclarativeBase):

    s_entity: Mapped[str] = mapped_column()
    i_entity: Mapped[int] = mapped_column()

    @hybrid_property
    def s_hybrid(self) -> str:
        return self.s_mixin + self.s_entity

    @s_hybrid.expression
    def s_hybrid(self) -> ColumnElement[str]:
        return self.s_mixin + self.s_entity

    @hybrid_property
    def i_hybrid(self) -> int:
        return self.i_mixin + self.i_entity

    @i_hybrid.expression
    def i_hybrid(self) -> ColumnElement[int]:
        return self.i_mixin + self.i_entity


def takes_int(v: int):
    pass


def takes_str(v: str):
    pass


def get_entity() -> TestEntity:
    pass


entity = get_entity()

takes_str(entity.i_entity)      # Warning: Expected type 'str', got 'int' instead
takes_str(entity.i_mixin)       # Warning: Expected type 'str', got 'int' instead
takes_str(entity.i_hybrid)      # Warning: Expected type 'str', got 'int' instead

takes_int(entity.i_entity)      # Correct
takes_int(entity.i_mixin)       # Correct
takes_int(entity.i_hybrid)      # Correct

entity.i_mixin = "10"           # Warning: Expected type 'int', got 'str' instead
entity.i_mixin = 10


