package wayfinding;

import java.util.Locale;

public class EntryPoint extends NavPoint {
    private static final String TAG = "wayfinder.EntryPoint";
    private static final String STRING_FMT = "%s: roomName is %s, angleIn is %f, NavPoint is %s";

    private String roomName;
    private float angleIn;



    public EntryPoint(JSONPoint jsonPoint){
        super(jsonPoint);
        this.roomName = jsonPoint.roomName;
        this.angleIn = jsonPoint.angleIn;
    }

    public void pointToRoom() {
        this.setRotation(this.angleIn);
    }

    public String getRoomName(){
        return this.roomName;
    }

//    public float getAngleIn() {
//        return this.angleIn;
//    }

    @Override
    public String toString(){
        return String.format(Locale.ENGLISH, STRING_FMT,
            TAG, this.roomName, this.angleIn, super.toString());
    }

}

// JSONPoint is just a helper class to help parse JSON using the gson library
// We cannot parse directly into EntryPoint and NavPoint as they both extend the Node class which
// has a method set as a variable or something -- gson can't handle that case on its own
class JSONPoint {
    String roomName;
    float angleIn;
    int id;
    float x,z;

    JSONPoint(){} // empty constructor needed for gson
}