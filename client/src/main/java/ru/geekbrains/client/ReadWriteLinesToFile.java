package ru.geekbrains.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface ReadWriteLinesToFile extends AutoCloseable {

    void close() throws FileNotFoundException;

    void writeLineToFile (String line);

    List<String> getLastLines (File file, Integer numberOfLines) throws IOException;
}
