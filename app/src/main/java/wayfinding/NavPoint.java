package wayfinding;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Material;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

class NavPoint extends BoundedNode {
    private static final String TAG = "wayfinder.NavPoint";
    private static final String STRING_FMT = "%s: id is %d, (x,z) is (%f,%f), edges are %s";
    private static final float HOVER_HEIGHT = 0f;

    private final int id;
    private final float x;
    private final float z;

    private Set<Edge> edges = new HashSet<>();


    NavPoint(int id, float x, float z, Material[] fadeTextures){
        super(fadeTextures);
        this.id = id;
        this.x = x;
        this.z = z;
    }

    NavPoint(JSONPoint jsonPoint, Material[] fadeTextures) { this(jsonPoint.id, jsonPoint.x, jsonPoint.z, fadeTextures); }


    public void setParent(NodeParent parent){
        super.setParent(parent);
        this.setLocalPosition(new Vector3(-this.x, HOVER_HEIGHT, -this.z)); // inverse of the initial translation of the map node
    }

    void setRotation(float degrees){
        this.setLocalRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0), degrees));
    }

    protected String getNodeString(){ return super.toString(); }

    @Override
    public String toString(){
        return String.format(Locale.ENGLISH, STRING_FMT,
            TAG, this.id, this.x, this.z,
            this.edges.stream().map(Edge::toString).collect(Collectors.joining("|")));
    }

    void addEdge(NavPoint to){
        this.edges.add(new Edge(this, to));
    }
    Edge[] getEdges(){ return this.edges.toArray(new Edge[]{}); }

    public int getId(){ return this.id; }
    public float getX(){ return this.x; }
    public float getZ(){ return this.z; }

}
