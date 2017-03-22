import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PingClient {

	public static void main(String[] argv) throws Exception {

		//Set a defult port number for ping client
		int default_port = 5005;

		//Command Line Arguments
		String server_ip = "";
		int server_port = -1;
		int ping_count = -1;
		long period = -1;
		int timeout = -1;

		//Parse Command Line Arguments
		for (String arg : argv) {
		    String[] splitArg = arg.split("=");
		    if (splitArg.length == 2 && splitArg[0].equals("--server_ip")) {
		    	server_ip = splitArg[1];
		    } else if (splitArg.length == 2 && splitArg[0].equals("--server_port")) {
		        server_port = Integer.parseInt(splitArg[1]);
		    } else if (splitArg.length == 2 && splitArg[0].equals("--count")) {
		        ping_count = Integer.parseInt(splitArg[1]);
		    } else if (splitArg.length == 2 && splitArg[0].equals("--period")) {
		        period = Long.parseLong(splitArg[1]);
		    } else if (splitArg.length == 2 && splitArg[0].equals("--timeout")) {
		        timeout = Integer.parseInt(splitArg[1]);
		    } else {
		        System.err.println("Usage: java PingServer --port=<port> [--loss_rate=<rate>] [--avg_delay=<delay>]");
		        return;
		    }
		}

		//Open socket
		DatagramSocket socket = new DatagramSocket(default_port);

		//Booleans that help with output
		boolean canWritePing = true;
		boolean originalTime = true;
	
		//Variables that keep track of statistics
		int nSequenceNumber = 0;
		int nTotalSent = 0;
		int nTotalReceived = 0;
		long startTime = 0;
		long endTime = 0;
		long lTotalTime = 0;
		ArrayList<Long> arrayDelays = new ArrayList<Long>();

		while(nSequenceNumber < ping_count) {

			//Wait until set period before continuing
			Thread.sleep(period);

			//initial timestamp
			long timestamp = System.currentTimeMillis();

			//Timestamp for first ping request sent
			if(originalTime) {
				startTime = timestamp;
				originalTime = false;
			}

			//Ping message
			String request = "PING " + nSequenceNumber + " " + timestamp + "\n";
			byte[] bytes = new byte[1024];

			bytes = request.getBytes();

			//try catch block for sending and receiving ping request and response

			try {

				DatagramPacket ping = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(server_ip), server_port);
				socket.send(ping);
				nTotalSent++;

				//Write initial ping + ip_addres to output
				if(canWritePing) {
					System.out.println("PING " + server_ip);
					canWritePing = false;
				}

				//set timeout
				socket.setSoTimeout(timeout);
				
				//receive response and increment total received
				DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
				socket.receive(response);
				nTotalReceived++;

				//time stamp for received packet
				long timestampReceived = System.currentTimeMillis();

				//add RTT to array
				arrayDelays.add(timestampReceived-timestamp);

				//time stamp for last received packet
				if (nSequenceNumber == ping_count -1) {
					endTime = System.currentTimeMillis();
				}

				//print data to output
				printData(response, nSequenceNumber, timestampReceived-timestamp);

			} catch (IOException e) {
				System.out.println("Error processing ping response: " + e.getMessage());		

				//time stamp if last packet is lost
				if (nSequenceNumber == ping_count -1) {
					endTime = System.currentTimeMillis();
				}
			}

			nSequenceNumber++;
		}

		//calculate total time
		lTotalTime = endTime - startTime;

		//print statistics
		printSummary(server_ip, nTotalSent, nTotalReceived, lTotalTime, arrayDelays);
	}

	/**
     * Method prints request data to output
     * @param requst
     * @param sequence
     * @param delay
     * @throws Exception
     */
	private static void printData(DatagramPacket request, int sequence, long delay) throws Exception {

		byte[] bytes = request.getData();

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

		String line = bufferedReader.readLine();

		System.out.println("PONG " + request.getAddress().getHostAddress()
							+ ": seq=" + sequence + " time=" + delay + " ms");
	}

	/**
     * Method prints reponse statistics to output
     * @param ip_address
     * @param totalSent
     * @param totalReceived
     * @param totalTime
     * @param delays
     * @throws Exception
     */
	private static void printSummary(String ip_address, int totalSent, int totalReceived, long totalTime, ArrayList<Long> times) throws Exception {
		
		long sum = 0;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;

		for(Long delay: times) {
			sum = sum + delay;
			
			if (delay < min) {
				min = delay;
			}

			if (delay > max) {
				max = delay;
			}

		}

		long avgRTT = 0;

		if(totalReceived != 0) { avgRTT = sum/totalReceived; }
		if(min == Long.MAX_VALUE) { min = 0; }
		if(max == Long.MIN_VALUE) { max = 0; }

		double dLossRate = (1.0 - ((totalReceived * 1.0)/(totalSent * 1.0))) * 100.0;
		System.out.println("--- " + ip_address + " ping statistics ---" );
		System.out.println(totalSent + " transmitted, " + totalReceived + " received, " + round(dLossRate,1) + "% loss, time " + totalTime + " ms");
		System.out.println("rtt min/avg/max = " + min + "/" + avgRTT + "/" + max + " ms");
	}


	/**
     * Method returns double rounded to a precision level passed as a parameter
     * @param value
     * @param precision
     * @throws Exception
     */
	public static double round(double value, int precision) {
		if (precision < 0) throw new IllegalArgumentException();

		BigDecimal bigDecimal = new BigDecimal(value);
		bigDecimal = bigDecimal.setScale(precision, RoundingMode.HALF_UP);
		return bigDecimal.doubleValue();
	}

}