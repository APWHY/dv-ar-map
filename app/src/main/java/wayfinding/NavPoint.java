package wayfinding;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Vector3;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class NavPoint extends Node {
    private static final String TAG = "wayfinder.NavPoint";
    private static final String STRING_FMT = "%s: id is %d, (x,z) is (%f,%f), edges are %s";
    private static final float HOVER_HEIGHT = 1f;

    private final int id;
    private final float x;
    private final float z;

    private Set<Edge> edges = new HashSet<>();

    public NavPoint(JSONPoint jsonPoint) {
        super();
        this.id = jsonPoint.id;
        this.x = jsonPoint.x;
        this.z = jsonPoint.z;
    }

    public NavPoint(int id, float x, float z){
        super();
        this.id = id;
        this.x = x;
        this.z = z;
    }

    public void setParent(NodeParent parent){
        super.setParent(parent);
        this.setLocalPosition(new Vector3(this.x, HOVER_HEIGHT, this.z));
    }

    protected String getNodeString(){ return super.toString(); }

    @Override
    public String toString(){
        return String.format(Locale.ENGLISH, STRING_FMT,
                TAG, this.id, this.x, this.z,
                this.edges.stream().map(Edge::toString).collect(Collectors.joining("|")));
    }

    public void addEdge(NavPoint to){
        edges.add(new Edge(this, to));
    }
    public Iterator<Edge> getEdges(){
        return this.edges.iterator();
    }

    public int getId(){ return id; }
    public float getX(){ return x; }
    public float getZ(){ return z; }

}
