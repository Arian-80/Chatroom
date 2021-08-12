import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public abstract class ResourceCloser {

    public static void closeCloseables(List<Closeable> objects){
        // Ignores any IO exception or NullPointer exception that might occur, ..-
        // -.. as for example, the latter could occur if the PrintWriter object has not been initialised yet.
        try {
            for (Closeable object : objects) {
                object.close();
            }
        } catch (IOException | NullPointerException ignored) {
        }
    }
}
