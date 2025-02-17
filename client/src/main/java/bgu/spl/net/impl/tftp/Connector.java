package bgu.spl.net.impl.tftp;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Connector {
    private volatile boolean isKeyboardLocked;
    private BufferedOutputStream writer;
    public Connector(BufferedOutputStream writer){
        this.writer = writer;
        this.isKeyboardLocked = false;
    }
    public void send(byte[] encodedBytes)
    {
        try {
            //TODO: check if we need to use this version of the buffer or the version we used in the server
            writer.write(encodedBytes);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public synchronized void lockKeyboard(){
        isKeyboardLocked = true;
    }
    public synchronized void freeKeyboard(){
        isKeyboardLocked = false;
        notifyAll();
    }

    public boolean isKeyboardLocked() {
        return isKeyboardLocked;
    }
}
