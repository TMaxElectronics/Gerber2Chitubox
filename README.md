# Gerber2Chitubox
This is a tool that allows you to expose photosensitive PCBs with lcd masked resin printers with a chitubox controller.
It loads gerbers and generates the file that the printer wants including any mirroring, offsets, drill guides etc.

THIS IS STILL ONLY IN THE CONCEPT STAGE! The code is working, but pretty much just hacked together to test out the idea. More updates will follow but use the software at your own risk until now.
It currently only supports the scaling for the elegoo mars (infact only MY printer since I calibrated it with that...), I'll add support for custom printer presets at some point.

# IT'S ALIIIVE :D
After initial testing suggested this project was dead, I spent some more time and who would have thought... it actually does work out very well :)

The G2C.jar binary a runnable jar file of the software if you want to give it a shot :)

# Required printer modifications
By default the printers lcd has a glass pane infront of it for protection and likely flatness too. This however reduces exposure accuracy a lot (more like 0.4mm/15mil process) because of the lamp geometry. To fix this you will have to remove the peice of glass. 

**DO THIS AT YOUR OWN RISK, YOU CAN VERY EASILY RUIN YOUR PRINTER WITH THIS**

First (carefully!) remove the display assembly from the printer. unfortunately it is glued in quite well (at least it was in my printer) which made it impossible to do without damage.You can buy new ones on amazon for ~20€ or other places though so don't sweat it if you break yours (I broke two trying this :P).

Once you have the screen out you can start seperating the glass pane. The two parts are glued together very well so put a drop or two of isopropanol in between the actual lcd and the glass, then slowly and carefully start seperating the screen with a plastic spudger. It shouldn't take much force at all, if it does add a bit more IPA.

Then install only the LCD in the printer and put the thinnest non-transparent tape you can find around the outside of it so it looks like this:  

[image](https://no_link_yet)

This is to prevent the massive light leakage around the display exposing the pcb where it shouldn't.

I also recommend making some alignment markers like you see in the picture. The ones I made register in the space for the resin tank to make using them repeatable.
This is less critical fo doing single layer exposures, but absolutely essential for more than that. You can make the "alignmen test" pcb to measure the offset you have between top and bottom layer for example, and then compensate for that during the file export.
I'll show how to do this in a video at some point if I find the time.

**Keep in mind that the printer cannot be used for 3d printing like this**. If you want to go back place the glass pane back on the display, and tape it down around the outside a little. Make sure to als re-level the build plate. It is probably a good idea to clean the glue off the back too, but I haven't found a solvent that works for that yet.

# Performance
I'm running an old V1 Elegoo mars, with the hack described above. Tested with Bungard pre-sensitised base material at 250s exposure time.

min. copper spacing: 	0.1mm (4mil)

min. trace width: 		0.15mm (6mil) (with some better exposure adjustment 0.1mm is likely possible)

Convenience wise it is good too. I can go from gerber export to pcb in hand (without soldermask, single layer) in ~30min, which took me more like 1h or more before.

# fix for gerbers with arcs
At the moment the gerber loader still can't load arcs correctly all the time (tested with altium).
You can go around this issue by un-selecting "use arcs" upon file export.

# Big thanks to the following projects:
[GerberPlot by wholder](https://github.com/wholder/GerberPlot) ; Heavily modified and used to read the input files. I'll contribute back to the original project once my modifications are proven

[PhotonFileValidator by Photonsters](https://github.com/Photonsters/PhotonFileValidator) ; Modified to allow for creation of new files, and to allow layer creation from a pixelbuffer
