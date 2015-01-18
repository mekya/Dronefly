package net.butterflytv.desktop;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.butterflytv.desktop.interfaces.IStatus;
import net.butterflytv.desktop.interfaces.JoystickSpeedListener;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

import de.yadrone.base.ARDrone;
import de.yadrone.base.IARDrone;
import de.yadrone.base.navdata.ControlState;
import de.yadrone.base.navdata.DroneState;
import de.yadrone.base.navdata.StateListener;



public class ArdroneJoystickController {

	private float forwardBackwardValue;
	private float rightLeftValue;
	private float spinLeftRigthValue;
	private boolean upButtonPressed;
	private boolean downButtonPressed;
	private float takeOffLandingValue;


	public static int TAKEOFF_LANDING = 3;  // Slider in extreme 3dpro
	public static int FORWARD_BACKWARD_AXIS = 1;  // Y axis extreme 3dpro
	public static int RIGHT_LEFT_AXIS = 0;  // X axis extreme 3dpro
	public static int ROTATION_AXIS = 2; //

	public static int UP_BUTTON = 4;
	public static int DOWN_BUTTON = 2;


	private Thread joystickThread;
	private Controller controller;
	private ARDrone drone;
	private IStatus droneStatus;
	
	private boolean isEnabled = false;
	private JoystickSpeedListener joystickSpeedListener;
	private int velocityX;
	private int velocityY;
	private int velocitySpin;
	private int velocityZ;
	private boolean takeOffStatus;

	public ArdroneJoystickController(IARDrone drone, IStatus droneStatus, JoystickSpeedListener speedListener) {
		this.drone = (ARDrone) drone;
		this.droneStatus = droneStatus;
		this.joystickSpeedListener = speedListener;
		isJoystickExist();
	}

	public boolean isJoystickExist() {
		boolean isControllerExists = false;
		try {
			if (Controllers.isCreated() == false) {
				Controllers.create();
			}
			
			Controllers.poll();
			
			if (Controllers.getControllerCount() > 0) {
				isControllerExists = true;
				if (joystickThread == null || joystickThread.isAlive() == false)  {
					joystickThread = new Thread() {
						@Override
						public void run() {
							handleJoystickInputs();
						}
					};
					joystickThread.start();
				}
			}
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
		return isControllerExists;
	}
	
	public void setEnabled(boolean enable) {
			isEnabled = enable;
	}

	private void handleJoystickInputs() {
	
		Controllers.poll();

		
		controller = Controllers.getController(0);
		for (int i = 0; i < controller.getAxisCount(); i++) {
			System.out.println(i + ":" + controller.getAxisName(i));
			controller.setDeadZone(i, (float) 0.3);
		}

		for (int i = 0; i < controller.getButtonCount(); i++) {
			System.out.println(i + ":" + controller.getButtonName(i));
		}
		
		int i = 0;

		while (true) {
			controller.poll();

			takeOffLandingValue = controller.getAxisValue(TAKEOFF_LANDING);
			forwardBackwardValue = controller.getAxisValue(FORWARD_BACKWARD_AXIS);
			rightLeftValue = controller.getAxisValue(RIGHT_LEFT_AXIS);
			spinLeftRigthValue = controller.getAxisValue(ROTATION_AXIS);
			upButtonPressed = controller.isButtonPressed(UP_BUTTON);
			downButtonPressed = controller.isButtonPressed(DOWN_BUTTON);

			float speed = 0;
			if (drone != null) {
				speed = drone.getSpeed();
			}
			// minus according to ardrone coordinate system
			velocityX = -(int) (forwardBackwardValue * speed);
			// minus according to ardrone coordinate system
			velocityY = -(int) (rightLeftValue * speed);
			// minus according to ardrone coordinate system
			velocitySpin = -(int) (spinLeftRigthValue * speed);
			velocityZ = 0;
			if (upButtonPressed) {
				// minus according to ardrone coordinate system
				velocityZ = (int) -speed;
			}
			else if (downButtonPressed) {
				velocityZ = (int) speed;
			}


			if (takeOffLandingValue < -0.7) {
				// take off
				takeOffStatus = true;
				if (droneStatus.isFlying() == false && isEnabled) {
					drone.getCommandManager().takeOff().doFor(5000);
				}
			} else if (takeOffLandingValue > 0.7) {
				// landing
				takeOffStatus = false;
				if (droneStatus.isFlying() == true && isEnabled) {
					drone.landing();
				}
			}

			System.out.println("speed X " + velocityX + " speedY " + velocityY + " speedZ " + velocityZ + " speedSpin " + velocitySpin + " take off value "  + takeOffLandingValue);
			if (isEnabled == true) {
				if (velocityX == 0 && velocityY == 0 && velocityZ == 0 && velocitySpin == 0) {
					drone.getCommandManager().hover();
				}
				else {
					drone.getCommandManager().move(velocityX, velocityY, velocityZ, velocitySpin);
				}
			}

			//TODO: special button for hover && emergency
		
			i++;
			if (i>3) {
				i = 0;
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						joystickSpeedListener.speedStatus(velocityX, velocityY, velocityZ, velocitySpin, takeOffStatus);
					}
				});
			}
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
}
