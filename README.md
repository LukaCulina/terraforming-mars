# Terraforming Mars

Terraforming Mars is a desktop strategy game built in JavaFX, inspired by the idea of turning the Red Planet into a habitable world. The project combines core Java programming, game logic, and an interactive user interface designed with Scene Builder.

The game includes a complete engine that handles all core mechanics, from resource management to turn-based gameplay. Players can choose between hot-seat multiplayer and online matches with real-time synchronization. Online players can also communicate via a Java RMI-based chat service.

Key highlights:

- 12 unique starting corporations and over 240 project cards

- Save/load functionality with automatic XML replay generation

- Host/client networking with synchronized game state

- Auto-generated code documentation using Java Reflection API

It’s built with Java 17+, using Apache Maven for building and dependency management, while XML and serialization handle data storage.



## Configuration

The game uses an application.properties file for configuration.

Location:
- The file is located in src/main/resources/hr/terraforming/mars/terraformingmars/application.properties.

Customization:
- You can modify the configuration directly in the source file. No external folders or absolute paths on your hard drive are required.

Default configuration:

```properties
rmi.port=1099
server.port=1234
hostname=localhost
```

## Running the Game

To run the game, make sure you have JDK 17 (or newer) and Maven installed. Then simply clone the repository and launch it with:

```bash
git clone https://github.com/LukaCulina/Terraforming-Mars
cd Terraforming-Mars
mvn clean javafx:run
```
