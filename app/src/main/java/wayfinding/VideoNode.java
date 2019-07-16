package wayfinding;

import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.augmentedimage.R;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;
import android.util.Log;

public class VideoNode extends Node {
    private MediaPlayer mediaPlayer;

    private ExternalTexture texture;

    private static final float VIDEO_HEIGHT_METERS = 0.85f;

    @Nullable
    private ModelRenderable videoRenderable;

    public VideoNode(Context context, int videoId) {
        super();

        texture = new ExternalTexture();

        mediaPlayer = MediaPlayer.create(context, videoId);
        mediaPlayer.setSurface(texture.getSurface());
        mediaPlayer.setLooping(true);

        ModelRenderable.builder()
                .setSource(context, R.raw.video_frame)
                .build()
                .thenAccept(
                        renderable -> {
                            videoRenderable = renderable;
                            renderable.getMaterial().setExternalTexture("videoTexture", texture);
                        }
                )
                .exceptionally(
                        throwable -> {
                            Log.e("VideoRenderable", throwable.toString());
                            return null;
                        });
    }

    public void render(AnchorNode node) {
        if (videoRenderable == null) {
            return;
        }

        // Create a node to render the video and add it to the anchor.
        Node videoNode = new Node();
        videoNode.setParent(node);

        // Set the scale of the node so that the aspect ratio of the video is correct.
        float videoWidth = mediaPlayer.getVideoWidth();
        float videoHeight = mediaPlayer.getVideoHeight();
        videoNode.setLocalScale(
                new Vector3(
                        VIDEO_HEIGHT_METERS * (videoWidth / videoHeight), VIDEO_HEIGHT_METERS, 1.0f));

        // Start playing the video when the first node is placed.
        if (mediaPlayer.isPlaying()) {
            videoNode.setRenderable(videoRenderable);
            return;
        }

        mediaPlayer.start();

        // Wait to set the renderable until the first frame of the  video becomes available.
        // This prevents the renderable from briefly appearing as a black quad before the video
        // plays.
        texture
                .getSurfaceTexture()
                .setOnFrameAvailableListener(
                        (SurfaceTexture surfaceTexture) -> {
                            videoNode.setRenderable(videoRenderable);
                            texture.getSurfaceTexture().setOnFrameAvailableListener(null);
                        });
    }
}
