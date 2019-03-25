package main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NonBlockingServer {
	private Map<SocketChannel, List<byte[]>> keepDataTrack = new HashMap<>();
	private ByteBuffer buffer = ByteBuffer.allocate(2 * 1024);

	private void startEchoServer() {
		// java 1.7은 try가 끝나면 소괄호 안의 자원을 자동으로 해제해준다. 
		try (Selector selector = Selector.open();// Selector는 등록된 채널 변경사항을 검사하고 접근할 수 있도록 해준다.
				ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) { // 논블로킹 서버의 소켓 생성, 소켓을 먼저 생성하고 포트를 바인딩함.

			if ((serverSocketChannel.isOpen()) && (selector.isOpen())) { // 잘 열렸는지 확인 
				serverSocketChannel.configureBlocking(false);
				serverSocketChannel.bind(new InetSocketAddress(8888));

				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
				System.out.println("접속 대기중");

				while (true) {
					
					selector.select();
					Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

					while (keys.hasNext()) {
						SelectionKey key = (SelectionKey) keys.next();
						keys.remove();

						if (!key.isValid()) {
							continue;
						}

						if (key.isAcceptable()) {
							this.accept0P(key, selector);

						} else if (key.isReadable()) {
							this.read0P(key);
						} else if (key.isWritable()) {
							this.write0P(key);
						}
					}

				}

			} else {
				System.out.println("서버소켓을 생성하지 못했습니다.");
			}
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}

	/**
	 * 접근할 때 메소드
	 * 
	 * @param key
	 * @param selector
	 * @throws IOException
	 */
	private void accept0P(SelectionKey key, Selector selector) throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverChannel.accept();
		socketChannel.configureBlocking(false);

		System.out.println("클라이언트 연결됨 : " + socketChannel.getRemoteAddress());

		keepDataTrack.put(socketChannel, new ArrayList<byte[]>());
		socketChannel.register(selector, SelectionKey.OP_READ);
	}

	/**
	 * 읽는 메소드
	 * 
	 * @param key
	 * @throws IOException
	 */
	private void read0P(SelectionKey key) throws IOException {
		try {
			SocketChannel socketChannel = (SocketChannel) key.channel();
			buffer.clear();
			int numRead = -1;
			try {
				numRead = socketChannel.read(buffer);
			} catch (Exception e) {
				System.err.println("데이터 읽기 에러");
			}

			if (numRead == 1) {
				this.keepDataTrack.remove(socketChannel);
				System.out.println("클라이언트 연결 종료 : " + socketChannel.getRemoteAddress());
				socketChannel.close();
				key.cancel();
				return;
			}

			byte[] data = new byte[numRead];
			System.arraycopy(buffer.array(), 0, data, 0, numRead);
			System.out.println(new String(data, "UTF-8") + " from " + socketChannel.getRemoteAddress());
			doEchoJob(key, data);

		} catch (Exception e) {
			// TODO: handle exception
			System.err.println(e);
		}
	}

	/**
	 * 쓰는 메소드
	 * 
	 * @param key
	 * @throws IOException
	 */
	private void write0P(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		List<byte[]> channelData = keepDataTrack.get(socketChannel);
		Iterator<byte[]> its = channelData.iterator();
		
		while (its.hasNext()) {
			byte[] it = its.next();
			its.remove();
			socketChannel.write(ByteBuffer.wrap(it));
		}
		
		key.interestOps(SelectionKey.OP_READ);
		
		
	}

	private void doEchoJob(SelectionKey key, byte[] data) {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		List<byte[]> ChannelData = keepDataTrack.get(socketChannel);
		ChannelData.add(data);
		
		key.interestOps(SelectionKey.OP_WRITE);
	}
	
	public static void main(String[] args) {
		NonBlockingServer main = new NonBlockingServer();
		main.startEchoServer();
	}
}