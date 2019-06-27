package wayfinding;

import android.content.Context;
import android.util.Log;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import java.util.concurrent.CompletableFuture;


/**
 * This class holds all the information about our understanding of the space our AR application
 * operates within.
 * We suggest using the base Scene as the parent in the MapPlan constructor for reasons outlined
 * below.
 */

public class MapPlan extends Node {

    private static final String TAG = "wayfinder.MapPlan";
    private static ModelRenderable basePlan;
    private static final Vector3 relativeMapPos = new Vector3(18.5f, -1f, 22f);

    public MapPlan(Context context, NodeParent parent, Node seeder, CompletableFuture<ModelRenderable> basePlanFuture) {
        super();
        // Upon construction, start loading the models for the floor plan. TODO remove this since we don't want the floor plan in the end product

        if (!basePlanFuture.isDone()) {
            CompletableFuture.allOf(basePlanFuture)
                    .thenAccept((Void aVoid) -> placeMap(parent, seeder, basePlanFuture))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
        } else {
            placeMap(parent, seeder, basePlanFuture);
        }

        // we create a temporary node within the local space of the augmentedImage that is detected
        // this node will be the one that we attach our understanding of the map space to
        // once done, we translate the map space such that the location of the augmentedImage lines
        // up with the it's location in the map space (this must be hard coded)

        // after this is done, we take the world position and rotation and use those values to
        // create the real mapNode object. The reason we do this is that by attaching the mapNode
        // to the scene instead of the augmentedImage, we can reduce the amount of tilt that
        // the model will suffer from as the scene is oriented upwards using the gravity sensor
        // in the AR device, as opposed to more complicated (and less accurate) world mapping
        // algorithms used to remember where the augmentedImage is in AR space


    }

    public void placeMap(NodeParent parent, Node seeder, CompletableFuture<ModelRenderable> basePlanFuture){
        basePlan = basePlanFuture.getNow(null);
        if (basePlan == null) {
            Log.e(TAG, "BasePlan should not be null!");
        }
        Node localMapNode = new Node();
        localMapNode.setParent(seeder);
        localMapNode.setLocalPosition(relativeMapPos);
        Vector3 mapLocalPos = localMapNode.getWorldPosition();
        Quaternion mapLocalRotation = localMapNode.getWorldRotation();;
        this.setLocalRotation(mapLocalRotation);
        this.setParent(parent);
        this.setLocalPosition(mapLocalPos);
        System.out.println(basePlan.toString());
        this.setRenderable(basePlan);
    }

}
