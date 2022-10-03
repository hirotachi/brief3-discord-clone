import java.io.*;
import java.net.Socket;
import java.util.List;
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
                bufferedWriter.write(username + ": " + message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            closeLeaks(socket, bufferedReader, bufferedWriter);
        }
    }

    public void receiveMsg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String receivedMsg;
                while (socket.isConnected()) {
                    try {
                        receivedMsg = bufferedReader.readLine();
                        System.out.println(receivedMsg);
                    } catch (IOException e) {
                        closeLeaks(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
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
        if(hasArgs) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }else{
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

