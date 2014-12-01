Dronefly (beta) <a href="https://play.google.com/store/apps/details?id=com.butterfly&hl=en"><img src="http://www.butterflytv.net/wp-content/uploads/2014/08/icon-butterflyTV-150x150.png" width="30"></a>
========
Live Streaming from Ardrone camera to Butterfly TV

You can control Ardrone 2.0 and broadcast live from Ardrone 2.0 camera to Butterfly TV with Dronefly

Butterfly TV is a Live Stream Messaging app that makes you broadcast Live to Contacts or Social Media (Beta)

<a href="https://play.google.com/store/apps/details?id=com.butterfly">
  <img alt="Get it on Google Play" src="https://developer.android.com/images/brand/en_generic_rgb_wo_60.png">
</a>

Installation
-------------
Software is tested in Ubuntu.

* Install ffmpeg (you can use apt-get) and make sure that ffmpeg executable is in your PATH. Because Dronefly directly use ffmpeg via Java Runtime
 
* Check out the project from this repository and run it on Eclipse


Usage
-------
* PC needs two network interfaces. One is wi-fi interface for connecting to Ardrone and other one is any network interface for connecting internet.

* We first connected ubuntu to Ardrone wi-fi hotspot and secondly we connected Android phone(with internet connection) to ubuntu via USB cable and enabled USB tethering. 

![Screenshot of Dronefly](https://github.com/ButterFly-Broadcast/Dronefly/blob/master/Dronefly_Screenshot.png "You can keyboard commands")

* Click "Connect Drone" then "Start Streaming" will be enabled. Write a stream name to text field and click "Start Streaming". You can stop streaming when you hit the same button.

* Right now you can control Ardrone with keyboard commands
    - Take off -> Enter 
    - Landing -> Ctrl + Space
    - Hover -> Space
    - Forward -> Arrow Up
    - Backward -> Arrow Down
    - Left -> Arrow Left
    - Right -> Arrow Right
    - Spin Left -> Shift + Arrow Left
    - Spin Right -> Shift + Arrow Right

* Use software with your own risk. There is no responsibility taken in any situation

Here are two videos one is directly from Ardrone camera 

  
