from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship, DeclarativeBase


class BaseEntity(DeclarativeBase):
    pass


class Test2Entity(BaseEntity):
    v1: Mapped[int] = mapped_column("s1", String)
    v2: Mapped[str] = mapped_column("s1", String)
    v3: Mapped[int] = mapped_column("s1", String)


class TestEntity(BaseEntity):
    s1: Mapped[str] = mapped_column("s1", String)
    s2: Mapped[str] = mapped_column("s2", String)
    i1: Mapped[int] = mapped_column("i1", Integer)

    v1: Mapped[str] = mapped_column("v1", String)
    v2: Mapped[int] = mapped_column("v2", Integer)

    o1: Mapped[Test2Entity] = relationship("Test2Entity")

    def __init__(self):
        super().__init__()
        self.s1 = "active"
        self.o1 = Test2Entity()

    def foo1(self):
        self.s2 = self.s1
        self.s1 = "inactive"
        self.s2 = 10            # Warning: Expected type 'str', got 'int' instead
        self.s1 = self.i1       # Warning: Expected type 'str', got 'int' instead

        # Providing a wrong type for an attribute does not change its type.
        self.o1.v1 = "test"     # Warning: Expected type 'int', got 'str' instead
        self.s2 = self.o1.v1    # Warning: Expected type 'str', got 'int' instead
        self.s2 = self.o1.v2
        self.s2 = self.o1.v3    # Warning: Expected type 'str', got 'int' instead

    def foo2(self):
        self.s1 = self.i1       # Warning: Expected type 'str', got 'int' instead
        self.s1 = self.o1.v1    # Warning: Expected type 'str', got 'int' instead

    def using_nested_functions(self):
        def nested():
            self.s1 = "inactive"
            self.s2 = 10        # Warning: Expected type 'str', got 'int' instead

        pass

# TODO: Provide a warning about overriding column definitions (??).
TestEntity.s1 = 10              # Warning: Expected type 'InstrumentedAttribute[str]', got 'int' instead
