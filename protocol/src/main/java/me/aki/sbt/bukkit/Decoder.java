package me.aki.sbt.bukkit;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class Decoder {
    private final InputStream in;

    public Decoder(InputStream in) {
        this.in = in;
    }

    public int readByte() {
        try {
            int b = in.read();

            if(b == -1)
                throw new IOException("EndOfStream");

            return b;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public <T> T[] readArray(Function<Decoder, T> readFunction, Class<T> tClass) {
        T[] array = (T[])Array.newInstance(tClass, readInt());
        for (int i = 0; i < array.length; i++) {
            array[i] = readFunction.apply(this);
        }
        return array;
    }

    public byte[] readByteArray() {
        byte[] array = new byte[readInt()];
        try {
            in.read(array);
        } catch (IOException e) {
            throw new IOError(e);
        }
        return array;
    }

    public int readInt() {
        return readByte() |
                readByte() << 8 |
                readByte() << 16 |
                readByte() << 24;
    }

    public String readString() {
        return new String(readByteArray(), StandardCharsets.UTF_8);
    }
}
