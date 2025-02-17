package bgu.spl.net.impl.tftp;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Keyboard implements Runnable {
    private final Connector connector;
    private final Scanner scanner;
    private boolean terminate;
    private TftpProtocol protocol;
    private TftpEncoderDecoder encoderDecoder;

    public Keyboard(Connector connector, TftpEncoderDecoder encDec,TftpProtocol protocol) {
        this.connector = connector;
        scanner = new Scanner(System.in);
        terminate = false;
        this.encoderDecoder = encDec;
        this.protocol =protocol;
    }

    public void run() {
        while (!terminate & !Thread.currentThread().isInterrupted()) {
            String inputString = scanner.nextLine();
            connector.lockKeyboard();
            String[] words = inputString.split("\\s+");
            if (!isInputLegal(inputString)) {
                System.out.println("command not legal");
                connector.freeKeyboard();
            } else {
                boolean shouldSendMessage = true;
                switch (words[0]) {
                    case "RRQ":
                        shouldSendMessage = handleRRQInput(inputString.substring(4));
                        break;
                    case "WRQ":
                        shouldSendMessage = handleWRQInput(inputString.substring(4));
                        break;
                    case "DIRQ":
                        protocol.setDirq();
                        break;
                    case "DISC":
                        terminate();
                        break;
                    default:
                        break;
                }
                if (shouldSendMessage) {
                    byte[] encodedMessage = encoderDecoder.encode(inputString.getBytes());
                    connector.send(encodedMessage);
                    synchronized (connector) {
                        while (connector.isKeyboardLocked()) {
                            try {
                                connector.wait();
                            } catch (InterruptedException e) {
                                terminate();
                            }
                        }
                    }
                }
                else connector.freeKeyboard();
            }
        }

        //TODO::disconect and close the program and the threads
    }

    public void terminate() {
        terminate = true;
    }

    private boolean isInputLegal(String inputString) {
        String[] words = inputString.split("\\s+");
        switch (words[0]) {
            case "LOGRQ":
            case "DELRQ":
            case "RRQ":
            case "WRQ":
                // Return true if exactly 2 words and second word does not contain '0'
                return words.length >= 2 && !inputString.contains("0");

            case "DIRQ":
            case "DISC":
                // Return true only if exactly 1 word
                return words.length == 1;

            default:
                // If none of the cases match, the input is considered illegal
                return false;
        }
    }

    private boolean handleRRQInput(String fileName) {
        //return true if should send message
        String StringFilePath = "Flies/"+fileName;
        Path filePath = Paths.get(StringFilePath);
        // Check if the file exists
        if (Files.exists(filePath)) {
            System.out.println("file already exists");
            return false;
        }
        try {
            System.out.println("Creating the file: " + fileName );
            Files.createFile(filePath);
            System.out.println("file created");
            protocol.setFileToDownload(fileName);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private boolean handleWRQInput(String fileName) {
        //return true if should send message
        String StringFilePath = "Flies/"+fileName;
        Path filePath = Paths.get(StringFilePath);
        // Check if the file exists
        if (!Files.exists(filePath)) {
            System.out.println("file does not exists");
            return false;
        }
        protocol.setFileToUpload(fileName);

        return true;
    }
}
