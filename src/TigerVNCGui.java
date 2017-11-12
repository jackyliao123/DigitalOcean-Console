import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class TigerVNCGui {

	public static void main(String[] args) throws Exception {

		final File f = new File(".dovnc");

		JFrame frame = new JFrame("DigitalOcean Console");
		JPanel panel = new JPanel();

		GridBagConstraints c = new GridBagConstraints();
		panel.setLayout(new GridBagLayout());
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		panel.add(new JLabel("Link to Console", JLabel.RIGHT), c);
		c.gridy = 2;
		panel.add(new JLabel("Cookie", JLabel.RIGHT), c);
		c.gridy = 3;
		panel.add(new JLabel("Accept-Language", JLabel.RIGHT), c);
		c.gridy = 4;
		panel.add(new JLabel("User-Agent", JLabel.RIGHT), c);

		c.weightx = 1;
		c.gridx = 2;
		c.gridy = 1;
		c.insets = new Insets(0, 10, 0, 0);
		final JTextField tlink = new JTextField();
		panel.add(tlink, c);
		c.gridy = 2;
		final JTextField tcookie = new JTextField();
		panel.add(tcookie, c);
		c.gridy = 3;
		final JTextField taccept = new JTextField();
		panel.add(taccept, c);
		c.gridy = 4;
		final JTextField tuser = new JTextField();
		panel.add(tuser, c);

		try {
			if(f.exists()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				tlink.setText(reader.readLine());
				tcookie.setText(reader.readLine());
				taccept.setText(reader.readLine());
				tuser.setText(reader.readLine());
				reader.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

		final Object wait = new Object();

		JButton save = new JButton("Save");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				try {
					FileOutputStream output = new FileOutputStream(f);
					output.write((tlink.getText() + "\n").getBytes(ConnectionTunnel.charset));
					output.write((tcookie.getText() + "\n").getBytes(ConnectionTunnel.charset));
					output.write((taccept.getText() + "\n").getBytes(ConnectionTunnel.charset));
					output.write((tuser.getText() + "\n").getBytes(ConnectionTunnel.charset));
					output.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		});

		JButton button = new JButton("Connect");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				synchronized(wait) {
					wait.notify();
				}
			}
		});

		JPanel buttons = new JPanel();
		buttons.setLayout(new GridLayout(1, 2));
		buttons.add(save);
		buttons.add(button);
		frame.add(buttons, BorderLayout.SOUTH);

		frame.add(panel);
		frame.setSize(600, 200);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		synchronized(wait) {
			try {
				wait.wait();
			} catch(InterruptedException e) {
			}
		}

		frame.dispose();

		String link = tlink.getText();
		String cookie = tcookie.getText();
		String acceptLanguage = taccept.getText();
		String userAgent = tuser.getText();

		ServerSocket ss = new ServerSocket(0);
		System.out.println("Listening on " + ss.getLocalPort());

		ConnectionTunnel tunnel = new ConnectionTunnel(null, null, link, cookie, acceptLanguage, userAgent);

		Process p1 = Runtime.getRuntime().exec(new String[]{"/usr/bin/vncpasswd", "-f"});
		p1.getOutputStream().write(tunnel.password.getBytes(ConnectionTunnel.charset));
		p1.getOutputStream().close();
		InputStream input = p1.getInputStream();

		ProcessBuilder builder = new ProcessBuilder("/usr/bin/vncviewer", "localhost:" + ss.getLocalPort(), "-passwd", "/dev/stdin");
		Process p2 = builder.start();
		OutputStream p2out = p2.getOutputStream();
		int read;
		byte[] buffer = new byte[4096];
		while((read = input.read(buffer, 0, buffer.length)) != -1) {
			p2out.write(buffer, 0, read);
		}
		p2out.close();
		System.out.println("Started process");

		Socket s = ss.accept();
		System.out.println("Accepted");

		tunnel.input = s.getInputStream();
		tunnel.output = s.getOutputStream();
		tunnel.startTunnel();

		System.out.println("Started");

	}
}
