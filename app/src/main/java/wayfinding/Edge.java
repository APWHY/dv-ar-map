package wayfinding;

import com.google.ar.sceneform.math.Vector3;

import java.util.Locale;

public class Edge {
    private static final String TAG = "wayfinder.Edge";
    private static final String STRING_FMT = "%s: [to.id: %d, distance: %f]";


    private final NavPoint to;
    private final float distance;

    public Edge (NavPoint from, NavPoint to){
        this.to = to;
        this.distance = Vector3.subtract(
                new Vector3 (from.getX(), 0, from.getZ()),
                new Vector3 (to.getX(), 0, to.getZ())
            ).length();
    }

    public NavPoint getTo() {
        return this.to;
    }

    public float getDistance() {
        return this.distance;
    }

    public String toString() {
        return String.format(Locale.ENGLISH, STRING_FMT,
            TAG, this.to.getId(), this.distance);
    }
}


// JSONEdge is just a helper class to help parse JSON using the gson library
// It is easier to specify edges as two points, but for implementation purposes we want neighbour
// information as opposed to double vertex format (Dijkstra's algorithm, basically)
class JSONEdge {
    int from, to;
    JSONEdge(){} // empty constructor needed for gson
}

