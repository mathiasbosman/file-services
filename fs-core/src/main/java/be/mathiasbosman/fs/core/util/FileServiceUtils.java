package be.mathiasbosman.fs.core.util;

import com.google.common.base.Joiner;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class FileServiceUtils {

  public static final String STRIP_CHARS = File.separatorChar + " ";
  public static final Consumer<ZipEntry> noConsumer = e -> {
  };
  public static final Predicate<ZipEntry> always = e -> true;
  public static final Predicate<ZipEntry> visible = e -> {
    final String name = e.getName();
    return !StringUtils.startsWith(name, ".") && !StringUtils.contains(name, "/.");
  };

  private FileServiceUtils() {
  }

  public static InputStream getResourceAsStream(Class<?> cls, String path) {
    InputStream resourceAsStream = cls.getClassLoader().getResourceAsStream(path);
    if (resourceAsStream == null) {
      throw new IllegalStateException("Resource path not found in classpath: " + path);
    }
    return resourceAsStream;
  }

  public static void walk(ZipInputStream zipInputStream, Consumer<ZipEntryInputStream> fileConsumer,
      Consumer<ZipEntry> folderConsumer) {
    walk(zipInputStream, always, fileConsumer, folderConsumer);
  }

  public static void walk(ZipInputStream zipInputStream, Predicate<ZipEntry> predicate,
      Consumer<ZipEntryInputStream> fileConsumer, Consumer<ZipEntry> folderConsumer) {
    walk(zipInputStream, predicate, noConsumer, fileConsumer, folderConsumer);
  }

  public static void walk(ZipInputStream zipInputStream, Predicate<ZipEntry> predicate,
      Consumer<ZipEntry> consumer, Consumer<ZipEntryInputStream> fileConsumer,
      Consumer<ZipEntry> folderConsumer) {
    Set<String> unique = new HashSet<>();
    ZipEntry entry;
    try {
      while (null != (entry = zipInputStream.getNextEntry())) {
        final String name = entry.getName();
        if (StringUtils.isBlank(name)) {
          throw new IllegalArgumentException(
              "Zip file corrupt. There is an entry with a blank name.");
        }
        if (!unique.add(name)) {
          throw new IllegalArgumentException(
              "Zip file corrupt. Entry '" + name + "' is not unique.");
        }
        if (!predicate.test(entry)) {
          continue;
        }
        consumer.accept(entry);
        if (entry.isDirectory()) {
          folderConsumer.accept(entry);
        } else {
          fileConsumer.accept(new ZipEntryInputStream(zipInputStream, entry));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String strip(String path) {
    return StringUtils.strip(path, STRIP_CHARS);
  }

  public static Pair<String, String> split(String path) {
    return split(path, File.separator);
  }

  public static Pair<String, String> split(String arg, String separator) {
    if (arg == null) {
      return Pair.of(null, null);
    }
    String path = strip(arg);
    int i = path.lastIndexOf(separator);
    return 0 < i
        ? Pair.of(path.substring(0, i), path.substring(i + 1))
        : Pair.of((String) null, path);
  }

  public static String combine(String... parts) {
    if (parts == null) {
      return null;
    }
    return Joiner.on(File.separatorChar).skipNulls().join(
        Arrays.stream(parts).map(input -> {
          String stripped = strip(input);
          return StringUtils.isEmpty(stripped) ? null : stripped;
        }).collect(Collectors.toList())
    );
  }

  /**
   * Gets the parent path of a given path.
   *
   * @param path Path part
   * @return Path of the parent
   */
  public static String getParentPath(String... path) {
    return FileServiceUtils.split(FileServiceUtils.combine(path)).getLeft();
  }

  public static String appendSeparator(String path) {
    return path.endsWith(File.separator) ? path : path + File.separatorChar;
  }

  /**
   * Returns the extension (determined by checking the last ".") of a path.
   *
   * @param parts Pah parts
   * @return The extension as {@link String} excluding the "." character
   */
  public static String getExtension(String... parts) {
    String combined = combine(parts);
    return Optional.of(combined)
        .filter(f -> f.contains(String.valueOf('.')))
        .map(f -> f.substring(combined.lastIndexOf('.') + 1))
        .orElse(null);
  }
}