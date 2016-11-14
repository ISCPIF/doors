# Doors #

The project aims at providing services around an LDAP authentication.

## Build & Run##
First, build the javascript:
```sh
$ cd doors/application
$ sbt
> project lab    // selects the parent project
> run            // creates the .war file and runs
```

## Run it ! ##
Open [http://localhost:8989/](http://localhost:8989/) in your browser.

## Standalone version ##
To generate the jar and the script controling it, just run:
```sh
$ sbt toJar
```

The jar and the script are generated in lab/target/scala_2.11. Just run:


```sh
$ ./doors <port>
```

Then, open ```http://<host>:<port>``` in your browser.
