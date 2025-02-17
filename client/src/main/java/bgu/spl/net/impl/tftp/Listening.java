package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessagingProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Listening implements Runnable{
    private Connector connector;
    private TftpProtocol protocol;
    private boolean terminate;
    private BufferedInputStream reader;
    private TftpEncoderDecoder encoderDecoder;
    public Listening(Connector connector, BufferedInputStream reader, TftpProtocol protocol, TftpEncoderDecoder encDec){
        this.connector = connector;
        this.reader =reader;
        this.protocol = protocol;
        this.encoderDecoder =encDec;
        terminate = false;
    }
    public void run()
    {
        try {
            int read;
            while (!terminate &&!Thread.currentThread().isInterrupted() && (read = reader.read()) >= 0) {
                byte[] msg;
                msg = encoderDecoder.decodeNextByte((byte) read);

                if (msg != null){
                    byte[] response = protocol.process(msg);
                    if (response != null){
                        connector.send(encoderDecoder.encode(response));
                    }
                    if (response == null || protocol.shouldFreeKeyboard()){
                        connector.freeKeyboard();
                    }
                }
            }
        }catch (IOException ex){
            ex.printStackTrace();
        }
    }
}
