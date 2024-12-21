package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import model.FileDataResponseType;
import model.FileListResponseType;
import model.FileSizeResponseType;
import model.RequestType;
import model.ResponseType;
import model.ResponseType.RESPONSE_TYPES;
import client.loggerManager;
import java.util.*;
import java.net.SocketTimeoutException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;

public class dummyClient {
    long totalDownloadedBytes = 0;
    int totalPartRequests = 0;

    private String calculateFileMD5(String filePath) throws IOException {
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            byte[] hashBytes = md5Digest.digest(fileData);

            StringBuilder hashString = new StringBuilder();
            for (byte b : hashBytes) {
                hashString.append(String.format("%02x", b));
            }

            return hashString.toString();
        } catch (Exception e) {
            throw new IOException("Error calculating MD5 hash", e);
        }
    }

    private void getFileList(String ip, int port) throws IOException {
        while (true) {
            try {
                InetAddress IPAddress = InetAddress.getByName(ip);
                RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_LIST, 0, 0, 0, null);
                byte[] sendData = req.toByteArray();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                DatagramSocket dsocket = new DatagramSocket();
                dsocket.setSoTimeout(1000);
                dsocket.send(sendPacket);
                byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                dsocket.receive(receivePacket);
                FileListResponseType response = new FileListResponseType(receivePacket.getData());
                loggerManager.getInstance(this.getClass()).debug(response.toString());
                break;
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout occurred while retrieving file list. Retrying...");
            }
        }
    }

    private long getFileSize(String ip, int port, int file_id) throws IOException {
        while (true) {
            try {
                InetAddress IPAddress = InetAddress.getByName(ip);
                RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_SIZE, file_id, 0, 0, null);
                byte[] sendData = req.toByteArray();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                DatagramSocket dsocket = new DatagramSocket();
                dsocket.setSoTimeout(1000);
                dsocket.send(sendPacket);
                byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                dsocket.receive(receivePacket);
                FileSizeResponseType response = new FileSizeResponseType(receivePacket.getData());
                loggerManager.getInstance(this.getClass()).debug(response.toString());
                return response.getFileSize();
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout occurred while retrieving file size. Retrying...");
            }
        }
    }

    private void getFileData(String ip, int port, int file_id, long start, long end) throws IOException {
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, file_id, start, end, null);
        byte[] sendData = req.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.setSoTimeout(1000);
        dsocket.send(sendPacket);
        byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
        long maxReceivedByte = start - 1;


        String outputPath = "./downloaded_file_" + file_id;
        try (RandomAccessFile file = new RandomAccessFile(outputPath, "rw")) {
            while (maxReceivedByte < end) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    dsocket.receive(receivePacket);
                    FileDataResponseType response = new FileDataResponseType(receivePacket.getData());

                    totalPartRequests++;
                    totalDownloadedBytes += response.getData().length;

                    if (response.getStart_byte() != maxReceivedByte + 1) {
                        System.out.println("Skipped bytes detected! Expected: " + (maxReceivedByte + 1) +
                                ", Received: " + response.getStart_byte());
                        getFileData(ip, port, file_id, maxReceivedByte + 1, response.getStart_byte());
                    }

                    loggerManager.getInstance(this.getClass()).debug(response.toString());
                    if (response.getResponseType() != RESPONSE_TYPES.GET_FILE_DATA_SUCCESS) {
                        break;
                    }
                    if (response.getEnd_byte() > maxReceivedByte) {
                        maxReceivedByte = response.getEnd_byte();

                        // Save the data to file
                        file.seek(response.getStart_byte() - 1);
                        file.write(response.getData());
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("No response received within the timeout period.");
                    getFileData(ip, port, file_id, maxReceivedByte + 1, end);
                    return;
                }
            }
        }


        System.out.println("File saved to: " + outputPath);

        boolean md5Check = calculateFileMD5(outputPath).equals(calculateFileMD5("./files/test" + file_id));
        System.out.println("MD5 Check: " + (md5Check ? "True" : "False"));

        System.out.println("Performance:");

        System.out.println("- File size/total downloaded bytes ratio: " + ((double) end / totalDownloadedBytes));
        System.out.println("Information:");
        System.out.println("- Size of the downloaded file: " + end + " bytes");
        System.out.println("- Total downloaded bytes: " + totalDownloadedBytes + " bytes");
        System.out.println("- Total number of part requests: " + totalPartRequests);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("ip:port is mandatory");
        }
        String[] adr1 = args[0].split(":");
        String ip1 = adr1[0];
        int port1 = Integer.valueOf(adr1[1]);
        dummyClient inst = new dummyClient();

        System.out.println("Getting files");
        inst.getFileList(ip1, port1);
        Scanner scanner = new Scanner(System.in);
        System.out.print("Select which file to download: ");
        int fileId = scanner.nextInt();
        long fileSize = inst.getFileSize(ip1, port1, fileId);
        System.out.println("Size of file " + fileId + " is: " + fileSize);

        long startTime = System.currentTimeMillis();

        inst.getFileData(ip1, port1, fileId, 1, fileSize);
        long endTime = System.currentTimeMillis();

        double duration = (endTime - startTime) / 1000.0;
        System.out.println("- Download time: " + duration + " seconds");
        System.out.println("Download Finished");
    }
}
