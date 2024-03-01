from sqlalchemy import ColumnElement
from sqlalchemy.orm import Mapped, mapped_column, DeclarativeBase


class BaseEntity(DeclarativeBase):
    pass


class TestEntity(BaseEntity):
    i1: Mapped[int] = mapped_column()


def takes_filter(v: ColumnElement[bool]) -> None:
    pass


takes_filter(TestEntity.i1 == 10)
takes_filter(TestEntity.i1 != 11)
takes_filter(TestEntity.i1 <= 12)
takes_filter(TestEntity.i1 < 13)
takes_filter(TestEntity.i1 >= 14)
takes_filter(TestEntity.i1 > 15)

takes_filter(20 == TestEntity.i1)
takes_filter(21 != TestEntity.i1)
takes_filter(22 <= TestEntity.i1)
takes_filter(23 < TestEntity.i1)
takes_filter(24 >= TestEntity.i1)
takes_filter(25 > TestEntity.i1)


def takes_bool(v: bool) -> None:
    pass


test_entity = TestEntity()

takes_bool(test_entity.i1 == 10)
takes_bool(test_entity.i1 != 11)
takes_bool(test_entity.i1 <= 12)
takes_bool(test_entity.i1 < 13)
takes_bool(test_entity.i1 >= 14)
takes_bool(test_entity.i1 > 15)

takes_bool(20 == test_entity.i1)
takes_bool(21 != test_entity.i1)
takes_bool(22 <= test_entity.i1)
takes_bool(23 < test_entity.i1)
takes_bool(24 >= test_entity.i1)
takes_bool(25 > test_entity.i1)

