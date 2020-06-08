# Blueprint

The blueprint service facilitates statistic production automation. The service scans jupyter[lab] notebooks for 
meta information about the dataset it reads and writes in order to build a graph. 

## Usage 

The java application contains two executable classes: `BlueprintApplication` and `Parser`.
The class `BlueprintApplication` is a helidon micro service that can be integrated with github hooks, the `Parser` 
application can be used to scan file locally. 

Start the service:
```shell script
> docker run --rm eu.gcr.io/prod-bip/dapla-blueprint:latest 
```

Parse a folder locally:
```
> docker run --rm eu.gcr.io/prod-bip/dapla-blueprint:latest java --enable-preview -cp blueprint.jar no.ssb.dapla.blueprint.parser.Parser --help \
  Usage: <main class> [-h] -c=<commitId> --host=<host> [--password=<password>]
                     -u=<repositoryURL> [--user=<user>] [-i=<ignores>]... ROOT
        ROOT                  the root file where to search for notebooks
    -c, --commit=<commitId>   Specify the commit to associate with the graph
    -h, --help                display a help message
        --host=<host>         Neo4J host
    -i, --ignore=<ignores>    folders to ignore
        --password=<password> Neo4J password
    -u, --url=<repositoryURL> Repository URL
        --user=<user>         Neo4J username
```
 
Blueprint based on files in notebook git repository


```
--illegal-access=warn
-Dio.netty.tryReflectionSetAccessible=true
--add-opens java.base/java.nio=io.netty.common
--add-opens java.base/jdk.internal.misc=io.netty.common
--add-opens java.base/jdk.internal.misc=ALL-UNNAMED
```

# Run unit tests in IntelliJ with the following options
```
--illegal-access=warn
-Dio.netty.tryReflectionSetAccessible=true
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
--add-opens java.base/java.io=ALL-UNNAMED
```
