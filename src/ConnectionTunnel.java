import me.jackyliao.base64.Base64;
import me.jackyliao.websocket.WebSocketClient;
import me.jackyliao.websocket.WebSocketClientProcessor;
import me.jackyliao.websocket.WebSocketListener;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;

public class ConnectionTunnel extends WebSocketListener implements Runnable {

	public static final Charset charset = Charset.forName("UTF-8");

	public Thread readThread;

	public Socket wsSocket;
	public WebSocketClient wsClient;
	public WebSocketClientProcessor wsProcessor;

	public OutputStream output;
	public InputStream input;

	public String connect = null;
	public String password = null;
	public String path = null;
	public int port = 0;

	public ConnectionTunnel(InputStream input, OutputStream output, String dropletId, String cookies, String acceptLanguage, String userAgent) throws IOException {
		this.input = input;
		this.output = output;

		scrape(dropletId, cookies, acceptLanguage, userAgent);
		connectWebsocket(cookies, userAgent);
	}

	public void onText(String data) {
		try {
			output.write(Base64.decode(data));
		} catch(IOException e) {
			try {
				output.close();
				System.exit(0);
			} catch(Exception ex) {
			}
			e.printStackTrace();
		}
	}

	public void onClose() {
		System.out.println("Connection closed");
		try {
			input.close();
			output.close();
		} catch(Exception e) {
		}
	}

	public static String get(String str, String tag) {
		int ind = str.indexOf(tag);
		if(ind == -1)
			return null;
		ind = str.indexOf('\"', ind + 1);
		if(ind == -1)
			return null;
		int nextInd = str.indexOf('\"', ind + 1);
		if(nextInd == -1)
			return null;
		return str.substring(ind + 1, nextInd);
	}

	public void scrape(String dropletId, String cookie, String acceptLanguage, String userAgent) throws IOException {
		URLConnection connection = new URL("https://cloud.digitalocean.com/droplets/" + dropletId + "/console?no_layout=true").openConnection();
		connection.addRequestProperty("Cookie", cookie);
		connection.addRequestProperty("Accept-Language", acceptLanguage);
		connection.addRequestProperty("User-Agent", userAgent);
		connection.connect();
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset));
		String line;
		boolean success = false;
		while((line = reader.readLine()) != null) {
			if(line.contains("id=\"noVNC_screen\"")) {
				connect = get(line, "data-host");
				path = get(line, "data-path");
				password = get(line, "data-password");
				String portStr = get(line, "data-port");
				if(connect == null || path == null || password == null || portStr == null)
					break;
				port = Integer.parseInt(portStr);
				success = true;
			}
		}

		reader.close();

		if(!success) {
			throw new IOException("Failed to parse data, cannot find all required fields");
		}
	}

	public void connectWebsocket(String cookie, String userAgent) throws IOException {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Cookie", cookie);
		headers.put("User-Agent", userAgent);

		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		wsSocket = factory.createSocket();
		wsSocket.connect(new InetSocketAddress(connect, port));
		wsClient = new WebSocketClient(wsSocket.getInputStream(), wsSocket.getOutputStream(), connect + ":" + port, "/" + path, headers);
		wsProcessor = new WebSocketClientProcessor(wsClient, this, true);
		wsProcessor.useMask = true;
		readThread = new Thread(this);
	}

	public void startTunnel() {
		wsProcessor.start();
		readThread.start();
	}

	public void run() {
		try {
			byte[] buffer = new byte[4096];
			int read;
			while((read = input.read(buffer, 0, buffer.length)) != -1) {
				String txt = Base64.encode(buffer, 0, read);
				wsProcessor.sendText(txt);
			}
		} catch(Exception e) {
			e.printStackTrace();
			try {
				input.close();
				output.close();
				wsProcessor.close();
			} catch(IOException ex) {}
		}
	}
}
