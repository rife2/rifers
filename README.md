# rife2.com website

## Running the server

```bash
./bld.sh compile run
```

Go to:

[http://localhost:8080/](http://localhost:8080/)


## Deploying the app

```bash
./bld.sh war
```

The resulting archive will be in:
`war/build/libs`


## Making an UberJar


```bash
./bld.sh uberjar
```

Then run it with:

```bash
java -jar build/dist/rifers-1.0.0-uber.jar
```