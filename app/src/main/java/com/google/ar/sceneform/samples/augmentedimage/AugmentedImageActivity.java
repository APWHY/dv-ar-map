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
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.augmentedimage.AugmentedImageNode;
import com.google.ar.sceneform.samples.augmentedimage.R;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import wayfinding.MapPlan;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 */
public class AugmentedImageActivity extends AppCompatActivity {

    private static final String TAG = "AugmentedImageActivity";

    private ArFragment arFragment;
    private ImageView fitToScanView;
    private com.google.ar.sceneform.samples.augmentedimage.AugmentedImageNode foundNode = null;

    private ModelRenderable mapModel;
    private ModelRenderable navArrow;
    private ModelRenderable destArrow;

    private boolean hasFinishedLoading = false;

    // Augmented image and its associated center pose anchor, keyed by the augmented image in
    // the database.
    private final Map<AugmentedImage, com.google.ar.sceneform.samples.augmentedimage.AugmentedImageNode> augmentedImageMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        fitToScanView = findViewById(R.id.image_view_fit_to_scan);

        CompletableFuture<ModelRenderable> mapModelFuture =
            ModelRenderable.builder()
                .setSource(this, R.raw.initial)
                .build();
        CompletableFuture<ModelRenderable> navArrowFuture =
            ModelRenderable.builder()
                .setSource(this, R.raw.blue_arrow)
                .build();
        CompletableFuture<ModelRenderable> destArrowFuture=
            ModelRenderable.builder()
                .setSource(this, R.raw.green_arrow)
                .build();



        CompletableFuture.allOf(mapModelFuture, navArrowFuture, destArrowFuture)
            .handle(
                (aVoid, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, String.format(Locale.ENGLISH, "Unable to load renderable, %s", throwable));
                        SnackbarHelper.getInstance().showMessage(this, "Failed to load renderables!");
                    }
                    try {
                        this.mapModel = mapModelFuture.get();
                        this.navArrow = navArrowFuture.get();
                        this.destArrow = destArrowFuture.get();

                        this.hasFinishedLoading = true;

                    } catch (InterruptedException | ExecutionException ex) {
                        Log.e(TAG, String.format(Locale.ENGLISH, "Unable to get renderable, %s", ex));
                        SnackbarHelper.getInstance().showMessage(this, "Failed to get renderables!");
                    }
                    return null;
                });








        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

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
            switch (augmentedImage.getTrackingState()) {
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
                    if (!augmentedImageMap.containsKey(augmentedImage)) {
                        foundNode = new AugmentedImageNode(this);
                        foundNode.setImage(augmentedImage);
                        augmentedImageMap.put(augmentedImage, foundNode);
                        arFragment.getArSceneView().getScene().addChild(foundNode);

                        MapPlan floorPlan = new MapPlan(this, arFragment.getArSceneView().getScene(), foundNode, mapModel, navArrow, destArrow);
                        System.out.println(floorPlan);
                        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

                    }

                    break;

                case STOPPED:
                    augmentedImageMap.remove(augmentedImage);
                    break;
            }
        }
    }
}
