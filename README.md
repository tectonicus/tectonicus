Tectonicus
==========
Tectonicus generates a 3D model of your Minecraft Java world and uses that to create zoomable maps with a high level of detail.
It is highly configurable and is focused on creating maps that look very close to what you see in Minecraft.

![Minecraft Castle](/SourceData/FloatingCastle.png)

## Example Map
[View the example map](http://map.tectonicus.org/) (this was rendered using an older version of Tectonicus)

Usage
-----------
1. [Download the latest version of Tectonicus](https://github.com/tectonicus/tectonicus/releases)
2. [Create an XML configuration file](https://github.com/tectonicus/tectonicus/wiki/Creating-a-Tectonicus-config-file)
3. Run Tectonicus from the command line: `java -jar Tectonicus-2.30.1.jar -c myconfig.xml`

If you experience out of memory errors you may need to increase the memory available to the Java VM:
`java -Xmx6G -jar Tectonicus-2.30.1.jar config=myconfig.xml`
6G = 6 gigabytes of RAM (or however much RAM you can afford to use)

Tectonicus generates a large amount of images which can use a significant amount of hard drive space (gigabytes) depending on your world size. This can be reduced by setting imageFormat to webp or jpg, and also by utilising closestZoomSize in the Map node.

While the first Tectonicus run can be time-consuming, subsequent runs with the same options will be significantly quicker due to Tectonicus only rendering changed tiles.

## Default config file
If Tectonicus is run without any command line arguments it will search for either 'tectonicus.xml' or 'tectonicusConfig.xml' in the same directory and try to use one of those as it's configuration file.

## Special Signs

By default, Tectonicus displays every sign your players place.  To stop your map being completely overrun you can use filter="special".

'Special' signs are those that begin and end with one of these characters:
```
- = ~ !
```

So the following signs would show up on the map:
```
-----------
someone's
house
----------
```
```
! my place! !
```
```
~ long
  sign
  on four
  lines ~
```

## Creating Views

Tectonicus can generate first person views and insert them into your map so you can showcase your buildings or discoveries.

![Example View](/Docs/ExampleView.png)

These are done by placing signs with text on them to label them as views, like so:

```
#view
Your view description
```

The text '#view' can be placed anywhere on the sign. Lines without a preceding '#' will be used as the description for the view.

Views are generated from the direction the sign is facing. You can also adjust the height, angle, and FOV:
```
#view h10 a135 f90
Your view description
```

Here there are three values: 'h10' shifts the view up by 10 Minecraft blocks. This can be useful to get a birds eye view of a structure. It also lets you bury signs underground out of sight. Negative numbers  also work here.
The 'a135' sets the elevation angle (how much the view looks up or down) to 135 degrees. This can be any number from 0 (looking straight up) to 180 (looking straight down).
The 'f90' sets the FOV to 90 (the default is 70, the same as Minecraft).

You can also change the view to be drawn at nighttime:
```
#view night
Description here
```

View parameters can be placed on any line, but the line must start with the '#' character.
```
#view h10 a135
#night
Description here
#f90
```

## Logging

Tectonicus generates a log file that is overwritten each time you run the program. By default, the log is located in log/tectonicus.log in the current working directory.
To change the log directory and/or append to the logfile instead of overwriting it use the system properties tectonicus.logDir and tectonicus.logAppend respectively: 
`java -Dtectonicus.logDir=newLogDir -Dtectonicus.logAppend=true -jar Tectonicus-2.30.1.jar`
If appending is enabled, by default the log file will roll over at the start of every month saving previous month's log files for six months or until the total log file size reaches 200MB.

You can provide your own custom [logback configuration](https://logback.qos.ch/manual/configuration.html):
`java -Dlogback.configurationFile=logbackConfig.xml -jar Tectonicus-2.30.1.jar`
Make sure to name your custom config file something other than logback.xml or it will not work.
