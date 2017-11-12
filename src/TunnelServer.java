import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class TunnelServer {

	public static String dropletId;
	public static String cookie;
	public static String acceptLanguage;
	public static String userAgent;

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		final File f = new File(".dovnc");
		boolean loaded = false;

		if(f.exists()) {
			System.out.print(f.getName() + " exists. Load? [Y/n] ");
			if(!scanner.nextLine().toLowerCase().trim().equals("n")) {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
					dropletId = reader.readLine();
					cookie = reader.readLine();
					acceptLanguage = reader.readLine();
					userAgent = reader.readLine();
					reader.close();
					loaded = true;
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}

		if(!loaded) {
			System.out.print("Droplet ID: ");
			dropletId = scanner.nextLine();
			System.out.print("Cookie: ");
			cookie = scanner.nextLine();
			System.out.print("Accept-Language: ");
			acceptLanguage = scanner.nextLine();
			System.out.print("User-Agent: ");
			userAgent = scanner.nextLine();

			System.out.print("Write to " + f.getName() + "? [Y/n] ");
			if(!scanner.nextLine().toLowerCase().trim().equals("n")) {
				try {
					FileOutputStream output = new FileOutputStream(f);
					output.write((dropletId + "\n").getBytes(ConnectionTunnel.charset));
					output.write((cookie + "\n").getBytes(ConnectionTunnel.charset));
					output.write((acceptLanguage + "\n").getBytes(ConnectionTunnel.charset));
					output.write((userAgent + "\n").getBytes(ConnectionTunnel.charset));
					output.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.print("Listen port: ");
		int port = scanner.nextInt();

		try {
			ServerSocket socket = new ServerSocket(port);
			while(true) {
				final Socket s = socket.accept();
				System.out.println("Accepted connection from " + s.getRemoteSocketAddress());
				new Thread() {
					public void run() {
						try {
							ConnectionTunnel tunnel = new ConnectionTunnel(s.getInputStream(), s.getOutputStream(), dropletId, cookie, acceptLanguage, userAgent);
							System.out.println("Password is: " + tunnel.password);
							tunnel.startTunnel();
						} catch(IOException e) {
							e.printStackTrace();
						}
					}
				}.start();
			}
		} catch(IOException e) {
			e.printStackTrace();
		}

	}
}
