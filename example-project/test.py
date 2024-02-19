from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship, DeclarativeBase


class BaseEntity(DeclarativeBase):
    pass


class Test2Entity(BaseEntity):
    v1: Mapped[int] = mapped_column("s1", String(244))
    v2: Mapped[str] = mapped_column("s1", String(244))
    v3: Mapped[int] = mapped_column("s1", String(244))


class TestEntity(BaseEntity):
    s1: Mapped[str] = mapped_column("s1", String(244))
    s2: Mapped[str] = mapped_column("s2", String(244))
    i1: Mapped[int] = mapped_column("i1", Integer())

    v1: Mapped[str] = mapped_column("v1", String(244))
    v2: Mapped[int] = mapped_column("v2", Integer())

    o1: Mapped[Test2Entity] = relationship("Test2Entity")

    def __init__(self):
        super().__init__()
        self.s1 = "active"
        self.o1 = Test2Entity()

    def foo1(self):
        self.s2 = self.s1
        self.s1 = "inactive"
        self.s2 = 10
        self.s1 = self.i1
        self.o1.v1 = "test"
        self.s2 = self.o1.v1
        self.s2 = self.o1.v2
        self.s2 = self.o1.v3

    def foo2(self):
        self.s1 = self.i1
        self.s1 = self.o1.v1


TestEntity.s1 = 10
