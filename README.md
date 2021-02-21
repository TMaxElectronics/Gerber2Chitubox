# Gerber2Chitubox
This is a tool that allows you to expose photosensitive PCBs with lcd masked resin printers with a chitubox controller.
It loads gerbers and generates the file that the printer wants including any mirroring, offsets, drill guides etc.

THIS IS STILL ONLY IN THE CONCEPT STAGE! The code is working, but pretty much just hacked together to test out the idea. More updates will follow but use the software at your own risk until now.
It currently only supports the scaling for the elegoo mars (infact only MY printer since I calibrated it with that...) but I am planning to add presets for other printers if this experiment is successful.

# Big thanks to the following projects:
[GerberPlot by wholder](https://github.com/wholder/GerberPlot) ; Heavily modified and used to read the input files.

[PhotonFileValidator by Photonsters](https://github.com/Photonsters/PhotonFileValidator) ; Modified to allow for creation of new files, and to allow layer creation from a pixelbuffer
