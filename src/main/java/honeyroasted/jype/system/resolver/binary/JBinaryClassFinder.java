package honeyroasted.jype.system.resolver.binary;

import honeyroasted.jype.location.JClassLocation;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public interface JBinaryClassFinder {

    Optional<byte[]> locate(JClassLocation location) throws IOException;

    void index(BiConsumer<JClassLocation, byte[]> visitor) throws IOException;

    final class Multi implements JBinaryClassFinder {
        private List<JBinaryClassFinder> finders;

        public Multi(List<JBinaryClassFinder> finders) {
            this.finders = finders;
        }

        @Override
        public Optional<byte[]> locate(JClassLocation location) throws IOException {
            for (JBinaryClassFinder finder : this.finders) {
                Optional<byte[]> found = finder.locate(location);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }

        @Override
        public void index(BiConsumer<JClassLocation, byte[]> visitor) throws IOException {
            for (JBinaryClassFinder finder : this.finders) {
                finder.index(visitor);
            }
        }
    }

    final class Jar implements JBinaryClassFinder {
        private Path path;

        public Jar(Path path) {
            this.path = path;
        }

        @Override
        public Optional<byte[]> locate(JClassLocation location) throws IOException {
            try(JarFile file = new JarFile(this.path.toFile())) {
                JarEntry target = file.getJarEntry(location.toInternalName() + ".class");
                if (target != null) {
                    return Optional.of(file.getInputStream(target).readAllBytes());
                }
            }
            return Optional.empty();
        }

        @Override
        public void index(BiConsumer<JClassLocation, byte[]> visitor) throws IOException {
            try(JarFile file = new JarFile(this.path.toFile())) {
                Enumeration<JarEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    JarEntry next = entries.nextElement();
                    String name = next.getRealName();
                    if (name.endsWith(".class")) {
                        JClassLocation location = JClassLocation.of(name.substring(0, name.length() - 6));
                        byte[] read = file.getInputStream(next).readAllBytes();
                        visitor.accept(location, read);
                    }
                }
            }
        }
    }

    final class Dir implements JBinaryClassFinder {
        private Path root;

        public Dir(Path root) {
            this.root = root.toAbsolutePath();
        }

        @Override
        public Optional<byte[]> locate(JClassLocation location) throws IOException {
            Path target = this.root.resolve(location.toInternalName() + ".class");
            if (Files.exists(target)) {
                return Optional.of(Files.readAllBytes(target));
            }
            return Optional.empty();
        }

        @Override
        public void index(BiConsumer<JClassLocation, byte[]> visitor) throws IOException {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.class");
            for (Path target : Files.walk(this.root).toList()) {
                if (matcher.matches(target)) {
                    JClassLocation location = JClassLocation.of(this.root.relativize(target.toAbsolutePath()).toString());
                    byte[] read = Files.readAllBytes(target);
                    visitor.accept(location, read);
                }
            }
        }
    }

}
