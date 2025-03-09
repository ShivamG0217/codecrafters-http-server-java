import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage
     try {
       ServerSocket serverSocket = new ServerSocket(4221);

       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);
       Socket clientSocket = serverSocket.accept();

         BufferedReader in = new BufferedReader(
                 new InputStreamReader(clientSocket.getInputStream())
         );
       String line;
       String path = "";
       String userAgent = "";
       while ((line = in.readLine()) != null && !line.isEmpty()) {
         System.out.println(line);
         if (line.startsWith("GET")) {
           path = line.split(" ")[1];
              System.out.println("Path: " + path);
         }
         if (line.startsWith("User-Agent")) {
           userAgent = line.split(" ",2)[1];
              System.out.println("User-Agent Extracted: " + userAgent);
         }
       }
//     String httpMethod = clientRequest.split();

       if (path.equals("/")) {
         clientSocket.getOutputStream().write(
                 "HTTP/1.1 200 OK\r\n\r\n".getBytes()
         );
       } else if (path.matches("/echo/.*")) {
            String message = path.split("/")[2];
            int length = message.length();
            clientSocket.getOutputStream().write(
                    ("HTTP/1.1 200 OK\r\n" //Status code
                            +"Content-Type: text/plain\r\nContent-Length: "+ length +"\r\n\r\n" //Headers
                            + message //Response Body
                    ).getBytes()
            );
       } else if (path.equals("/user-agent"))
       {
            int length = userAgent.length();
            clientSocket.getOutputStream().write(
                    ("HTTP/1.1 200 OK\r\n" //Status code
                            +"Content-Type: text/plain\r\nContent-Length: "+ length +"\r\n\r\n" //Headers
                            + userAgent //Response Body
                    ).getBytes()
            );
       }else{
            clientSocket.getOutputStream().write(
                  "HTTP/1.1 404 Not Found\r\n\r\n".getBytes()
            );
         }
       serverSocket.accept(); // Wait for connection from client.
       System.out.println("accepted new connection");
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
