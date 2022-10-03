
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {

    private static final List<ClientHandler> clientArray = new ArrayList<>();
    private Socket socket;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private String username;

    public ClientHandler(Socket clientSocket) {

        try {
            this.socket = clientSocket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = bufferedReader.readLine(); // first message is the name of the client
            clientArray.add(this);
            broadcastMessage("SERVER: " + username + " has joined the chat!");
        } catch (IOException e) {
            closeLeaks(socket, bufferedReader, bufferedWriter);
        }

    }

    private void closeLeaks(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClient();
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

    private void broadcastMessage(String msg) {
        for (ClientHandler clients : clientArray) {
            try {
                if (!clients.getUsername().equals(username)) {
                    clients.getBufferedWriter().write(msg);
                    clients.getBufferedWriter().newLine();
                    clients.getBufferedWriter().flush();
                }
            } catch (IOException ioException) {
                closeLeaks(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    @Override
    public void run() {
        String msgFromClient;

        while (socket.isConnected()) {
            try {
                msgFromClient = bufferedReader.readLine();
                broadcastMessage(msgFromClient);
            } catch (IOException e) {
                closeLeaks(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    private void removeClient() {
        clientArray.remove(this);
        broadcastMessage("Server: " + this.username + " has left the chat");
    }

    public String getUsername() {
        return username;
    }

    public BufferedWriter getBufferedWriter() {
        return bufferedWriter;
    }
}
