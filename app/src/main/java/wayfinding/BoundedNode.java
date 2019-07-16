package wayfinding;

import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.Locale;

class BoundedNode extends Node {
    private static final String TAG = "wayfinder.BoundedNode";

    // offsets for the region within which this node is visible
    private double forward;
    private double backward;
    private double left;
    private double right;
    private Renderable model;
    private boolean isVisible = false; // whether or not the node will be visible at all

    private Material[] fadeTextures;

    BoundedNode(double forward, double backward, double left, double right, Renderable model, Material[] fadeTextures) {
        super();
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;

        this.model = model;
        this.fadeTextures = fadeTextures;
    }


    BoundedNode(Material[] fadeTextures) {
        this(5, 5, 5, 5, null, fadeTextures);
    }

    Renderable getModel() {
        return this.model;
    }

    void setModel(Renderable model) {
        this.model = model;
    }

    void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    void changeColor(Color color) {
        Material[] newTextures = new Material[this.fadeTextures.length];
        for (int j = 0; j < this.fadeTextures.length; j++) {
            newTextures[j] = this.fadeTextures[j].makeCopy();
            newTextures[j].setFloat3(MaterialFactory.MATERIAL_COLOR, new Color(color.r, color.g, color.b, (float) j * 0.01f));
        }
        this.fadeTextures = newTextures;
    }

    void update(Context context, Scene scene) {
        if (!this.isVisible) {
            this.setRenderable(null);
            return;
        }
        try {
            Vector3 personLoc = worldToLocalPoint(scene.getCamera().getWorldPosition());
            if ((personLoc.x <= this.left && personLoc.x >= -this.right) &&
                    (personLoc.z <= this.forward && personLoc.z >= -this.backward)) {

                // personLoc is (x1,z1) gradient is (z1/x1) as we can use origin as the other point
                // formula for intersection of personLoc vector from origin and corners of the rectangle is
                // (z1/x1)x = z

                double horizontal = this.right;
                if (personLoc.x > 0) {
                    horizontal = this.left;
                } // note the x axis is flipped here
                double vertical = this.forward;
                if (personLoc.z < 0) {
                    vertical = this.backward;
                }

                Double boundaryHorizontal = Math.abs(personLoc.x / horizontal);
                Double boundaryVertical = Math.abs(personLoc.z / vertical);

                // opacity will fade the object in for the outside half of the rectangle.
                // It is set to full for the inside half of the rectangle
                // (which is actually only 1 quarter of the area, so maybe I might need to rejig the numbers a little)
                float opacity = 2 * (1 - (float) Math.max(boundaryHorizontal, boundaryVertical));
                this.setOpacity(context, opacity);

                this.setRenderable(this.model);

            } else {
                this.setRenderable(null);
            }
        } catch (NullPointerException e) {
            Log.e(TAG, String.format(Locale.ENGLISH, "Could not get world position of camera in scene, %s", e));

        }
    }

    // opacity is a float from 0 to 1 -- numbers outside of that will be ignored
    private void setOpacity(Context context, float opacity) {
        if (opacity < 0) return;

        if (opacity > 1) opacity = 1;
        if (this.model instanceof ViewRenderable) {
            ((LinearLayout) ((ViewRenderable) this.model).getView()).getChildAt(0).setAlpha(opacity);
            return;
        }

        if (opacity < 1) {
            this.model.setMaterial(fadeTextures[(int) (opacity * 100)]);
        } else {
            this.model.setMaterial(fadeTextures[100]);
        }
    }
}
