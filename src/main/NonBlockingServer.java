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
		// java 1.7�� try�� ������ �Ұ�ȣ ���� �ڿ��� �ڵ����� �������ش�. 
		try (Selector selector = Selector.open();// Selector�� ��ϵ� ä�� ��������� �˻��ϰ� ������ �� �ֵ��� ���ش�.
				ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) { // ����ŷ ������ ���� ����, ������ ���� �����ϰ� ��Ʈ�� ���ε���.

			if ((serverSocketChannel.isOpen()) && (selector.isOpen())) { // �� ���ȴ��� Ȯ�� 
				serverSocketChannel.configureBlocking(false);
				serverSocketChannel.bind(new InetSocketAddress(8888));

				serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
				System.out.println("���� �����");

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
				System.out.println("���������� �������� ���߽��ϴ�.");
			}
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}

	/**
	 * ������ �� �޼ҵ�
	 * 
	 * @param key
	 * @param selector
	 * @throws IOException
	 */
	private void accept0P(SelectionKey key, Selector selector) throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverChannel.accept();
		socketChannel.configureBlocking(false);

		System.out.println("Ŭ���̾�Ʈ ����� : " + socketChannel.getRemoteAddress());

		keepDataTrack.put(socketChannel, new ArrayList<byte[]>());
		socketChannel.register(selector, SelectionKey.OP_READ);
	}

	/**
	 * �д� �޼ҵ�
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
				System.err.println("������ �б� ����");
			}

			if (numRead == 1) {
				this.keepDataTrack.remove(socketChannel);
				System.out.println("Ŭ���̾�Ʈ ���� ���� : " + socketChannel.getRemoteAddress());
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
	 * ���� �޼ҵ�
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
