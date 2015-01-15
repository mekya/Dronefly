package net.butterflytv.desktop;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

import de.yadrone.apps.controlcenter.YADroneControlCenter;
import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.configuration.ConfigurationListener;
import de.yadrone.base.exception.ARDroneException;
import de.yadrone.base.exception.IExceptionListener;
import de.yadrone.base.navdata.BatteryListener;
import flex.messaging.io.MessageIOConstants;
import flex.messaging.io.amf.client.AMFConnection;
import flex.messaging.io.amf.client.exceptions.ClientStatusException;
import flex.messaging.io.amf.client.exceptions.ServerStatusException;

import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;

public class MainForm implements KeyListener {

	private static final String CONNECT_DRONE = "Connect Drone";
	private static final String DRONE_CONNECTED = "Connected";
	private JFrame frame;
	private IARDrone drone;

	private JLabel lblStatusLabel;
	protected Process streamingProcess;
	private JPanel panel;
	private JButton btnStreamArdroneCamera;
	private JButton btnConnectDrone;
	
	GoProStreamer goProStreamer = new GoProStreamer();


	public static final String SERVER_ADDR = "54.229.106.62";
	public static final String SERVER_HTTP_GATEWAY = "http://" + SERVER_ADDR + ":5080/ButterFly_Red5/gateway";
	public static final String SERVER_RTMP = "rtmp://" + SERVER_ADDR + "/ButterFly_Red5/";
	private JTextField txtStreamname;

	enum DroneCommand {
		NONE, DRONE_UP,	DRONE_DOWN, DRONE_LEFT, DRONE_RIGHT, DRONE_FORWARD, DRONE_BACKWARD, 
		DRONE_SPIN_LEFT, DRONE_SPIN_RIGHT, DRONE_TAKE_OFF, DRONE_LANDING, DRONE_HOVER
	}

	DroneCommand command = DroneCommand.NONE;
	private Thread droneCommandThread;
	private boolean droneCommandStopReq = false;
	private JTextField txtStreamname4GoPro;
	private JButton btnStreamGoPro;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainForm window = new MainForm();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainForm() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 350);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Dronefly for Butterfly TV(Beta)");
		frame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent arg0) {}

			@Override
			public void windowIconified(WindowEvent arg0) {}

			@Override
			public void windowDeiconified(WindowEvent arg0) {}

			@Override
			public void windowDeactivated(WindowEvent arg0) {}

			@Override
			public void windowClosing(WindowEvent arg0) {
				
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				if (droneCommandThread.isAlive()) {
					droneCommandStopReq = true;
				}
			}

			@Override
			public void windowActivated(WindowEvent arg0) {	}
		});
		panel = new JPanel();
		panel.setBounds(0, 0, 450, 320);
		panel.addKeyListener(this);
		panel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent arg0) {
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				panel.requestFocusInWindow();
			}
			@Override
			public void mouseExited(MouseEvent arg0) {}
			@Override
			public void mouseEntered(MouseEvent arg0) {}
			@Override
			public void mouseClicked(MouseEvent arg0) {}
		});
		panel.setFocusable(true);
		//frame.getContentPane().add(panel);
		panel.setLayout(null);

		JLabel lblXValue = new JLabel("Left/Right/Forward/Backward: Arrow keys");
		lblXValue.setBounds(12, 64, 350, 15);
		panel.add(lblXValue);

		JLabel lblYValue = new JLabel("Spin (Left/Right): Shift + (Left Arrow/Right Arrow)");
		lblYValue.setBounds(12, 89, 350, 15);
		panel.add(lblYValue);

		JLabel lblUp = new JLabel("Up/Down: W/S");
		lblUp.setBounds(12, 113, 200, 15);
		panel.add(lblUp);

		JLabel lblTakeofflanding = new JLabel("Takeoff/Landing: Enter/Ctrl + Space");
		lblTakeofflanding.setBounds(12, 140, 350, 15);
		panel.add(lblTakeofflanding);

		JLabel emergencyResetLabel = new JLabel("Emergency/Reset: Ctrl + E/R");
		emergencyResetLabel.setBounds(12, 167, 350, 15);
		panel.add(emergencyResetLabel);

		btnConnectDrone = new JButton(CONNECT_DRONE);
		btnConnectDrone.setBounds(147, 12, 148, 25);
		btnConnectDrone.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				startDrone();
			}
		});
		panel.add(btnConnectDrone);

		btnStreamArdroneCamera = new JButton("Start Streaming");
		btnStreamArdroneCamera.setBounds(147, 241, 157, 25);
		btnStreamArdroneCamera.setEnabled(false);
		btnStreamArdroneCamera.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (streamingProcess == null) {
					streamToServer();
					btnStreamArdroneCamera.setText("Stop Streaming");
				}
				else {
					stopStreaming();
					btnStreamArdroneCamera.setText("Start Streaming");
				}
			}
		});
		panel.add(btnStreamArdroneCamera);

		lblStatusLabel = new JLabel("");
		lblStatusLabel.setBounds(157, 184, 150, 15);
		panel.add(lblStatusLabel);

		txtStreamname = new JTextField();
		txtStreamname.setBounds(150, 211, 257, 18);
		panel.add(txtStreamname);
		txtStreamname.setColumns(10);

		JLabel lblStreamName = new JLabel("Stream Name:");
		lblStreamName.setBounds(12, 212, 133, 15);
		panel.add(lblStreamName);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(0, 0, 450, 320);
		tabbedPane.addTab("Ardrone 2.0", panel);
		
		JPanel goProPanel = new JPanel();
		goProPanel.setLayout(null);
		goProPanel.setBounds(0, 0, 450, 320);
		
		
		lblStreamName = new JLabel("Stream Name:");
		lblStreamName.setBounds(12, 22, 133, 15);
		goProPanel.add(lblStreamName);
		
		txtStreamname4GoPro = new JTextField();
		txtStreamname4GoPro.setBounds(150, 22, 257, 18);
		goProPanel.add(txtStreamname4GoPro);
		txtStreamname4GoPro.setColumns(10);
		
		btnStreamGoPro = new JButton("Start Streaming");
		btnStreamGoPro.setBounds(147, 61, 157, 25);
		goProPanel.add(btnStreamGoPro);
		
		btnStreamGoPro.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (goProStreamer.isStreaming() == false) {
					if (goProStreamer.registerStream(txtStreamname4GoPro.getText())) {
						goProStreamer.startStreamService();
						goProStreamer.startStreaming();
						goProStreamer.keepAlive();
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (goProStreamer.isStreaming() == true) {
							btnStreamGoPro.setText("Stop Streaming");
						}
						else {
							JOptionPane.showMessageDialog(null, "GoPro streaming could not be started");
						}
					}
					else {
						JOptionPane.showMessageDialog(null, "Stream registration is unsuccessfull");
					}
				}
				else {
					goProStreamer.stopStreaming();
					btnStreamGoPro.setText("Start Streaming");
				}
				
			}
		});
		
		tabbedPane.addTab("GoPro4", goProPanel);
		
		frame.getContentPane().add(tabbedPane);
		
		

	}

	public void startDrone() {
		if (drone == null) {
			try {
				drone = new ARDrone("192.168.1.1", null);
				drone.getCommandManager().start();
				drone.getNavDataManager().addBatteryListener(new BatteryListener() {
					@Override
					public void voltageChanged(int arg0) {
					}
					@Override
					public void batteryLevelChanged(int percent) {
						lblStatusLabel.setText("Battery %" + percent);
					}
				});
				drone.getConfigurationManager().getConfiguration(new ConfigurationListener() {
					@Override
					public void result(String configuration) {
						System.out.println(configuration);
					}
				});
				drone.addExceptionListener(new IExceptionListener() {
					@Override
					public void exeptionOccurred(ARDroneException ex) {
						ex.printStackTrace();
						System.out.println("drone exception listener");
					}
				});
				btnStreamArdroneCamera.setEnabled(true);
				btnConnectDrone.setText(DRONE_CONNECTED);
				btnConnectDrone.setEnabled(false);
				startCommandThread();

			} catch (Exception ex) {
				JOptionPane.showMessageDialog(null, "Ardrone connection isn't established");
				ex.printStackTrace();
			}
		}
		else {
			drone.getCommandManager().stop();
		}
	}

	private void runTask(Runnable task) {
		SwingUtilities.invokeLater(task);
	}

	private void stopStreaming() {
		if (streamingProcess != null )
		{
			streamingProcess.destroy();
			streamingProcess = null;
		}
	}
	
	public void streamToServer() {
		AMFConnection amfConnection = new AMFConnection();
		amfConnection.setObjectEncoding(MessageIOConstants.AMF0);
		try {
			amfConnection.connect(SERVER_HTTP_GATEWAY);
			final String streamURL = "from_drone" + (int) (Math.random() * 10000) + String.valueOf(System.currentTimeMillis());
			lblStatusLabel.setText("registering stream ");

			boolean resultBool = (Boolean) amfConnection.call("registerLiveStream", txtStreamname.getText(), streamURL, "ahmetmermerkaya@gmail.com", "ahmetmermerkaya@gmail.com", true, "eng");

			if (resultBool == true) {
				lblStatusLabel.setText("starting thread");

				new Thread() {

					@Override
					public void run() {
						try {
							// ffmpeg  -f v4l2 -i /dev/video1 -f alsa -ar 48000 -ac 2 -i hw:0 -c:a libfdk_aac -ar 44100 -b:a 128k -pix_fmt yuv420p -vcodec libx264 -crf 27 -f flv " + SERVER_RTMP + streamURL
							streamingProcess = Runtime.getRuntime().exec("ffmpeg -i tcp://192.168.1.1:5555 -f alsa -ar 48000 -ac 2 -i hw:0 -c:a libfdk_aac -ar 44100 -b:a 128k -vcodec libx264 -r 30 -crf 27 -f flv " + SERVER_RTMP + streamURL);

							InputStream errorStream = streamingProcess.getErrorStream();
							byte[] data = new byte[1024];
							int length = 0;
							while ((length = errorStream.read(data, 0, data.length)) > 0) {
								System.out.println(new String(data, 0, length));
								System.out.println(System.currentTimeMillis());
							}
						} catch (IOException ex) {
							Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
						} 
					}
				}.start();

			} else {
				lblStatusLabel.setText("Registration unsuccessfull");
			}

		} catch (ClientStatusException ex) {
			Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ServerStatusException ex) {
			Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
		}

		amfConnection.close();

	}

	@Override
	public void keyPressed(KeyEvent event) {
		switch (event.getKeyCode())
		{
		case KeyEvent.VK_UP:
			drone.forward();
			System.out.println("forward");
			break;
		case KeyEvent.VK_DOWN:
			drone.backward();
			System.out.println("backward");
			break;
		case KeyEvent.VK_LEFT:
			if (event.isShiftDown()) {
				drone.spinLeft();
				System.out.println("spin left");
			}
			else {
				drone.goLeft();
				System.out.println("LEFT");
			}
			break;
		case KeyEvent.VK_RIGHT:
			if (event.isShiftDown()) {
				drone.spinRight();
				System.out.println("spin right");
			}
			else {
				drone.goRight();
				System.out.println("RIGHT");
			}
			break;
		case KeyEvent.VK_W:
			drone.up();
			System.out.println("up");
			break;
		case KeyEvent.VK_S:
			drone.down();
			System.out.println("down");
			break;
		case KeyEvent.VK_ENTER:
			drone.takeOff();
			System.out.println("take off");
			break;
		case KeyEvent.VK_SPACE:
			if (event.isControlDown()) {
				drone.landing();
			}
			else {
				drone.hover();
			}
			System.out.println("landing");
			break;
		case KeyEvent.VK_E:
			if (event.isControlDown()) {
				drone.getCommandManager().emergency();
			}
			break;
		case KeyEvent.VK_R:
			if (event.isControlDown()) {
				drone.reset();
			}
		case KeyEvent.VK_T:
			drone.toggleCamera();
		default:
			System.out.println(event.getKeyCode());
		}
	}

	@Override
	public void keyReleased(KeyEvent event) {
		switch (event.getKeyCode())
		{
		case KeyEvent.VK_UP:
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_A:
		case KeyEvent.VK_W:
		case KeyEvent.VK_S:
		case KeyEvent.VK_D:
		
			command = DroneCommand.DRONE_HOVER;
			break;
		case KeyEvent.VK_SPACE:
		case KeyEvent.VK_ENTER:
			break;
		default:
			System.out.println(event.getKeyCode());
		}

	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	public void startCommandThread() {
		droneCommandThread = new Thread() {
			@Override
			public void run() {
				while (true) {
					synchronized (command) {

						switch (command) 
						{
						case DRONE_DOWN:
							drone.down();
							break;
						case DRONE_UP:
							drone.up();
							break;
						case DRONE_LEFT:
							drone.goLeft();
							break;
						case DRONE_RIGHT:
							drone.goRight();
							break;
						case DRONE_FORWARD:
							drone.forward();
							break;
						case DRONE_BACKWARD:
							drone.backward();
							break;
						case DRONE_SPIN_LEFT:
							drone.spinLeft();
							break;
						case DRONE_SPIN_RIGHT:
							drone.spinRight();
							break;
						case DRONE_TAKE_OFF:
							drone.takeOff();
							break;
						case DRONE_LANDING:
							drone.landing();
							break;
						case DRONE_HOVER:
							drone.hover();
							break;
						case NONE:
							break;
						}
					}
					
					if (droneCommandStopReq == true) {
						break;
					}
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		droneCommandThread.start();

	}
}
