# rife2.com website

## Download the dependencies

```bash
./bld download
```

## Running the server

```bash
./bld compile run
```

Go to:

[http://localhost:8080/](http://localhost:8080/)


## Deploying the app

```bash
./bld war
```

The resulting archive will be in `build/dist`.

The counter and workflow demos use RIFE2's continuations, which need the RIFE2
agent at runtime. `./bld run` applies it automatically; when you deploy the WAR
to an external servlet container you must add the agent to that container's JVM
yourself, for example with `-javaagent:/path/to/rife2-agent-<version>.jar`.


## Making an UberJar


```bash
./bld uberjar
```

Then run it with:

```bash
java -jar build/dist/rifers-1.0.0-uber.jar
```