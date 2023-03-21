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

The resulting archive will be in:
`war/build/libs`


## Making an UberJar


```bash
./bld uberjar
```

Then run it with:

```bash
java -jar build/dist/rifers-1.0.0-uber.jar
```