/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.sceneform.samples.augmentedimage;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import wayfinding.MapPlan;
import wayfinding.VideoNode;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 */
public class AugmentedImageActivity extends AppCompatActivity {
    private static final String VIDEO_CODE_FILE_NAME = "frame.png";

    private ArFragment arFragment;
    private ImageView fitToScanView;
    private AnchorNode foundNode = null;

    private MapPlan mapPlan;
    private VideoNode video;

    // Augmented image and its associated center pose anchor, keyed by the augmented image in
    // the database.
    private final Map<AugmentedImage, AnchorNode> augmentedImageMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        fitToScanView = findViewById(R.id.image_view_fit_to_scan);
        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

        this.video = new VideoNode(this, R.raw.dv_video);
        this.mapPlan = new MapPlan(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (augmentedImageMap.isEmpty()) {
            fitToScanView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Registered with the Sceneform Scene object, this method is called at the start of each frame.
     *
     * @param frameTime - time since last frame.
     */
    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        // If there is no frame or ARCore is not tracking yet, just return.
        if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            return;
        }

        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);


        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            TrackingState trackingState = augmentedImage.getTrackingState();
            switch (trackingState) {
                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    String text = "Detected Image " + augmentedImage.getIndex();
                    SnackbarHelper.getInstance().showMessage(this, text);
                    break;

                case TRACKING:
                    // Have to switch to UI Thread to update View.
                    fitToScanView.setVisibility(View.GONE);
                    // Create a new anchor for newly found images.

                    if (augmentedImageMap.containsKey(augmentedImage)) {
                        if (!VIDEO_CODE_FILE_NAME.equals(augmentedImage.getName())) {
                            this.mapPlan.update(this);
                        }
                        break;
                    }

                    foundNode = new AnchorNode();
                    foundNode.setAnchor(augmentedImage.createAnchor(augmentedImage.getCenterPose()));
                    // save node for future reference.
                    augmentedImageMap.put(augmentedImage, foundNode);

                    Scene scene = arFragment.getArSceneView().getScene();
                    scene.addChild(foundNode);

                    // render either video or map.
                    // ok, this filename based detection is crappy.
                    if (VIDEO_CODE_FILE_NAME.equals(augmentedImage.getName())) {
                        // it's video.
                        video.render(foundNode);
                        break;
                    }

                    // it's a map!
                    this.mapPlan.showMap(scene, foundNode);
//                    this.mapPlan.update(this);
                    break;
                case STOPPED:
                    augmentedImageMap.remove(augmentedImage);
                    break;
            }
        }
    }

    public void chooseTarget(View v){ this.mapPlan.chooseTarget(((Button) v).getText().toString()); }


}
