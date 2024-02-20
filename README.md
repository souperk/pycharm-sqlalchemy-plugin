# SQLAlchemy plugin for PyCharm

## Disclaimer

I am a python dev with literally zero Java/Kotlin background, while this plugin hasn't burnt down my house yet, I have no confidence it won't in the future. USE AT YOUR OWN RISK

## Features

Adds type support for model instance attributes:
```python
class Entity(DeclarativeBase):
    status: Mapped[str] = mapped_column("status", String)

entity = Entity()
entity.status = "active"    # Correct
entity.status = 10          # Warning: Expected type 'str', got 'int' instead
```

Breaks type support for equality comparisons with class attributes. (Fixed for EAP builds, unfortunately cannot fix for older versions of PyCharm...)

```python
class Entity(DeclarativeBase):
    status: Mapped[str] = mapped_column("status", String)

query = (
    select(Entity)
    .filter(Entity.status == "active")  # Warning: Expected type 'ColumnElement[bool]', got 'bool' instead
)

query = (
    select(Entity)
    .filter("active" == Entity.status)  # No warning, pycharm has a bug (see PY-24960).
)
```

See `example_project` for more examples.

## Installation

The plugin is not uploaded to JetBrains plugin registry (yet), instead you have to install from disk. The following options are available:
1. Download a build from the `builds` directory.
2. Clone the repository and use the `buildPlugin` gradle task. The build will be in the `build/distributions` directory.

The oldest version supported is `2023.3`, if you want to use an older release try to change `gradle.properties` before building. 

## Feature Requests

I started this plugin as a weekend project, I have no plan what-so-ever for it's continued development or support. However, if you got an idea for a feature, feel free to open an issue.

Some features I want to work on:
1. Conversion of v1 style column definitions to v2, i.e. `status = Column("status", String)` to `status: Mapped[str] = mapped_column("status", String)`
2. Inspection/Conversion for explicitly specifying column name, i.e. use `status: Mapped[str] = mapped_column("status", String())` instead of `status: Mapped[str] = mapped_column(String())`. 
3. Inspection for matching database schema with alchemy definitions when using data sources.

## Acknowledgements

Special thanks to the creators of [pydantic-pycharm-plugin](https://github.com/koxudaxi/pydantic-pycharm-plugin) which I used as a reference.