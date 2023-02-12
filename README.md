# rife2.com website

## Running the server

```bash
./gradlew clean run
```

Go to:

[http://localhost:8080/](http://localhost:8080/)


## Deploying the app

```bash
./gradlew clean war
```

The resulting archive will be in:
`war/build/libs`


## Making an UberJar


```bash
./gradlew clean uberJar
```

Then run it with:

```bash
java -jar app/build/libs/rifers-uber-1.0.jar
```