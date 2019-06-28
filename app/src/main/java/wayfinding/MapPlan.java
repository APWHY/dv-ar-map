package wayfinding;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.augmentedimage.R;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

    private Map<String, EntryPoint> entries = new HashMap<>();
    private SparseArray<NavPoint> navs = new SparseArray<>();

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
        Gson gson = new Gson();

        // load entry points (nodes which are in the doorway of a room)
        InputStream inputStream = context.getResources().openRawResource(R.raw.entry_points);
        JSONPoint[] jsonNavs = gson.fromJson(new InputStreamReader(inputStream), JSONPoint[].class);

        for (JSONPoint p: jsonNavs){
            EntryPoint newEntry = new EntryPoint(p);
            entries.put(p.roomName , newEntry);
            navs.put(p.id, newEntry);
        }

        // load nav points (the other nodes in our graph)
        inputStream = context.getResources().openRawResource(R.raw.nav_points);
        jsonNavs = gson.fromJson(new InputStreamReader(inputStream), JSONPoint[].class);

        for (JSONPoint p: jsonNavs) navs.put(p.id, new NavPoint(p));

        // populate graph with edges
        inputStream = context.getResources().openRawResource(R.raw.edges);
        JSONEdge[] jsonEdges = gson.fromJson(new InputStreamReader(inputStream), JSONEdge[].class);
        for (JSONEdge e: jsonEdges){
            try {
                navs.get(e.from).addEdge(navs.get(e.to));
                navs.get(e.to).addEdge(navs.get(e.from));
            } catch (NullPointerException ex) {
                Log.e(TAG, String.format(Locale.ENGLISH,
                    "NullPointerException when assigning edges %d and %d.", e.from, e.to));
            }
        }

        for( int j = 0; j < navs.size(); j++) navs.valueAt(j).setParent(this);

        System.out.println(entries.get("Spindle").toString());
        System.out.println(entries.get("Venture Room 1").toString());

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
