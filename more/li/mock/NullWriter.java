package li.mock;

import java.io.IOException;
import java.io.Writer;

/**
 *  @author : 明伟 
 */
public class NullWriter extends Writer {
    public void write(char[] cbuf, int off, int len) throws IOException {}

    public void flush() throws IOException {}

    public void close() throws IOException {}
}