package ru.geekbrains.client;


import org.apache.commons.io.input.ReversedLinesFileReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class HystoryService implements ReadWriteLinesToFile{

    private PrintWriter printWriter;

    private HystoryService(PrintWriter printWriter){
        this.printWriter = printWriter;
    }

    public static HystoryService getInstance(File file) throws FileNotFoundException{
        try {
            return new HystoryService(new PrintWriter(new FileOutputStream(file, true), true));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void close() {
        if (printWriter != null){
            printWriter.close();
        }

    }

    @Override
    public void writeLineToFile(String line) {
        printWriter.println(line);
    }

    @Override
    public List<String> getLastLines(File file, Integer numberOfLines) {
        List<String> result = new ArrayList<>();
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(file, Charset.defaultCharset())){
            for (int i = 0; i < numberOfLines; i++) {
                String line = reader.readLine();
                if (line == null){
                    return result;
                }
                result.add(line);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
}
