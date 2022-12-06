Convert any Android (5.0 or higher) tablet into (almost) 24x7 Crestron Panel. No root is required (Important)

CrestronKeeper starts/restarts/makes foreground Crestron App. Two major use cases for the application are:
-	starting Crestron App on boot/screen unlock
-	running an external App (e.g. Sonos) and making sure the control goes back to Crestron App after specified number of seconds

The following functions are supported:
- Start Crestron App when Android boots up
- Start/switch to Crestron App when the device is unlocked (screen is turned on)
- Web server on port 9877 takes a couple of commands. Using dynamic icon with the image file controlled by a serial join, controller can effectively control the behavior of the panel. The “image” that you try to load in a 1x1 pixel dynamic icon can be one of the following:
     -	http://127.0.0.1:9877/ping/xxx/whatever.png waits for xxx seconds and then switches device to the Crestron App
     -	http://127.0.0.1:9877/reset/whatever.png effectively restarts Crestron App (makes it background/kills background/starts)
     -	Other none empty request starts Crestron App and  makes it foreground
     
This is how I set up wall mounted panel/Crestron App to run 24x7:
- Run Crestron full screen
- Check "Auto Reconnect" and "Keep Screen Awake"
- Set turn off the screen after 5 min in Android Settings
- In my experience Crestron App hangs after 0x80000000 milliseconds (a little more than 24 days) if runs without restart. Therefore
     - (probably the best solution if supported by panel) Restart panel at least once a week by using internal schedule (not possible on some models)
     - (untested) Using text console, execute the “projectrefresh on” command on the Crestron App panel and hope the Crestron App will reload itself once a week at around midnight
     - (the above options do not work) make sure controller resets the application nightly (by sending http://127.0.0.1:9877/reset/dummy.png )

Using the application:
Install apk. This will automatically start Crestron App. Switch to the CrestronKeeper window and turn on the ‘Display over other apps’ permission.

Use on your own risk - this is my first app in Kotlin (like it better than Java)