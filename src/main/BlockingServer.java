package main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class BlockingServer {
	public static void main(String[] args) throws Exception {
		BlockingServer server = new BlockingServer();
		server.run();
	}

	private void run() throws IOException {
		ServerSocket servers = new ServerSocket(8888);
		System.out.println("���Ӵ����");
	
		while (true) {
			Socket sock = servers.accept();
			System.out.println("Ŭ���̾�Ʈ �����");
			
			OutputStream out = sock.getOutputStream();
			InputStream in = sock.getInputStream();
			
			while (true) {
				try {
					int request = in.read();
					out.write(request);
				} catch (IOException e) {
					// TODO: handle exception
					break;
				}
				
			}
			
		}
	
	}

}
