Convert any Android (5.0 or higher) tablet into (almost) 24x7 Crestron Panel

CrestronKeeper starts/restarts/makes foreground Crestron App. Two major use cases for the application are:
-	starting Crestron App on boot/screen unlock
-	running an external App (e.g. Sonos) and making sure the control goes back to Crestron App after specified number of seconds

The following functions are supported so far:
-	Start Crestron App when Android boots up
-	Start/switch to Crestron App when the device is unlocked (screen is turned on)
-	Web server on port 9877 takes a couple of commands. Using dynamic icon with the image file controlled by a serial join, Crestron controller can effectively control the behavior of the Crestron App. The “image” that you try to load in a 1x1 pixel dynamic icon can be one f the following:
     o	http://127.0.0.1:1988/ping/xxx/whatever.png waits for xxx seconds and then switches device to the Crestron App
     o	http://127.0.0.1:1988/reset/whatever.png effectively restarts Crestron App (makes it background/kills background/starts)
     o	Other none empty request starts Crestron App and  makes it foreground
     This is how I set up wall mounted panel/Crestron App to run 24x7:
-	Run Crestron full screen
-	Check keep screen on checkbox
-	In my experience the panel would hang after 0x80000000 milliseconds if you do not restart it. Therefore
     o	(tested and works on my panels) Restart panel at least once a week by using internal schedule (not possible on all the models)
     o	(untested) Using text console, execute the “projectrefresh on” command on the Crestron App panel. In theory this will force the application to refresh the data once a week at around midnight
     o	(if Android does not allow scheduled reboots) make sure controller resets the application say nightly (by sending http://127.0.0.1:1988/reset/whatever.png )

Using the application:
Install apk. This will automatically start Crestron App. Switch to the CrestronKeeper window and turn on the ‘draw on top’ permission.
