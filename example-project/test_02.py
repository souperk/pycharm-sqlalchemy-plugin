from sqlalchemy import String, ColumnElement
from sqlalchemy.orm import Mapped, mapped_column, DeclarativeBase


class BaseEntity(DeclarativeBase):
    pass


class TestEntity(BaseEntity):
    v1: Mapped[int] = mapped_column("s1", String(244))
    v2: Mapped[str] = mapped_column("s1", String(244))
    v3: Mapped[int] = mapped_column("s1", String(244))


def takes_filter(v: ColumnElement[bool]) -> None:
    pass


takes_filter(TestEntity.v1 == 10)
takes_filter(TestEntity.v1 != 11)
takes_filter(TestEntity.v1 <= 12)
takes_filter(TestEntity.v1 < 13)
takes_filter(TestEntity.v1 >= 14)
takes_filter(TestEntity.v1 > 15)

takes_filter(20 == TestEntity.v1)
takes_filter(21 != TestEntity.v1)
takes_filter(22 <= TestEntity.v1)
takes_filter(23 < TestEntity.v1)
takes_filter(24 >= TestEntity.v1)
takes_filter(25 > TestEntity.v1)


def takes_bool(v: bool) -> None:
    pass


test_entity = TestEntity()

takes_bool(test_entity.v1 == 10)
takes_bool(test_entity.v1 != 11)
takes_bool(test_entity.v1 <= 12)
takes_bool(test_entity.v1 < 13)
takes_bool(test_entity.v1 >= 14)
takes_bool(test_entity.v1 > 15)

takes_bool(20 == test_entity.v1)
takes_bool(21 != test_entity.v1)
takes_bool(22 <= test_entity.v1)
takes_bool(23 < test_entity.v1)
takes_bool(24 >= test_entity.v1)
takes_bool(25 > test_entity.v1)

