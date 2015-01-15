package net.butterflytv.desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;


public class GoProStreamer {

	private static final String CAMERA_IP = "10.5.5.9";
	private static int PORT = 8554;
	private static DatagramSocket mOutgoingUdpSocket;
	private Process streamingProcess;
	private KeepAliveThread mKeepAliveThread;
	private String streamURL;

	public GoProStreamer() {
		try {
			mOutgoingUdpSocket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void startStreamService() {
		HttpURLConnection localConnection = null;
		try {
			String str = "http://" + CAMERA_IP + "/gp/gpExec?p1=gpStreamA9&c1=restart";
			localConnection = (HttpURLConnection) new URL(str).openConnection();
			localConnection.addRequestProperty("Cache-Control", "no-cache");
			localConnection.setConnectTimeout(5000);
			localConnection.setReadTimeout(5000);
			int i = localConnection.getResponseCode();
			if (i >= 400) {
				throw new IOException("sendGET HTTP error " + i);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		if (localConnection != null) {
			localConnection.disconnect();
		}
	}

	private void sendUdpCommand(int paramInt)throws SocketException, IOException
	{
		Locale localLocale = Locale.US;
		Object[] arrayOfObject = new Object[4];
		arrayOfObject[0] = Integer.valueOf(0);
		arrayOfObject[1] = Integer.valueOf(0);
		arrayOfObject[2] = Integer.valueOf(paramInt);
		arrayOfObject[3] = Double.valueOf(0.0D);
		byte[] arrayOfByte = String.format(localLocale, "_GPHD_:%d:%d:%d:%1f\n", arrayOfObject).getBytes();
		String str = CAMERA_IP;
		int i = PORT;
		DatagramPacket localDatagramPacket = new DatagramPacket(arrayOfByte, arrayOfByte.length, new InetSocketAddress(str, i));
		this.mOutgoingUdpSocket.send(localDatagramPacket);
	}
	
	public void startStreaming() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					streamingProcess = Runtime.getRuntime().exec("ffmpeg -f mpegts -i udp://10.5.5.9:8554 -c:a libfdk_aac -ar 44100 -b:a 128k -vcodec libx264 -r 30 -crf 23 -f flv " + MainForm.SERVER_RTMP + streamURL);


					InputStream errorStream = streamingProcess.getErrorStream();
					byte[] data = new byte[1024];
					int length = 0;
					while ((length = errorStream.read(data, 0, data.length)) > 0) {
						System.out.println(new String(data, 0, length));
						System.out.println(System.currentTimeMillis());
					}
					
				} catch (IOException e) {
					if (streamingProcess != null) {
						streamingProcess = null;
					}
					e.printStackTrace();
				}
			}
		};
		thread.start();
	}
	
	public void keepAlive() {
		mKeepAliveThread = new KeepAliveThread();
		mKeepAliveThread.start();
	}
	
	class KeepAliveThread extends Thread {
		
		public void run() {
			try {
				Thread.currentThread().setName("keepalive");
				if (mOutgoingUdpSocket == null) {
					mOutgoingUdpSocket = new DatagramSocket();
				}
				while ((!Thread.currentThread().isInterrupted()) && (mOutgoingUdpSocket != null)) {
					sendUdpCommand(2);
					Thread.sleep(2500L);
					System.out.println("keep alive udp");
				}
			}
			catch (SocketException e) {
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void stopStreaming() {
		if (streamingProcess != null) {
			streamingProcess.destroy();
			streamingProcess = null;
		}
		stopKeepalive();
		mOutgoingUdpSocket.disconnect();
		mOutgoingUdpSocket.close();
	}
	
	private void stopKeepalive() {
		if (mKeepAliveThread != null) {
			mKeepAliveThread.interrupt();
			try {
				mKeepAliveThread.join(10L);
				mKeepAliveThread = null;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	public boolean isStreaming() {
		return streamingProcess != null;
	}
	
	public boolean registerStream(String name) {
		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		boolean resultBool = false;
		try {
			amfConnection.connect(MainForm.SERVER_HTTP_GATEWAY);
			streamURL = "from_gopro" + (int) (Math.random() * 10000) + String.valueOf(System.currentTimeMillis());
			
			resultBool = (Boolean) amfConnection.call("registerLiveStream", name, streamURL, "ahmetmermerkaya@gmail.com", "ahmetmermerkaya@gmail.com", false, "eng");

		} catch (ClientStatusException e) {
			e.printStackTrace();
			Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, e);
		} catch (ServerStatusException e) {
			e.printStackTrace();
			Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, e);
		}
		amfConnection.close();
		return resultBool;
	}

}
