package wayfinding;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.samples.augmentedimage.R;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;


/**
 * This class holds all the information about our understanding of the space our AR application
 * operates within.
 * I suggest using the base Scene as the parent in the MapPlan constructor for reasons outlined
 * below.
 */

public class MapPlan extends Node {

    private static final String TAG = "wayfinder.MapPlan";
    private static final Vector3 RELATIVE_MAP_POS = new Vector3(18.5f, -1f, 22f);

    // We have all of the data for the map graph in the three json files, but we put the root node data here
    // This is so that when we move the position of the image it is easier to move the root node around
    private static final int ROOT_NAV = 0; // the id of the root node
    private static final int[] ROOT_EDGES = new int[] {2,21,22,23,24,25,3}; // the nodes that this root node is connected to

    private ModelRenderable mapModel;
    private ModelRenderable navArrow;
    private ModelRenderable destArrow;
    private ViewRenderable roomMenu;
    private final CountDownLatch latch = new CountDownLatch(1);

    Node menuNode;

    private Map<String, EntryPoint> entries = new HashMap<>();
    private SparseArray<NavPoint> navs = new SparseArray<>();
    Map<NavPoint, DijkstraPoint> navToDijkstra = new HashMap<>();

    public MapPlan(Context context) {
        super();

        this.loadGraphFromJSON(context);
        this.loadModels(context);
        this.setUpModels();
        this.makeSPT();
    }

    public void update(){
        Scene scene = getScene(); // save a million unneeded function calls
        for (int j = 0; j < navs.size(); j++) { navs.valueAt(j).update(scene); }
//        entries.get("Spindle").update(getScene());

    }

    public void showMap(NodeParent parent, Node seeder){


        // Upon construction, start loading the models for the floor plan. TODO remove this since we don't want the floor plan in the end product

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

        Node localMapNode = new Node();
        localMapNode.setParent(seeder);
        localMapNode.setLocalPosition(RELATIVE_MAP_POS);
        Vector3 mapLocalPos = localMapNode.getWorldPosition();
        Quaternion mapLocalRotation = localMapNode.getWorldRotation();

        this.setLocalRotation(mapLocalRotation);
        this.setParent(parent);
        this.setLocalPosition(mapLocalPos);
//        this.setRenderable(mapModel);




        this.menuNode = new Node();
        menuNode.setParent(seeder);
        menuNode.setLocalPosition(new Vector3(0,0.25f,0));
        menuNode.setLocalScale(new Vector3(0.5f,0.5f,0.5f));

        Vector3 menuWorldPos = menuNode.getWorldPosition();
        menuNode.setParent(this);
        menuNode.setWorldPosition(menuWorldPos);
        try {
            menuNode.setWorldRotation(getScene().getCamera().getWorldRotation());

        } catch (NullPointerException e){
            Log.e(TAG, String.format(Locale.ENGLISH, "Couldn't get world rotation from scene camera, %s", e));

        }

        // wait for models to finish loading before doing this part -- blocking operation but I don't want to make a whole runnable thing just for
        try{
            latch.await();
            menuNode.setRenderable(roomMenu);
            for (int j = 0; j < navs.size(); j++) { navs.valueAt(j).setModel(navArrow); }
            for (EntryPoint p : entries.values().toArray(new EntryPoint[]{})){ p.setModel(destArrow); }
        } catch (InterruptedException e) {
            Log.e(TAG, String.format(Locale.ENGLISH, "Couldn't wait for renderables, %s", e));
        }
//        menuNode.setRenderable(roomMenu);


    }

    // Traverses the linked list provided for by this.makeSPT() for the target, makes arrows appear
    // at each node in the linked list before rotating them to point at each other
    public void chooseTarget(String roomName){
        for (int j = 0; j < navs.size(); j++) { navs.valueAt(j).setVisible(false); }
        this.menuNode.setRenderable(null);

        EntryPoint target = entries.get(roomName);
        target.setVisible(true);

        DijkstraPoint current =  navToDijkstra.get(navs.get(target.getId()));

        while (current.prev != null){
            // calculate the smaller of the two angles between the default direction of the arrow
            // and the direction we want the arrow to be pointing in
            float angleTo = Vector3.angleBetweenVectors(
                Vector3.subtract(
                    new Vector3(current.prev.point.getX(),0,current.prev.point.getZ()+1), //this might be able to be the vector (0,0,1)
                    new Vector3(current.prev.point.getX(),0,current.prev.point.getZ())
                ),
                Vector3.subtract(
                    new Vector3(current.point.getX(),0,current.point.getZ()),
                    new Vector3(current.prev.point.getX(),0,current.prev.point.getZ())
                )
            );
            // account for the fact that our rotations are only in the clockwise direction by
            // making an obtuse rotation if needed (since angleBetweenVectors only gives value
            // in the range [0,180])
            if(current.point.getX() < current.prev.point.getX()) { // note that Android Studio uses RHS axes
                angleTo = 360 - angleTo;
            }
            current.prev.point.setRotation(angleTo);
            current.prev.point.setVisible(true);

            current = current.prev;
        }
    }

    private void loadGraphFromJSON(@org.jetbrains.annotations.NotNull Context context){
        Gson gson = new Gson();

        // load entry points (nodes which are in the doorway of a room)
        InputStream inputStream = context.getResources().openRawResource(R.raw.entry_points);
        JSONPoint[] jsonNavs = gson.fromJson(new InputStreamReader(inputStream), JSONPoint[].class);

        for (JSONPoint p : jsonNavs) {
            EntryPoint newEntry = new EntryPoint(p);
            entries.put(p.roomName, newEntry);
            navs.put(p.id, newEntry);
        }

        // load nav points (the other nodes in our graph)
        inputStream = context.getResources().openRawResource(R.raw.nav_points);
        jsonNavs = gson.fromJson(new InputStreamReader(inputStream), JSONPoint[].class);

        for (JSONPoint p : jsonNavs) navs.put(p.id, new NavPoint(p));

        // add the required data for the root node and insert into the graph
        navs.put(ROOT_NAV, new NavPoint(ROOT_NAV, RELATIVE_MAP_POS.x, RELATIVE_MAP_POS.z));
        for (int rootEdge : ROOT_EDGES){
            try {
                navs.get(ROOT_NAV).addEdge(navs.get(rootEdge));
                navs.get(rootEdge).addEdge(navs.get(ROOT_NAV));
            } catch (NullPointerException ex) {
                Log.e(TAG, String.format(Locale.ENGLISH,
                        "NullPointerException when assigning an edge between the root and %d.", rootEdge));
            }
        }

        // populate graph with edges
        inputStream = context.getResources().openRawResource(R.raw.edges);
        JSONEdge[] jsonEdges = gson.fromJson(new InputStreamReader(inputStream), JSONEdge[].class);
        for (JSONEdge e : jsonEdges) {
            try {
                navs.get(e.from).addEdge(navs.get(e.to));
                navs.get(e.to).addEdge(navs.get(e.from));
            } catch (NullPointerException ex) {
                Log.e(TAG, String.format(Locale.ENGLISH,
                        "NullPointerException when assigning edges %d and %d.", e.from, e.to));
            }
        }
        for (int j = 0; j < navs.size(); j++) { navs.valueAt(j).setParent(this); }

    }

    // load the models (floor plan, blue and green arrows) -- the floor plan is only needed for debugging, however
    private void loadModels(Context context){
        // prepare to generate the menu we use to choose our target
        LayoutInflater menuInflater = ((AppCompatActivity) context).getLayoutInflater();
        LinearLayout menuWrapper = new LinearLayout(context);
        View dvMapWrapper = menuInflater.inflate(R.layout.room_menu,menuWrapper,true);
        ScrollView roomMenu = dvMapWrapper.findViewById(R.id.dvMenu);
        ViewGroup roomMenuLayout = (ViewGroup) roomMenu.getChildAt(0);

        // the menu is generated dynamically from the contents of navs
        for (String room: entries.keySet()){
            View menuItemWrapper =  menuInflater.inflate(R.layout.menu_item, roomMenuLayout, false);
            TextView newLine = menuItemWrapper.findViewById(R.id.menuItem);
            newLine.setText(room);
            roomMenuLayout.addView(newLine);

            // prepare the room cards that we display in front of every room before passing the models to the relevant EntryPoint objects
            LayoutInflater roomInflater = ((AppCompatActivity) context).getLayoutInflater();
            LinearLayout roomCardWrapper = new LinearLayout(context);
            View roomCard = roomInflater.inflate(R.layout.room_card, roomCardWrapper, true);
            TextView cardText = roomCard.findViewById(R.id.room_card);
            cardText.setText(room);
            ViewRenderable.builder().setView(context, roomCardWrapper).build()
                    .thenAccept( renderable -> entries.get(room).setCard(renderable))
                    .exceptionally(e -> {
                        Log.e(TAG, String.format(Locale.ENGLISH, "Unable to load renderable, %s", e));
                        return null;
                    });
        }
        CompletableFuture<ModelRenderable> mapModelFuture =
                ModelRenderable.builder()
                        .setSource(context, R.raw.initial)
                        .build();
        CompletableFuture<ModelRenderable> navArrowFuture =
                ModelRenderable.builder()
                        .setSource(context, R.raw.blue_arrow)
                        .build();
        CompletableFuture<ModelRenderable> destArrowFuture=
                ModelRenderable.builder()
                        .setSource(context, R.raw.green_arrow)
                        .build();
        // Build a renderable from a 2D View.
        CompletableFuture<ViewRenderable> roomMenuFuture =
                ViewRenderable.builder().setView(context, menuWrapper).build();

        CompletableFuture.allOf(mapModelFuture, navArrowFuture, destArrowFuture, roomMenuFuture)
                .handle(
                        (aVoid, throwable) -> {
                            if (throwable != null) {
                                Log.e(TAG, String.format(Locale.ENGLISH, "Unable to load renderable, %s", throwable));
                                SnackbarHelper.getInstance().showMessage((AppCompatActivity) context, "Failed to load renderables!");
                            }
                            try {
                                this.mapModel = mapModelFuture.get();
                                this.navArrow = navArrowFuture.get();
                                this.destArrow = destArrowFuture.get();
                                this.roomMenu = roomMenuFuture.get();
                                this.latch.countDown();
                            } catch (InterruptedException | ExecutionException ex) {
                                Log.e(TAG, String.format(Locale.ENGLISH, "Unable to get renderable, %s", ex));
                                SnackbarHelper.getInstance().showMessage((AppCompatActivity) context, "Failed to get renderables!");
                            }
                            return null;
                        });
    }


    private void setUpModels(){
        for(EntryPoint e: entries.values().toArray(new EntryPoint[]{})){ e.pointToRoom(); }
    }

    // This is a helper class which we use to build our SPT (Shortest Path Tree) out of the graph
    // that we have stored in this.navs
    private class DijkstraPoint {
        NavPoint point;
        DijkstraPoint prev;
        double dist;
        private final double INF = 9999;

        private DijkstraPoint(NavPoint point) {
            this.point = point;
            this.prev = null;
            this.dist = INF;
        }
        @NonNull
        public String toString(){
            return String.format(Locale.ENGLISH, "DijkstraPoint for: %s, Prev: %s, Dist: %f",
                    this.point.getId(), this.prev, this.dist);
        }
    }

    private class DijkstraComparator implements Comparator<DijkstraPoint> {
        @Override
        public int compare(DijkstraPoint first, DijkstraPoint second){
//            if (first.dist == second.dist){ return 0;}
//            if (first.dist < second.dist){ return -1;}
            return Double.compare(first.dist, second.dist);
        }
    }
    // Implementation of Dijkstra's algothrim for the graph. Can be made more efficient
    // by having it stop once the target is found but this allows the user to maybe come back and
    // choose a different one.
    private void makeSPT(){
        // initialise our vertex set and add the root node to it
        Queue<DijkstraPoint> unexplored = new PriorityQueue<>(new DijkstraComparator());
        DijkstraPoint rootDijkstra = new DijkstraPoint(navs.valueAt(ROOT_NAV));
        rootDijkstra.dist = 0;
        unexplored.add(rootDijkstra);
        // add the remaining nodes
        for (int j = 1; j < navs.size(); j++) {
            NavPoint n = navs.valueAt(j);
            unexplored.add(new DijkstraPoint(n));
        }
        for (DijkstraPoint d: unexplored){
            this.navToDijkstra.put(d.point, d);
        }
        while (unexplored.size() > 0){
            DijkstraPoint current = unexplored.poll();

            for (Edge e : current.point.getEdges()){
                DijkstraPoint to = this.navToDijkstra.get(e.getTo());
                if(!unexplored.contains(to)){continue;}
                double alt = current.dist + e.getDistance();
                if (alt < to.dist){
                    to.dist = alt;
                    to.prev = current;
                    unexplored.remove(to); // not doing this means the unexplored priority queue loses order
                    unexplored.add(to);    // it's ugly but does the job for now and a more efficient algorithm will take more time
                }
            }
        }
    }

}

