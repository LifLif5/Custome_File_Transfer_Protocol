package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private List<Byte> decodedBytes = new LinkedList<>();
    short decodedOpCode = -1;
    boolean decodingName = false;
    byte[] shortBeingDecoded;
    short dataLeftInPacket = -1;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        boolean finished = false;


        if (decodedOpCode == -1) { //opcode
            if (decodeByteToSavedShort(nextByte)) {
                decodedOpCode = Util.byteToShort(shortBeingDecoded);
                shortBeingDecoded = null;
                if (decodedOpCode<1 || decodedOpCode>10){
                    Util.addBytes("none".getBytes(),decodedBytes);
                    finished = true;
                }
                else {
                    Util.addBytes(Util.stringFromOpCode(decodedOpCode).getBytes(), decodedBytes);
                    Util.addSpace(decodedBytes);
                    switch (decodedOpCode) {
                        case 6:
                        case 10:
                            finished = true;
                            break;
                        case 1:
                        case 2:
                        case 7:
                        case 8:
                            decodingName = true;
                            break;
                    }
                }
            }
        }
        else if (decodingName)
        {
            if (decodedOpCode == 3){
                decodedBytes.add(nextByte);
                dataLeftInPacket--;
                if (dataLeftInPacket == 0) {
                    finished = true;
                    dataLeftInPacket = -1;
                }
            }
            else {
                if (nextByte == 0) {
                    finished = true;
                } else {
                    decodedBytes.add(nextByte);
                }
            }
        }
        else {
            switch (decodedOpCode) {
                case 3:
                    if (decodeByteToSavedShort(nextByte)) {

                        Util.addBytes(String.valueOf(Util.byteToShort(shortBeingDecoded)).getBytes(),decodedBytes);
                        Util.addSpace(decodedBytes);
                        if (dataLeftInPacket == -1){
                            dataLeftInPacket = Util.byteToShort(shortBeingDecoded);

                        }
                        else{
                            decodingName = true;
                        }
                        shortBeingDecoded = null;
                    }
                    break;
                case 4:
                    if (decodeByteToSavedShort(nextByte)) {

                        Util.addBytes(String.valueOf(Util.byteToShort(shortBeingDecoded)).getBytes(), decodedBytes);//not sure
                        shortBeingDecoded = null;
                        finished = true;
                    }
                    break;
                case 5:
                    if (decodeByteToSavedShort(nextByte)) {
                        Util.addBytes(String.valueOf(Util.byteToShort(shortBeingDecoded)).getBytes(), decodedBytes);
                        Util.addSpace(decodedBytes);
                        shortBeingDecoded = null;
                        decodingName = true;
                    }
                    break;
                case 9:
                    if (nextByte == 0)
                        Util.addBytes("del".getBytes(), decodedBytes);
                    else if (nextByte == 1) {
                        Util.addBytes("add".getBytes(), decodedBytes);
                    }
                    Util.addSpace(decodedBytes);
                    decodingName = true;
                    break;
            }
        }


        if (!finished){
            return null;
        }
        else{
            byte[] output = new byte[decodedBytes.size()];
            for (int i = 0; i <output.length ; i++) {
                output[i] = decodedBytes.remove(0); // remove the first element (and it clears)
            }
            decodingName = false;
            decodedOpCode = -1;
            shortBeingDecoded = null;

            return output;
        }
    }

    @Override
    public byte[] encode(byte[] message) {
        List<Byte> encodedBytes = new LinkedList<>();
            String stringMessage = new String(message, StandardCharsets.UTF_8);
            String[] words = stringMessage.split("\\s+");
            short opcode = Util.opCodeFromString(words[0]);
            if (opcode == -1) {
                return null;
            }
            Util.addBytes(Util.shortToByteArray(opcode),encodedBytes);

            switch (words[0]) {
                case "RRQ":
                case "WRQ":
                case "LOGRQ":
                case "DELRQ":
                    //add the bytes-size(filename in UTF8)
                    Util.addBytes(stringMessage.substring(words[0].length()+1).getBytes(),encodedBytes);
                    break;

                case "DATA":
                    return Arrays.copyOfRange(message,"DATA ".getBytes().length,message.length);

                case "ACK":
                    Util.addBytes(Util.shortToByteArray(Util.getShortFromNumStr(words[1])),encodedBytes);
                    break;
                case "ERROR":
                    Util.addBytes(Util.shortToByteArray(Util.getShortFromNumStr(words[1])),encodedBytes);
                    if (words.length>2){
                        for (String word : Arrays.copyOfRange(words,2,words.length)){
                            Util.addSpace(encodedBytes);
                            Util.addBytes(word.getBytes(),encodedBytes);

                        }
                    }
                    else {
                        Util.addBytes(Util.getErrorMsg(words[1]), encodedBytes);
                    }
                    break;
                case "DIRQ":
                    //do nothing
                    break;
                case "BCAST":
                    if (words[1].equals("del"))
                        encodedBytes.add((byte)0);

                    if (words[1].equals("add"))
                        encodedBytes.add((byte)1);
                    //add the bytes-size(filename in UTF8)
                    Util.addBytes(words[2].getBytes(),encodedBytes);
                    break;
                case "DISC":
                    // do nothing
                    break;
                default:
                    break;

            }
            if (opcode == 1 | opcode == 2|opcode == 5 | opcode == 7|opcode == 8 | opcode == 9) //for DISC and DIRQ
                encodedBytes.add((byte) 0);

        byte[] output = new byte[encodedBytes.size()];
        for (int i = 0; i <output.length ; i++) {
            output[i] = encodedBytes.remove(0); // remove the first element (and it clears)
        }
        return output;
    }

    private boolean decodeByteToSavedShort(byte b){
        if (shortBeingDecoded == null) {
            shortBeingDecoded = new byte[2];
            shortBeingDecoded[0] = b;
            return false;
        } else {
            shortBeingDecoded[1] = b;
            return true;
        }
    }
}