package com.somewater.jsync.core.util;

import java.io.*;

public class SerializationUtil {
    public static byte[] objectToBytes(Serializable object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;
        in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        return (T) in.readObject();
    }
}
