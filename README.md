# Doors #

The project aims at providing services around an LDAP authentication.

## Build & Run##
First, build the javascript:
```sh
$ cd doors
$ sbt
> go // Build the client JS files and move them to the right place
```

Then, start the server:
```sh
> jetty:start // Start the server
```

## Run it ! ##
Open [http://localhost:8080/](http://localhost:8080/) in your browser.

## Standalone version ##
To generate the jar and the script controling it, just run:
```sh
$ sbt toJar
```

The jar and the script are generated in server/target/scala_2.11. Just run:


```sh
$ ./doors <port>
```

Then, open ```http://<host>:<port>``` in your browser.




