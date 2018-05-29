package me.aki.sbt.bukkit;

import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Encoder {
    private final OutputStream out;

    public Encoder(OutputStream out) {
        this.out = out;
    }

    public void writeByte(int i) {
        try {
            out.write(i);
        } catch (IOException e) {
            throw  new IOError(e);
        }
    }

    public void writeByteArray(byte[] array) {
        try {
            writeInt(array.length);
            out.write(array);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void writeInt(int i) {
        writeByte(i);
        writeByte(i >>> 8);
        writeByte(i >>> 16);
        writeByte(i >>> 24);
    }

    public void writeString(String string) {
        writeByteArray(string.getBytes(StandardCharsets.UTF_8));
    }

    public <T> void writeArray(BiConsumer<Encoder, T> writeFunction, T[] array) {
        writeInt(array.length);

        for(T element : array)
            writeFunction.accept(this, element);
    }
}
