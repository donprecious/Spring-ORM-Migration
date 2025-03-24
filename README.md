# Java ORM Migration Tool

A Java ORM schema migration tool for automating database schema changes based on entity definitions.

## Features

- Automatic schema comparison between entity model and database
- Migration script generation
- Migration tracking
- Schema validation
- Support for multiple database dialects

## Requirements

- Java 17+
- Maven
- Mockito 5.11.0+ for Java 23 support

## Getting Started

1. Clone the repository
2. Build with Maven: `mvn clean install`
3. Run the tests: `mvn test`

## Configuration

To use with Java 23, ensure ByteBuddy experimental mode is enabled in the POM as shown:

```xml
<properties>
    <net.bytebuddy.experimental>true</net.bytebuddy.experimental>
</properties>
```

And configure the surefire plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Dnet.bytebuddy.experimental=${net.bytebuddy.experimental}</argLine>
    </configuration>
</plugin>
```
