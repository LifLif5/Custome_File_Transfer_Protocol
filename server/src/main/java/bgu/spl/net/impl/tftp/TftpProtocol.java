package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;
//import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {
    private String username = "";
    private boolean terminate = false;
    private ConnectionsImpl<byte[]> connections;
    private int client;

    Queue<byte[]> packets = new LinkedList<>();
    List<Byte> receivedData = new ArrayList<>();

    int currentPacket = 0;
    int currentReceivedPacket = 1;

    boolean needToUpload=false;
    String fileToUpload=null;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connections = (ConnectionsImpl<byte[]>) connections;
        client = connectionId;
    }

    @Override
    public void process(byte[] message) {
        String stringMessage = new String(message, StandardCharsets.UTF_8);
        System.out.println("started process "+ stringMessage);
        String[] words = stringMessage.split("\\s+");
        short opcode = Util.opCodeFromString(words[0]);
        String response = "";

        if (username.equals("") & (opcode != 7& opcode!=10)){
            System.out.println(opcode);
            response = "ERROR 6";
        }
        else {
            switch (opcode) {
                case 1:
                    try {
                        response = handleRRQ(words);
                    } catch (IOException e) {}
                    break;
                case 2:
                    response = handleWRQ(words);
                    break;
                case 3:
                    short blockNumber = Short.parseShort(words[2]);
                    int firstDataIndex = (words[0]+" "+ words[1]+" "+words[2] +" ").getBytes().length;
                    byte[] dataToProcess = Arrays.copyOfRange(message,firstDataIndex,message.length);
                    response = handleDATA(blockNumber,dataToProcess);
                    break;
                case 4:
                    handleACK(words);
                    break;
                case 5:
                    response = handleError(words);
                    break;
                case 6:
                    this.handleDIRQ();
                    break;
                case 7:
                    response = handleLOGRQ(words);
                    break;
                case 8:
                    response = handleDELRQ(words);
                    break;
                case 9:
                    response = handleBCAST(words);
                    break;
                case 10:
                    response = handleDISC();
                    break;
                default:
                    response = "ERROR 4";
                    break;
            }
        }
        System.out.println("the response is: " + response);
        if (!response.equals(""))
            connections.send(client,response.getBytes());

    }

    @Override
    public boolean shouldTerminate() {
        return terminate;
    }
    private String handleRRQ(String[] words) throws IOException {
        String fileName = words[1];
        File file = new File("Flies/"+fileName);
        String response = "";

        if (!file.exists()) {
            System.out.println(file.toPath());
            response = "ERROR 1";
        }
        else {
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                generateDataPackets(data);
                connections.send(client,packets.poll());
                currentPacket++;
            }
            catch (IOException e) {}
        }
        return response;
    }

    private String handleWRQ(String[] words) {
        String fileName = words[1];
        File file = new File(fileName);
        String StringFilePath = "Flies/"+fileName;
        // Convert the file path string to a Path object
        Path filePath = Paths.get(StringFilePath);
        String response = "";
        try {
            // Check if the file exists
            if (!Files.exists(filePath)) {
                System.out.println("the file does not exist");
                Files.createFile(filePath);
                this.needToUpload = true;//server is waiting for an upload
                this.fileToUpload = fileName;
                response = "ACK 0";
            } else {
                response = "ERROR 5";//file already exists
            }
        } catch (IOException e) {
            response = "ERROR 0 "+e.getMessage();//not defined
        }
        return response;
    }

    private String handleDATA(short blockNumber, byte[] data) {
        System.out.println("handling data");
        byte[] fileBytes;
        for (byte x : data) {
            receivedData.add(x);
        }
        currentReceivedPacket++;
        if(data.length<512)
        {
            needToUpload=false;
            fileBytes  = mergePackets();
            CreateNewFile(fileToUpload,fileBytes);
            connections.broadcast(("BCAST add "+this.fileToUpload).getBytes());
            this.fileToUpload = "";
        }

        return "ACK " + blockNumber;
    }

    private void handleACK(String[] words) {
        int ackNum=Integer.parseInt(words[1]);
        if(ackNum==0){//then maybe client needs to upload a file to the server
            if(shouldTerminate())
            {
                this.connections.disconnect(client);
                return;
            }
            if(!packets.isEmpty())
            {
                //start uploading the file
                String fileName = fileToUpload;
                File file = new File(fileName);
                try {
                    byte[] data = Files.readAllBytes(file.toPath());
                    generateDataPackets(data);
                    connections.send(client,packets.poll());
                }
                catch (IOException e) {}
            }

            else {
                return;
            }
        }
        else if (!packets.isEmpty()) {//not ACK 0 , so sending packet
            connections.send(client, packets.poll());
        }
        else {
            fileToUpload = "";
        }
    }

    private String handleError(String[] words) {
        //throw new NotImplementedException();
return null;
    }

    private void handleDIRQ() {
        List<Byte> dataList =new LinkedList<>() ;
        Path folder = getFilesPath();
        // Collect all file names into a single string
        try {
            for (Path itemPath : Files.newDirectoryStream(folder)) {
                Util.addBytes(itemPath.getFileName().toString().getBytes(),dataList);
                dataList.add((byte)0);
            }
            byte[] dataArr = new byte[dataList.size()];
            for (int i = 0; i <dataArr.length; i++) {
                dataArr[i] = dataList.remove(0);
            }
            generateDataPackets(dataArr);
            connections.send(client,packets.poll());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String handleLOGRQ(String[] words) {
        //TODO not good
        System.out.println("im in handleLOGRQ!");
        String response = "";
        String username=words[1];
        if(connections.checkIfUserNameExist(username))
            response = "ERROR 7";
        else if (connections.checkIfClientLoggedIn(client))
            response = "ERROR 0 You Are Already Logged In";
        else{
            response = "ACK 0";
            this.username = username;
            connections.addUsername(client,username);
        }
        return response;
    }

    private String handleDELRQ(String[] words) {
        String fileName = words[1];
        File file = new File(fileName);
        String StringFilePath = "Flies/"+fileName;
        // Convert the file path string to a Path object
        Path filePath = Paths.get(StringFilePath);
        String response = "";
        // Check if the file exists
        if (!Files.exists(filePath)) {
            response = "ERROR 1";//file not found
        } else {
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            response = "ACK 0";
            connections.broadcast(("BCAST del "+fileName).getBytes());
        }
        return response;
    }

    private String handleBCAST(String[] words) {
        return "";
    }

    private String handleDISC() {
        connections.disconnect(client);
        this.terminate = true;
        return "ACK 0";
    }


    private static Path getFilesPath() {
        String folderPath = "Flies";
        String projectRoot = System.getProperty("user.dir");
        // Construct the absolute folder path by concatenating the project root and the relative folder path
        folderPath = Paths.get(projectRoot, folderPath).toString();

        // Create a Path object representing the folder path
        return Paths.get(folderPath);

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


}
