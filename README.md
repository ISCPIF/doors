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
