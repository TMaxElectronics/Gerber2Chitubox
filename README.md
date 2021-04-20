# Gerber2Chitubox
This is a tool that allows you to expose photosensitive PCBs with lcd masked resin printers with a chitubox controller.
It loads gerbers and generates the file that the printer wants including any mirroring, offsets, drill guides etc.

THIS IS STILL ONLY IN THE CONCEPT STAGE! The code is working, but pretty much just hacked together to test out the idea. More updates will follow but use the software at your own risk until now.
It currently only supports the scaling for the elegoo mars (infact only MY printer since I calibrated it with that...) but I am planning to add presets for other printers if this experiment is successful.

# this project is probably already dead...
Though the idea seemed cool and first tests were promising, there are some fundamental limitations to this setup (at least on my printer). There is a 0.5mm piece of glass in front of the LCD that, together with the optical geometry, results in a dramatic reduction in fidelity. Removing this does fix it and I have made qutie a few PCBs with this software since but it is a destructive act. Using the printer for normal prints isn't really posible afterwards and the likelyhood of damaging the display is high. I'm about to make a quick demo video for my channel where I show what I'm talking about here... stay tuned for that if you're interested

# Big thanks to the following projects:
[GerberPlot by wholder](https://github.com/wholder/GerberPlot) ; Heavily modified and used to read the input files.

[PhotonFileValidator by Photonsters](https://github.com/Photonsters/PhotonFileValidator) ; Modified to allow for creation of new files, and to allow layer creation from a pixelbuffer
