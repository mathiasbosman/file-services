[![Build and analyze](https://github.com/mathiasbosman/file-services/actions/workflows/build.yml/badge.svg)](https://github.com/mathiasbosman/file-services/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mathiasbosman_file-services&metric=alert_status)](https://sonarcloud.io/dashboard?id=mathiasbosman_file-services)
# 🗃️ File Services

This package holds some simple interfaces to use certain file services.

## Supported services

Currently, the library supports:

- [NIO (new IO) systems](#NIO-file-system)
- [Amazon S3 systems](#S3-file-system)

## Setup

Setting up a file service is as simple as calling its constructor and passing the file system to be
used.

### NIO file system

Every NIO (new IO) system (`java.nio.file.FileSystem`) can be used.

To initiate you can pass both the system and the root path. The system can be left out in which
case `FileSystems.getDefault()` will be used.

```java
private final FileService fileService = new NIOFileService(nioSystem, "path/to/root");
// or
private final FileService fileService = new NIOFileService("path/to/root");
// which is the same as
private final FileService fileService = new NIOFileService(FileSystems.getDefault(), "path/to/root");
```

### Nextcloud

To use a Nextcloud service pass the `NextCloudConnector` instance. It is only possible to use a
single user to connect.

If need be you can create multiple instances of the service with different connectors.

```java
NextcloudConnector connector = new NextcloudConnector("localhost", false, 9002,
    "admin", "admin");
private final FileService fileService = new NextcloudFileService(connector);
// or
private final FileService fileService = new NIOFileService("path/to/root");
// which is the same as
private final FileService fileService = new NIOFileService(FileSystems.getDefault(), "path/to/root");
```

Be aware that this service relies heavily on the underlying supporting systems such as the database
and the API.

### S3 file system

To set up a file service that uses Amazon's S3 file system provide the S3-system as well as the
bucket name:

```java
private final FileService fileService = new S3FileSystem(s3, "bucketName");
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
FileService fileService = new S3FileService(s3, "bucket_name");
```

## Contributing

If you wish to contribute make sure to read [the guidelines](CONTRIBUTING.md) as to which Java
version and code style you should use.

## Credits

* Amazon
* [NextcloudConnector](https://github.com/a-schild/nextcloud-java-api)
  from [a-schild](https://github.com/a-schild)