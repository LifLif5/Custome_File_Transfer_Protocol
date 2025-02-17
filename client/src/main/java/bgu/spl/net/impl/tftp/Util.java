package bgu.spl.net.impl.tftp;

import java.util.ArrayList;
import java.util.List;

public class Util {
    public static byte[] shortToByteArray(short a) {
        return new byte[]{(byte) (a >> 8), (byte) (a & 0xff)};
    }

    public static short byteToShort(byte[] bytes) {
        return (short) (((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF));
    }



    public static void addBytes(byte[] bytesToAdd, List<Byte> byteList) {
        // Assuming encodedBytes is a collection or something similar. This method's logic needs adjustment based on actual encodedBytes type.
        // For simplicity, let's assume encodedBytes is a List<Byte> or similar. Actual implementation would vary.
        for (byte b : bytesToAdd)
            byteList.add(b); // This line depends on the actual type of encodedBytes

    }

    public static short opCodeFromString(String command) {
        short output = -1; // Default output value if no case matches
        switch (command) {
            case "RRQ":
                output = 1;
                break;
            case "WRQ":
                output = 2;
                break;
            case "DATA":
                output = 3;
                break;
            case "ACK":
                output = 4;
                break;
            case "ERROR":
                output = 5;
                break;
            case "DIRQ":
                output = 6;
                break;
            case "LOGRQ":
                output = 7;
                break;
            case "DELRQ":
                output = 8;
                break;
            case "BCAST":
                output = 9;
                break;
            case "DISC":
                output = 10;
                break;
            default:
                break;
        }
        return output;
    }

    public static short getShortFromNumStr(String number) {
        return (Short.parseShort(number));
    }

    public static byte[] getErrorMsg(String val) {
        String[] errors =
                {
                        "Not defined, see error message (if any).",
                        "File not found – RRQ DELRQ of non-existing file.",
                        "Access violation – File cannot be written, read or deleted.",
                        "Disk full or allocation exceeded – No room in disk.",
                        "Illegal TFTP operation – Unknown Opcode.",
                        "File already exists – File name exists on WRQ.",
                        "User not logged in – Any opcode received before Login completes.",
                        "User already logged in – Login username already connected"
                };
        return errors[Integer.parseInt(val)].getBytes();
    }

    public static String stringFromOpCode(short opCode) {
        switch (opCode) {
            case 1:
                return "RRQ";
            case 2:
                return "WRQ";
            case 3:
                return "DATA";
            case 4:
                return "ACK";
            case 5:
                return "ERROR";
            case 6:
                return "DIRQ";
            case 7:
                return "LOGRQ";
            case 8:
                return "DELRQ";
            case 9:
                return "BCAST";
            case 10:
                return "DISC";
            default:
                return "Unknown Command";
        }
    }
    public static void addSpace(List<Byte> byteList) {
        byteList.add((byte) 0x20); // Add space (U+0020)
    }
    public static byte[] concatenateByteArrays(List<byte[]> listOfByteArrays) {
        // First, calculate the total length of the resulting concatenated array
        int totalLength = 0;
        for (byte[] byteArray : listOfByteArrays) {
            totalLength += byteArray.length;
        }

        // Create a new array that can hold all the bytes
        byte[] result = new byte[totalLength];

        // Copy bytes from the input byte arrays into the result array
        int currentPosition = 0;
        for (byte[] byteArray : listOfByteArrays) {
            System.arraycopy(byteArray, 0, result, currentPosition, byteArray.length);
            currentPosition += byteArray.length; // Move the current position for the next copy
        }

        return result;
    }
    public static List<byte[]> splitByteArray(byte[] inputArray) {
        final int chunkSize = 518; // The maximum size of each chunk
        List<byte[]> result = new ArrayList<>();

        int start = 0; // Start index of the current chunk
        while (start < inputArray.length) {
            int end = Math.min(inputArray.length, start + chunkSize); // End index (exclusive) of the current chunk
            byte[] chunk = new byte[end - start]; // Create a chunk of the appropriate size
            System.arraycopy(inputArray, start, chunk, 0, chunk.length); // Copy bytes into the chunk
            result.add(chunk); // Add the chunk to the result list
            start += chunkSize; // Move to the start of the next chunk
        }

        return result;
    }
    public static List<byte[]> splitByZero(byte[] byteArray) {
        List<byte[]> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < byteArray.length; i++) {
            if (byteArray[i] == 0x00) {
                if (i > start) {
                    byte[] part = new byte[i - start];
                    System.arraycopy(byteArray, start, part, 0, i - start);
                    parts.add(part);
                }
                start = i + 1;
            }
        }
        if (start < byteArray.length) {
            byte[] part = new byte[byteArray.length - start];
            System.arraycopy(byteArray, start, part, 0, byteArray.length - start);
            parts.add(part);
        }
        return parts;
    }
}
