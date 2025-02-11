# ♕ BYU CS 240 Chess

This project demonstrates mastery of proper software design, client/server architecture, networking using HTTP and WebSocket, database persistence, unit testing, serialization, and security.

## 10k Architecture Overview

The application implements a multiplayer chess server and a command line chess client.

A sequence diagram describing the chess server operations can be viewed [here](https://sequencediagram.org/index.html?presentationMode=readOnly#initialData=IYYwLg9gTgBAwgGwJYFMB2YBQAHYUxIhK4YwDKKUAbpTngUSWDABLBoAmCtu+hx7ZhWqEUdPo0EwAIsDDAAgiBAoAzqswc5wAEbBVKGBx2ZM6MFACeq3ETQBzGAAYAdAE5M9qBACu2GADEaMBUljAASij2SKoWckgQaIEA7gAWSGBiiKikALQAfOSUNFAAXDAA2gAKAPJkACoAujAA9D4GUAA6aADeAETtlMEAtih9pX0wfQA0U7jqydAc45MzUyjDwEgIK1MAvpjCJTAFrOxclOX9g1AjYxNTs33zqotQyw9rfRtbO58HbE43FgpyOonKUCiMUyUAAFJForFKJEAI4+NRgACUh2KohOhVk8iUKnU5XsKDAAFUOrCbndsYTFMo1Kp8UYdKUAGJITgwamURkwHRhOnAUaYRnElknUG4lTlNA+BAIHEiFRsyXM0kgSFyFD8uE3RkM7RS9Rs4ylBQcDh8jqM1VUPGnTUk1SlHUoPUKHxgVKw4C+1LGiWmrWs06W622n1+h1g9W5U6Ai5lCJQpFQSKqJVYFPAmWFI6XGDXDp3SblVZPQN++oQADW6ErU32jsohfgyHM5QATE4nN0y0MxWMYFXHlNa6l6020C3Vgd0BxTF5fP4AtB2OSYAAZCDRJIBNIZLLdvJF4ol6p1JqtAzqBJoIcDcuj3ZfF5vD6L9sgwr5iWw63O+nxPF+SwfgC5wFrKaooOUCAHjysL7oeqLorE2IJoYLphm6ZIUgatJvqMJpEuGFocjA3K8gagrCjAoriq60pJpe8HlEa2iOs6HFOvKMAAGrAMgWiZDAEHvLxiYEvhLLlJ63pBgGQYhqx5qRtR0YwLGwY8ThnaAWmaE8tmuaYMZnbFmmwEVmBU5BrOzb-H+nbZD2MD9oOvSviOowfjWTmNi5v7Lqu3h+IEXgoOge4Hr4zDHukmSYB5F5FNQ17SAAoruOX1DlzQtA+qhPt0ql1iFaDYVe-5nECJbTs5aCWTBHZwQJCEwEh9iJahCW+hhGK1fBGryaSMDkmAemVTO1XkUybpUeUtExmp2hCmEzXVaGFHLexmVdeU07xnKuFyftClGCg3CZBQj6JHNLWLWaEaFJa6bDBANAwKVT5bTAO1zntS1sZ14JFI9SQcLdFIoCuhmHcZ5RoYl5kIHm7Ugp1QEHDZ7nnmAfYDkOS6cBF66BJCtq7tCMAAOKjqyyWnmlRNsjZ5QVAzBXFfYo7dMD6Bucm2MncFINWRDgnILETOjKoqHQsNWEyRdMgTe6U0UrNws1aDb0rTRPLrXGm2MfrhuUYdOES+b8jq+NV2TXLYAK2osKvTbH3UZEFioL9okIIzo4OhpEYy91FRuzAEAAGah6MjKNE7yPi3uKtqBZ0u47ZfQC4r4wVP0hcoAAktI4zTH0J6ZAa9nVn0OgIKADYN6BNdlwAcqBi6NPjdWEzkxNeaTvll6oxcD2YFOeJFG7YD4UDYNw8C6hJHspClZ4j5zdXc7UDT84LvT63sL4933bYE2LjVpvrsxX+K0v8ZDSmZB7sJwBvKAe6rWI06XTBpNaaetJboG9gdX2q1Ta6Q2vIQGVsI7WXOvbfSjskbALeopX+X9n4oCgWxGBukbRJxQOHLWqDOJTVHJXNOd9UzlB-l6T+o4MZY3vtQkoVwC50KrqUQeWUcaFHSqPbyl9+HjHJiueeVMAiWFukhZIMAABSEAeTkMCC3Nu7M9550PpSO8LQy5DnPi+VewBFFQDgBAJCUBq58NGJXaRosAIZ0fpJVu1jbH2KfvwtqXCo7lAAFYaLQF-dRPJ-4oDRCNIBmsXbazASpfWRDNIkLWvAh2wAkEQNaig22aCgYIOAAkiOhF3ajlhGXSu6T3rslgbyD2DEwi1OkNbA6wTaHJwMudfeNCRJiT1DAEAdjoCzA2NgMAbTRyTLACAcpWsuLYHEn-apZd1JUK0uUSkqyRktJ4oU7pOZwwMPcffcoUSInsOzpjQJqZuF41FqcMRJMfI9BkZTKKAQvBWK7F6WAwBsCr0IPERI282ZiIGTwyouV8qFWKsYNxDUmGjO4HgL2DzYJv0EiADFcJRpdWdiA7W+LAUGiVkSviiTSWKQJbNalslaU4PRYCj2VLOnEMaTIOGEl6LaFmHpRksxDnyHmSAFwXLNInJ8GcpGjDgTMIJRw7FHUDGliEccQ6byx4fK+UAA)

[![Sequence Diagram](10k-architecture.png)](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGEAtIGckCh0AcCGAnUBjEbAO2DnBElIEZVs8RCSzYKrgAmO3AorU6AGVIOAG4jUAEyzAsAIyxIYAERnzFkdKgrFIuaKlaUa0ALQA+ISPE4AXNABWAexDFoAcywBbTcLEizS1VZBSVbbVc9HGgnADNYiN19QzZSDkCrfztHFzdPH1Q-Gwzg9TDEqJj4iuSjdmoMopF7LywAaxgvJ3FC6wCLaFLQyHCdSriEseSm6NMBurT7AFcMaWAYOSdcSRTjTka+7NaO6C6emZK1YdHI-Qma6N6ss3nU4Gpl1ZkNrZwdhfeByy9hwyBA7mIT2KAyGGhuSWi9wuc0sAI49nyMG6ElQQA)

## Modules

The application has three modules.

- **Client**: The command line program used to play a game of chess over the network.
- **Server**: The command line program that listens for network requests from the client and manages users and games.
- **Shared**: Code that is used by both the client and the server. This includes the rules of chess and tracking the state of a game.

## Starter Code

As you create your chess application you will move through specific phases of development. This starts with implementing the moves of chess and finishes with sending game moves over the network between your client and server. You will start each phase by copying course provided [starter-code](starter-code/) for that phase into the source code of the project. Do not copy a phases' starter code before you are ready to begin work on that phase.

## IntelliJ Support

Open the project directory in IntelliJ in order to develop, run, and debug your code using an IDE.

## Maven Support

You can use the following commands to build, test, package, and run your code.

| Command                    | Description                                     |
| -------------------------- | ----------------------------------------------- |
| `mvn compile`              | Builds the code                                 |
| `mvn package`              | Run the tests and build an Uber jar file        |
| `mvn package -DskipTests`  | Build an Uber jar file                          |
| `mvn install`              | Installs the packages into the local repository |
| `mvn test`                 | Run all the tests                               |
| `mvn -pl shared test`      | Run all the shared tests                        |
| `mvn -pl client exec:java` | Build and run the client `Main`                 |
| `mvn -pl server exec:java` | Build and run the server `Main`                 |

These commands are configured by the `pom.xml` (Project Object Model) files. There is a POM file in the root of the project, and one in each of the modules. The root POM defines any global dependencies and references the module POM files.

## Running the program using Java

Once you have compiled your project into an uber jar, you can execute it with the following command.

```sh
java -jar client/target/client-jar-with-dependencies.jar

♕ 240 Chess Client: chess.ChessPiece@7852e922
```

