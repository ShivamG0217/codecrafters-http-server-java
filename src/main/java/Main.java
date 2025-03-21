import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;

public class Main {
  public static void main(String[] args) {


    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");
      String directory = null;

  for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--directory") && i + 1 < args.length) {
          directory = args[i + 1];
          break;
      }
  }

  if (directory == null) {
      directory = "";
      System.out.println("Directory argument not provided.");
  }

  System.out.println("Directory: " + directory);


      int availableCores = Runtime.getRuntime().availableProcessors();
      int threadPoolSize = availableCores * 2; // Example for I/O-bound tasks

    ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);

    try {
        // Since the tester restarts your program quite often, setting SO_REUSEADDR
        // ensures that we don't run into 'Address already in use' errors
        ServerSocket serverSocket = new ServerSocket(4221);
        serverSocket.setReuseAddress(true);


        while (true) {
            Socket clientSocket = serverSocket.accept();
            String finalDirectory = directory;
            threadPool.submit(() -> handleClient(clientSocket, finalDirectory));
        }
    } catch (IOException e) {
        System.out.println("IOException: " + e.getMessage());
    }
  }

  public static void handleClient(Socket clientSocket, String directory) {
      try {
          BufferedReader in = new BufferedReader(
                  new InputStreamReader(clientSocket.getInputStream())
          );
          String method = "";
          String line;
          String path = "";
          String userAgent = "";
          String body = "";
          String acceptEncoding = "";
          Set<String> acceptEncodings = new HashSet<String>();
          int contentLength = 0;
          //Request Line
          line = in.readLine();
          if (line.startsWith("GET") || line.startsWith("POST")) {
              method = line.split(" ")[0];
              path = line.split(" ")[1];
          }

          while ((line = in.readLine()) != null && !line.isEmpty()) {
              System.out.println(line);

              if (line.startsWith("User-Agent")) {
                  userAgent = line.split(" ",2)[1];
              }
              if (line.startsWith("Content-Length")) {
                  contentLength = Integer.parseInt(line.split(" ")[1]);
              }
              if (line.startsWith("Accept-Encoding")){
                  String headerValue = line.split(" ", 2)[1];
                  String[] encodings = headerValue.split(",");
                  for (String encoding : encodings) {
                      acceptEncodings.add(encoding.trim());
                  }
              }
          }

          // Read the body, if the request has one
            if (contentLength > 0) {
                char[] buffer = new char[contentLength];
                in.read(buffer, 0, contentLength);
                body = new String(buffer);
                System.out.println(body);
            }

          if (path.equals("/")) {
              clientSocket.getOutputStream().write(
                      "HTTP/1.1 200 OK\r\n\r\n".getBytes()
              );
          } else if (path.matches("/echo/.*")) {
              String message = path.split("/")[2];
              int length = message.length();
              if (acceptEncodings.contains("gzip")) {
                  clientSocket.getOutputStream().write(
                          ("HTTP/1.1 200 OK\r\n" //Status code
                                  +"Content-Type: text/plain\r\nContent-Length: "+ length +"\r\nContent-Encoding: "+ "gzip" +"\r\n\r\n" //Headers
                                  + message //Response Body
                          ).getBytes()
                  );

              } else {
                  clientSocket.getOutputStream().write(
                          ("HTTP/1.1 200 OK\r\n" //Status code
                                  +"Content-Type: text/plain\r\nContent-Length: "+ length +"\r\n\r\n" //Headers
                                  + message //Response Body
                          ).getBytes()
                  );
              }
          } else if (path.equals("/user-agent"))
          {
              int length = userAgent.length();
              clientSocket.getOutputStream().write(
                      ("HTTP/1.1 200 OK\r\n" //Status code
                              +"Content-Type: text/plain\r\nContent-Length: "+ length +"\r\n\r\n" //Headers
                              + userAgent //Response Body
                      ).getBytes()
              );
          }else if (path.matches("/files/.*") && method.equals("GET")) {
              String fileName = path.split("/")[2];
              System.out.println("fileName: " + fileName);
              File file = new File(directory + fileName);
              if (file.exists() && file.isFile()) {
                  byte[] fileContent = Files.readAllBytes(file.toPath());
                  clientSocket.getOutputStream().write(
                          ("HTTP/1.1 200 OK\r\n" // Status code
                                  + "Content-Type: application/octet-stream\r\n"
                                  + "Content-Length: " + fileContent.length + "\r\n\r\n" // Headers
                                  + new String(fileContent) // Response Body
                          ).getBytes()
                  );
              } else {
                  clientSocket.getOutputStream().write(
                          "HTTP/1.1 404 Not Found\r\n\r\n".getBytes()
                  );
              }
          }else if (path.matches("/files/.*") && method.equals("POST")) {
              String fileName = path.split("/")[2];
              //create new file with the given name
                File file = new File(directory, fileName);
                if (!file.exists()) {
                    file.createNewFile();
                }
                Files.write(file.toPath(), body.getBytes());
                clientSocket.getOutputStream().write(
                        "HTTP/1.1 201 Created\r\n\r\n".getBytes()
                );
          } else{
              clientSocket.getOutputStream().write(
                      "HTTP/1.1 404 Not Found\r\n\r\n".getBytes()
              );
          }
            clientSocket.getOutputStream().flush();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
      }
  }
}
