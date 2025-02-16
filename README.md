# Railer, a plugin for [Paper](https://papermc.io/) allowing to build railways following ground shape in few clicks

Disclaimer, this is a pet project yet working find as far as I know.

No particular care have been taken to make the code easy to maintain and to understand. 

## Prerequisites

- JDK 21 or higher
- Paper minecraft server 1.21 or higher

## Build

`./gradlew clean && ./gradlew jar`

## Run

After having built.

- Copy `build/libs/Railer-1.0.jar` to the `plugins` repository of your paper server.
- Run the paper server (for example `java -jar paper-1.21.1-69.jar`)
