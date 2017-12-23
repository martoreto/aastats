# aa-stats

This is a more elaborate and quite functional sample app for Android Auto SDK,
featuring reading telemetry data from the car, using the Vendor Extensions channel.

It can:

* display power, torque, boost, gear, oil temp., gearbox temp., battery voltage in this layout:

  ![screenshot](media/screenshot1.png)
  
  (of course it's not hard to change!)
  
* log all the available telemetry to storage in JSON format,

* upload logs to Google BigQuery,

* notify when oil reaches operating temperature,

* beep when maneuvering and the steering wheel crosses zero angle.

## Requirements

1. You need to build it yourself. ;)

   * You also need to rename the application package name (in ``app/build.gradle``) and [obtain ``google-services.json``](https://developers.google.com/mobile/add), and put it in ``app/``.

1. And also install the appropriate aa-vex-* .apk for your car brand (which means initially only VAG cars are supported).
