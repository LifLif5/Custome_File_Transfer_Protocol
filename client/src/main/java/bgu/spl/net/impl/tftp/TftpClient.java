package bgu.spl.net.impl.tftp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class TftpClient {
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    public static void main(String[] args) {
        try {
            Socket sock = new Socket("127.0.0.1", 7777);

            BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream());


            Connector connector = new Connector(out);
            TftpProtocol tftpProtocol = new TftpProtocol();
            Listening listener = new Listening(connector, in,tftpProtocol, new TftpEncoderDecoder());
            Keyboard keyboard = new Keyboard(connector, new TftpEncoderDecoder(),tftpProtocol);

            Thread listeningThread = new Thread(listener);
            Thread keyboardThread = new Thread(keyboard);

            listeningThread.start();
            keyboardThread.start();
        }
        catch (IOException e) {
             throw new RuntimeException(e);
    }
    }
}
