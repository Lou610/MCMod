# MC Mod

A custom Minecraft mod built with Fabric for Minecraft 1.20.4.

## Features

- **Teleportation Crystal**: A special item that teleports the player 10 blocks in the direction they're looking when used
- **Custom Block**: A strong metal block that requires a tool to break
- **Custom Item Group**: A dedicated creative inventory tab for all mod items
- **Crafting Recipe**: 9 Teleportation Crystals can be crafted into 1 Custom Block
- **Built with the latest Fabric API**: Compatible with Minecraft 1.20.4

## Development Setup

### Prerequisites

- Java 17 or higher
- Gradle (included via wrapper)
- Minecraft 1.20.4

### Building the Mod

1. Clone this repository
2. Navigate to the mod directory
3. Run the following commands:

```bash
# On Windows
gradlew build

# On macOS/Linux
./gradlew build
```

### Running the Mod

To run the mod in a development environment:

```bash
# On Windows
gradlew runClient

# On macOS/Linux
./gradlew runClient
```

### Building for Distribution

To create a distributable JAR file:

```bash
# On Windows
gradlew build

# On macOS/Linux
./gradlew build
```

The built mod will be in `build/libs/` directory.

## Installation

1. Install Fabric Loader for Minecraft 1.20.4
2. Install Fabric API
3. Place the mod JAR file in your `mods` folder
4. Start Minecraft

## Mod Structure

```
src/main/java/com/example/mcmod/
├── MCMod.java              # Main mod class
└── mixin/                  # Mixin classes (if needed)

src/main/resources/
├── assets/mcmod/
│   ├── lang/en_us.json     # English translations
│   ├── models/item/        # Item models
│   └── textures/item/      # Item textures
├── fabric.mod.json         # Mod metadata
└── mcmod.mixins.json       # Mixin configuration
```

## Customization

### Adding New Items

1. Create a new Item instance in `MCMod.java`
2. Register it in the `onInitialize()` method
3. Add it to the item group
4. Create the necessary model and texture files
5. Add translations to the language file

### Adding New Blocks

1. Create a new Block instance
2. Register the block and its item
3. Create block model, texture, and blockstate files
4. Add to the item group

## License

This mod is licensed under the MIT License.

## Contributing

Feel free to submit issues and enhancement requests! 