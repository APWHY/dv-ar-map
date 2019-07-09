package wayfinding;

import android.util.Log;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Renderable;

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
//    private boolean isDisplaying; // whether or not we display the node (contingent on distance from node)

    BoundedNode(double forward, double backward, double left, double right, Renderable model){
        super();
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        Log.d(TAG, String.format("SETSETSETSETforward: %f, backward %f, left %f, right %f", this.forward, this.backward, this.left, this.right));

        this.model = model;
    }


    BoundedNode(){this(5,5, 5, 5,null);}

    void setModel(Renderable model){ this.model = model; }
    void setVisible(boolean isVisible){ this.isVisible = isVisible; }
    void update(Scene scene){
//        Log.d(TAG, "UPDATED");
        // TODO: CALL UPDATE EVERYWHERE FROM APPCOMPATACTIVITY CLASS
        if (!this.isVisible) {
            this.setRenderable(null);
            return;
        }
        try{

            Vector3 personLoc = worldToLocalPoint(scene.getCamera().getWorldPosition());
//            Log.d(TAG, personLoc.toString());
//            Log.d(TAG, String.format("forward: %f, backward %f, left %f, right %f", this.forward, this.backward, this.left, this.right));
            if(( personLoc.x <= this.left    && personLoc.x >= -this.right    ) &&
                    ( personLoc.z <= this.forward && personLoc.z >= -this.backward )){
                this.setRenderable(this.model);
            } else {
                this.setRenderable(null);
            }
        } catch (NullPointerException e) {
            Log.e(TAG, String.format(Locale.ENGLISH, "Could not get world position of camera in scene, %s", e));

        }



    }


}
