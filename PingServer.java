import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Server to process ping requests over UDP.
 */
public class PingServer {

  public static void main(String[] argv) {
    // Server port.
    int port = -1;
    double lossRate = 0.3;
    int avgDelay = 100;

    // Process command-line arguments.
    for (String arg : argv) {
      String[] splitArg = arg.split("=");
      if (splitArg.length == 2 && splitArg[0].equals("--port")) {
        port = Integer.parseInt(splitArg[1]);
      } else if (splitArg.length == 2 && splitArg[0].equals("--loss_rate")) {
        lossRate = Double.parseDouble(splitArg[1]);
      } else if (splitArg.length == 2 && splitArg[0].equals("--avg_delay")) {
        avgDelay = Integer.parseInt(splitArg[1]);
      } else {
        System.err.println("Usage: java PingServer --port=<port> [--loss_rate=<rate>] [--avg_delay=<delay>]");
        return;
      }
    }

    // Check port number.
    if (port == -1) {
      System.err.println("Must specify port number with --port");
      return;
    }
    if (port <= 1024) {
      System.err.println("Avoid potentially reserved port number: " + port + " (should be > 1024)");
      return;
    }

    // Create random number generator for use in simulating 
    // packet loss and network delay.
    Random random = new Random();

    try {
      // Create a datagram socket for receiving and sending UDP packets
      // through the port specified on the command line.
      DatagramSocket socket = new DatagramSocket(port);

      System.out.println("Ping server listening on UDP port " + port + " ...");

      // Processing loop.
      while (true) {
        // Create a datagram packet to hold incomming UDP packet.
        DatagramPacket request = new DatagramPacket(new byte[1024], 1024);

        // Block until the host receives a UDP packet.
        socket.receive(request);
         
        // Print the recieved data.
        printData(request);

        // Decide whether to reply, or simulate packet loss.
        if (random.nextDouble() < lossRate) {
          System.out.println("   Reply not sent.");
          continue; 
        }

        // Simulate network delay.
        Thread.sleep((int) (random.nextDouble() * 2 * avgDelay));

        // Send reply.
        InetAddress clientHost = request.getAddress();
        int clientPort = request.getPort();
        byte[] buf = request.getData();
        DatagramPacket reply = new DatagramPacket(buf, buf.length, clientHost, clientPort);
        socket.send(reply);

        System.out.println("   Reply sent.");
      }
    } catch (IOException | InterruptedException e) {
      System.err.println("Error processing ping request: " + e.getMessage());
    }
  }

  /** 
   * Print ping data to the standard output stream.
   */
  private static void printData(DatagramPacket request) throws IOException {
    // Obtain references to the packet's array of bytes.
    byte[] buf = request.getData();

    // Wrap the bytes in a byte array input stream,
    // so that you can read the data as a stream of bytes.
    ByteArrayInputStream bais = new ByteArrayInputStream(buf);

    // Wrap the byte array output stream in an input stream reader,
    // so you can read the data as a stream of characters.
    InputStreamReader isr = new InputStreamReader(bais);

    // Wrap the input stream reader in a bufferred reader,
    // so you can read the character data a line at a time.
    // (A line is a sequence of chars terminated by any combination of \r and \n.) 
    BufferedReader br = new BufferedReader(isr);

    // The message data is contained in a single line, so read this line.
    String line = br.readLine();

    // Print host address and data received from it.
    System.out.println( "Received from " +  request.getAddress().getHostAddress() + ": " + line);
  }
}
