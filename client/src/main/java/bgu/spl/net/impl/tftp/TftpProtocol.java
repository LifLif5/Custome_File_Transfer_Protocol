package bgu.spl.net.impl.tftp;


import bgu.spl.net.api.MessagingProtocol;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.*;

public class TftpProtocol implements MessagingProtocol<byte[]>
{

    private boolean terminate = false;
    private boolean isDirqTime = false;

    Queue<byte[]> packets = new LinkedList<>();
    List<Byte> receivedData = new ArrayList<>();

    String fileToDownload = "";


    @Override
    public byte[] process(byte[] message)
    {
        // ACK, DATA, BCAST, ERROR
        String msg = new String(message, StandardCharsets.UTF_8);
        String[] words = msg.split("\\s+");
        //System.out.println("start process "+ words[0]);
        short opcode = Util.opCodeFromString(words[0]);
        byte[] response =null;
            switch (opcode) {
                case 3:
                    short blockNumber = Short.parseShort(words[2]);
                    int firstDataIndex = (words[0]+" "+ words[1]+" "+words[2] +" ").getBytes().length;
                    byte[] dataToProcess = Arrays.copyOfRange(message,firstDataIndex,message.length);
                    response = handleDATA(blockNumber,dataToProcess);
                    break;
                case 4:
                    response = handleACK(words);
                    break;
                case 5:
                    response = handleError(words);
                    break;
                case 9:
                    response = handleBCAST(words);
                    break;
                default:
                    System.out.println("Unknown opcode: " + opcode);
            }

        return response;
        }


    @Override
    public boolean shouldTerminate() {
        return terminate;
    }


    private byte[] handleDATA(short blockNumber, byte[] data) {
        System.out.println("handling data");
        byte[] fileBytes;
        for (byte x : data) {
            receivedData.add(x);
        }
        if(data.length<512)
        {
            if(isDirqTime)
            {
                fileBytes = mergePackets();
                List<byte[]> fileNames = Util.splitByZero(fileBytes);
                for (byte[] file: fileNames){
                    System.out.println(new String(file,StandardCharsets.UTF_8 ));
                }
                isDirqTime = false;
            }
            else {
                Path filePath =new File("Flies/"+this.fileToDownload).toPath();
                fileBytes = mergePackets();
                try {
                    Files.write(filePath,fileBytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            this.receivedData.clear();

        }
        return ("ACK " + String.valueOf(blockNumber)).getBytes();
    }

    private byte[] handleACK(String[] words) {
        System.out.println("handling ack "+ words[1]);
        byte[] response;
        int ackNum = Integer.parseInt(words[1]);
        if (ackNum > 0) {
            response = packets.poll();

            return response;
        }
        else { //then maybe Server needs to upload a file to the Client
                if (packets.isEmpty())
                {
                    return null;
                }
                else //ACK 0 AND need to upload
                {
                    response = packets.poll();
                    return response;
                }
        }
    }


    private byte[] handleError(String[] words) {
        packets.clear();
        if (words[1].equals("1")){
            // Construct the file path
            String filePathString = "Flies/" + fileToDownload;
            Path filePath = Paths.get(filePathString);
            // Delete the file
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String errMsg ="";
        for (String word:words)
            errMsg += word+" ";
        System.out.println(errMsg);
        return null;
    }
    private byte[] handleBCAST(String[] words) {
        String action = words[1];
        String filename = words[2];
        System.out.println("BCAST " + action + " " + filename);
        return null;
    }

    private void CreateNewFile(String filename,byte[] fileBytes)
    {
        String folderPath = "Flies";
        try {
            // Create the file and write the bytes to it
            Path filePath = Paths.get(folderPath, filename);
            Files.write(filePath, fileBytes);

            System.out.println("File created successfully at: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void generateDataPackets(byte[] dataBytes){
        int size = dataBytes.length;

        short blockNumber = 1;

        while (size >= 0){

            short sizeDataInPacket = (short)Math.min(size,512);


            List<Byte> dataBlock = new LinkedList<>();
            Util.addBytes("DATA ".getBytes(),dataBlock);
            Util.addBytes(Util.shortToByteArray((short)3),dataBlock);
            Util.addBytes(Util.shortToByteArray(sizeDataInPacket),dataBlock);
            Util.addBytes(Util.shortToByteArray(blockNumber),dataBlock);

            blockNumber++;
            for (int i = 0; i < sizeDataInPacket ; i++) {
                dataBlock.add(dataBytes[i]);
            }
            byte[] blockDataArray = new byte[dataBlock.size()];
            for (int i = 0; i < blockDataArray.length; i++) {
                blockDataArray[i] = dataBlock.get(i);
            }

            this.packets.add(blockDataArray);

            if (sizeDataInPacket < 512) {
                size = -1;
            } else {
                dataBytes = Arrays.copyOfRange(dataBytes, 512, dataBytes.length);
                size = dataBytes.length;
            }
        }

    }

    private byte[] mergePackets() {
        int totalLength = 0;
        for (byte x : receivedData) {
            totalLength +=1;
        }
        byte[] merged = new byte[totalLength];
        for (int i = 0; i <merged.length ; i++) {
            merged[i]=receivedData.remove(0);
        }
        return merged;
    }

    public void setFileToUpload(String fileName)
    {
        File file = new File("Flies/"+fileName);
        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        generateDataPackets(data);
    }

    public void setFileToDownload(String filename)
    {
        fileToDownload=filename;
    }
    public void setDirq(){
        isDirqTime = true;
    }
    public boolean shouldFreeKeyboard(){
        if(!packets.isEmpty() || !receivedData.isEmpty())
            return false;
        return true;
    }
}
