import java.io.*;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;


    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            this.username = username;

            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            closeLeaks(socket, bufferedReader, bufferedWriter);
        }
    }

    public void sendMsg() {
        try {
            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                System.out.print(">> ");
                String message = scanner.nextLine();
                if (message.startsWith("/upload")) {
                    uploadFile(message);
                }
                else if (message.startsWith("/list")){
                    listFilesInCurrentDirectory();
                } else if (message.startsWith("/exit")){
                    System.out.println("Closing connection...");
                    closeLeaks(socket, bufferedReader, bufferedWriter);
                    System.exit(0);
                    return;
                }
                else {
                    bufferedWriter.write(username + ": " + message);
                }


                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            closeLeaks(socket, bufferedReader, bufferedWriter);
        }
    }

    private void uploadFile(String command) {
//        /upload filePath
        String[] commandParts = command.split(" ");
        String filePath = commandParts[1];
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File" + filePath + " does not exist!");
            return;
        }

//         convert the whole file to bytes and then send it
        try {
            byte[] fileBytes = new byte[(int) file.length()];
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.read(fileBytes);

            bufferedWriter.write("/upload " + file.getName() + " " + Arrays.toString(fileBytes));
            bufferedWriter.newLine();
            bufferedWriter.flush();
            fileInputStream.close();
            System.out.println("File " + file.getName() + " sent successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void downloadFile(String receivedMsg) {
//         /upload filename fileBytes(bytes)
//         while not done keep appending to downloading file in hashmap
//         when done, write to file
        String[] commandParts = receivedMsg.split(" ");
        String fileName = commandParts[1];
        String fileBytes = String.join(" ", Arrays.copyOfRange(commandParts, 2, commandParts.length));
        String downloadPath = System.getProperty("user.dir") + File.separator + "download" + File.separator + fileName;
        System.out.println("Downloading " + downloadPath + "...");
//         check if download folder exists and create if not
        File downloadFolder = new File(System.getProperty("user.dir") + File.separator + "download");
        if (!downloadFolder.exists()) downloadFolder.mkdir();

//         convert filesBytes to bytes array and then save to file in download folder
        File file = new File(downloadPath);
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("Could not create file " + downloadPath);
            return;
        }
        byte[] fileBytesArray = convertStringToByteArray(fileBytes);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(fileBytesArray);
            fileOutputStream.close();
            System.out.println("Downloaded " + downloadPath + "!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] convertStringToByteArray(String fileBytes) {
        String[] fileBytesStringArray = fileBytes.substring(1, fileBytes.length() - 1).split(", ");
        byte[] fileBytesArray = new byte[fileBytesStringArray.length];
        for (int i = 0; i < fileBytesStringArray.length; i++) {
            fileBytesArray[i] = Byte.parseByte(fileBytesStringArray[i]);
        }
        return fileBytesArray;
    }

    public void receiveMsg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String receivedMsg;
                while (socket.isConnected()) {
                    try {
                        receivedMsg = bufferedReader.readLine();
                        if (receivedMsg.startsWith("/upload")) {
                            downloadFile(receivedMsg);
                            continue;
                        }
                        System.out.println(receivedMsg);
                    } catch (IOException e) {
                        closeLeaks(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }

    private void listFilesInCurrentDirectory() {
        File currentDirectory = new File(System.getProperty("user.dir"));
        File[] files = currentDirectory.listFiles();
        for (File file : files) {
            System.out.println(file.getName());
        }
    }

    private void closeLeaks(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (socket != null) {
                socket.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException {
        boolean hasArgs = args.length > 0;
        String host = "localhost";
        int port = 6667;
        if (hasArgs) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        } else {
            System.out.println("No args provided, using default host and port");
        }


        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username for the group chat: ");
        String username = scanner.nextLine();
        Socket socket = new Socket(host, port);
        Client client = new Client(socket, username);
        client.receiveMsg();
        client.sendMsg();
    }
}

