package wayfinding;

import android.content.Context;

import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.augmentedimage.R;

import java.util.concurrent.CompletableFuture;

class ArrowLibrary {
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


}
