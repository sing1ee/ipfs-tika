# ipfs-tika
Java web application taking IPFS hashes, extracting (textual) content and metadata through Apache's Tika.

## Compiling
`mvn compile`

### gradle
```shell
gradle clean build
```

## Running
`mvn exec:java -Dexec.mainClass="com.ipfssearch.ipfstika.App"`

### gradle

```shell
./gradlew run
```
or build a package:

```shell
./gradlew distTar
```

untar the package, then run:

```shell
./bin/ipfs-tika
```

## New Feature

- gradle support
- more detail argument for parsing
