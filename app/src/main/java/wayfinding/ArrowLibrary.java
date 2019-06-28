package wayfinding;

import android.content.Context;
import android.util.Log;

import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.augmentedimage.R;

import java.util.concurrent.CompletableFuture;

class ArrowLibrary {

    private static final String TAG = "wayfinder.ArrowLibrary";

    private static CompletableFuture<ModelRenderable> nav;
    private static CompletableFuture<ModelRenderable> destination;

    public ArrowLibrary(Context context) {
        if (nav == null) {
            nav =
                    ModelRenderable.builder()
                            .setSource(context, R.raw.blue_arrow)
                            .build();
            destination =
                    ModelRenderable.builder()
                            .setSource(context, R.raw.green_arrow)
                            .build();
        }
    }

    public ModelRenderable getNav(){
        if (!nav.isDone()) CompletableFuture.allOf(nav)
                .thenAccept((Void aVoid) -> getNav())
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Exception loading navArrow", throwable);
                            return null;
                        });
        else{
            return nav.getNow(null);
        }
        return null;
    }
    public ModelRenderable getDestination(){
        if (!destination.isDone()) CompletableFuture.allOf(destination)
                .thenAccept((Void aVoid) -> getDestination())
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Exception loading destinationArrow", throwable);
                            return null;
                        });
        else{
            return destination.getNow(null);
        }
        return null;
    }

}
