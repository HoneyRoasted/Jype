package honeyroasted.jypestub.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public record JStubFolder(List<JStubFile> files) {

    public static JStubFolder read(Path dir, ObjectMapper mapper, boolean recursive) throws IOException {
        List<JStubFile> files = new ArrayList<>();

        PathMatcher jar = FileSystems.getDefault().getPathMatcher("glob:**{.yml,.jstub}");
        try(Stream<Path> walk = Files.walk(dir, recursive ? Integer.MAX_VALUE : 1).filter(jar::matches)) {
            walk.forEach(p -> {
                try {
                    files.add(mapper.readValue(p.toFile(), JStubFile.class));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return new JStubFolder(files);
    }

}
