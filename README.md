# File Services

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

### S3 file system

To set up a file service that uses Amazon's S3 file system provide the S3 system and the bucket
name:

```java
private final FileService fileService = new S3FileSystem(s3, "bucketName");
```

To create an Amazon S3 file system the provided factory can be used:

```java
AmazonS3 s3 = AmazonS3Factory.builder()
    .key("minio_key")
    .secret("minio_secret")
    .bucket("bucket_name")
    .serviceEndpoint("minio_url")
    .pathStyleAccessEnabled(true)
    .createBucketIfMissing(true)
    .build()
    .toAmazonS3();
FileService fileService = new S3FileService(s3, "bucket_name");
```

## Contributing

If you wish to contribute make sure to read [the guidelines](CONTRIBUTING.md) as to which Java
version and code style you should use.