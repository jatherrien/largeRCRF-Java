package ca.joeltherrien.randomforest.utils;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * These static methods are designed to make the R interface more performant; and to avoid using R loops.
 *
 */
public final class RUtils {

    public static double[] extractTimes(final RightContinuousStepFunction function){
        return function.getX();
    }

    public static double[] extractY(final RightContinuousStepFunction function){
        return function.getY();
    }

    /**
     * Convenience method to help R package serialize Java objects.
     *
     * @param object The object to be serialized.
     * @param filename The path to the object to be saved.
     * @throws IOException
     */
    public static void serializeObject(Serializable object, String filename) throws IOException {
        final ObjectOutputStream outputStream = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
        outputStream.writeObject(object);
        outputStream.close();
    }

    /**
     * Convenience method to help R package load serialized Java objects.
     *
     * @param filename The path to the object saved.
     * @throws IOException
     */
    public static Object loadObject(String filename) throws IOException, ClassNotFoundException {
        final ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new FileInputStream(filename)));
        final Object object = inputStream.readObject();
        inputStream.close();

        return object;

    }


}
