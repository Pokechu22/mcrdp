# mcrdp

An experimental Minecraft mod that displays a single RDP connection while in-game.

And by "experimental", I mean **experimental** - this is very unstable.  It's more a toy project than anything else, but it's interesting.

Powered by [properjavardp](http://properjavardp.sourceforge.net/) (specifically, [a tweaked version](https://github.com/Pokechu22/properjavardp)).  Most of the graphical artifacts are from it; still trying to figure those out.

## Building and running

Right now, the build process isn't great.

1. Download my tweaked version of [properJavaRDP](https://github.com/Pokechu22/properjavardp).
2. Run `gradlew install` inside of properJavaRDP, to add it to your local maven repository.
3. Open `src/main/java/mcrdp/LiteModMcRdp.java`.  Change the options at the top (`server`, `username`, `width`, and `height`) to values you want - the defaults will not work for you.  (Right now, these aren't changable in game - that will change).
4. Run `gradlew runClient` inside of mcrdp.  ForgeGradle will spend a while setting up the workspace, and then the development launcher will start (you can choose "cancel" to enter offline mode).
5. As Minecraft launches, a _separate_ properJavaRDP will open in the background - this is the only way to interact with RDP currently.  You can alt-tab to that window to type or click on the connected computer.
6. See usage below for setting up an RDP display ingame.

This project currently can't easily be installed as a standalone mod; it's only reasonable to use it in the development environment.  This will be changed in the future.

## Usage

A sign containing the text `mcrdp` on the first line will activate a display.  The second line can contain the width and height in blocks, such as `1x1` (if not specified, `8x6` is assumed).

## TODO

* Allow setting the server and username ingame
* Hide the RDP window when not needed
* Some kind of ingame interaction with the computer instead of using a second window
* Send info from the MC world to the connected system (with redstone?) - may not be possible
* Bundle dependencies, so that it can be used as an actual mod
* Allow multiple different RDP servers at once (needs major properJavaRDP changes)
* Fix graphical artifacts (properJavaRDP)
* Fix properJavaRDP crashes due to `IndexOutOfBoundsException`s (properJavaRDP)
* Sound?  (properJavaRDP)

-----

![](demo.png)

_Demo connected to my raspberry pi, hanging in my old skyblock island_