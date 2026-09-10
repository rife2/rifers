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

The counter and workflow demos use RIFE2's continuations. `./bld run` applies
the RIFE2 agent automatically, and `./bld war` and `./bld uberjar` instrument
the classes ahead of time, so the deployed archive doesn't need the agent on the
container's JVM.


## Making an UberJar


```bash
./bld uberjar
```

Then run it with:

```bash
java -jar build/dist/rifers-2.0.6-uber.jar
```