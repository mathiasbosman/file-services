# 🗃️ File Services

[![Build and analyze](https://github.com/mathiasbosman/file-services/actions/workflows/build.yml/badge.svg)](https://github.com/mathiasbosman/file-services/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mathiasbosman_file-services&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=mathiasbosman_file-services)

This package holds some simple interfaces to use certain file services.

## Supported services

Currently, the library supports:

- [NIO (new IO) systems](#NIO-file-system)
- [Amazon S3 systems](#S3-file-system)

## Basic Setup

### Maven

The packages can be obtained from the GitHub repository. To use them add the repository to your
Maven settings:

```xml
<repository>
  <id>github</id>
  <url>https://maven.pkg.github.com/mathiasbosman/file-services</url>
  <!-- if you use them you can enable snapshots -->
  <snapshots>
    <enabled>true</enabled>
  </snapshots>
</repository>
```

### NIO file system

Every NIO (new IO) system (`java.nio.file.FileSystem`) can be used.

The `fs-nio` dependency should be included:

```xml
<dependency>
  <groupId>be.mathiasbosman</groupId>
  <artifactId>fs-nio</artifactId>
  <version>${file-services.version}</version>
</dependency>
```

(with the preferred version)

To initiate you can pass both the system and the root path. The system can be left out in which
case `FileSystems.getDefault()` will be used.

```java
private final FileService fileService = new NIOFileService(nioSystem,"path/to/root");
// or
private final FileService fileService = new NIOFileService("path/to/root");
// which is the same as
private final FileService fileService = new NIOFileService(FileSystems.getDefault(),"path/to/root");
```

### S3 file system

For using S3 file services add the `fs-s3` dependency:

```xml
<dependency>
  <groupId>be.mathiasbosman</groupId>
  <artifactId>fs-s3</artifactId>
  <version>${file-services.version}</version>
</dependency>
```

(with the preferred version)

To set up a file service that uses Amazon's S3 file system provide the S3-system as well as the
bucket name:

```java
private final FileService fileService = new S3FileSystem(s3,"bucketName");
```

To create an Amazon S3 file system the provided static factory method can be used:

```java
AmazonS3 s3 = AmazonS3Factory.toAmazonS3(
    "endpointUrl",
    Region.EU_London.toAWSRegion(),
    "key",
    "secret",
    "bucketName",
    true,
    false
    );
    FileService fileService = new S3FileService(s3,"bucket_name");
```

## Contributing

If you wish to contribute make sure to read [the guidelines](CONTRIBUTING.md) as to which Java
version and code style you should use.

## Credits

* Amazon
* [jeremylong](https://github.com/jeremylong)
  / [DependencyCheck](https://github.com/jeremylong/DependencyCheck)