# Introduction
Built off the sample example found [here](https://github.com/google-ar/sceneform-android-sdk/tree/master/samples/augmentedimage)

This is a demo app which allows a user to find their way around the Sydney BCGDV office.

[Example video](https://drive.google.com/file/d/1Hc3D2SL1ztB0TYWzHs07SyerGJZwrhGT/view?usp=sharing)

The augmented images database has two images in it (you'll need to print these out or have them on your monitor for the app to work):

1. [This one](https://i.imgur.com/XSbTCdc.jpg) is used to bring up a menu that lets the user select where they want to navigate to.
2. [This one](https://i.imgur.com/jA6y6uI.png) is used to show a video (the one at the end of the demo) when scanned.

To add more images to the database, check AR Core's documentation on the [arcoreimg](https://developers.google.com/ar/develop/c/augmented-images/arcoreimg) tool and the Augmented Images library in general.

To make life easier, the root node's location and edge details can be changed in the MapPlan.java file instead of the .json files used to define the rest of the map.

The project is meant for Android devices with an API level of >26, although it should still work for API levels 24-26. It should be buildable out of Android Studio without any changes.

Thanks to Bohdan Kostko for his work on this project.

