# Installation

The library is hosted on **GitHub Packages**. You need to configure your project to resolve dependencies from there.

## Maven

### 1. Configure Repository
Add the repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub TitanEch0 Apache Maven Packages</name>
        <url>https://maven.pkg.github.com/TitanEch0/TopGames-API</url>
    </repository>
</repositories>
```

### 2. Add Dependency
Add the library dependency:

```xml
<dependency>
    <groupId>io.github.titanech0</groupId>
    <artifactId>topgames-api</artifactId>
    <version>1.0.5</version>
</dependency>
```

### 3. Authentication (Important!)
GitHub Packages requires authentication to download artifacts. Add your GitHub credentials (username and Personal Access Token) to your `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

## Gradle

Add the repository and dependency to your `build.gradle`:

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/TitanEch0/TopGames-API")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'io.github.titanech0:topgames-api:1.0.5'
}
```
