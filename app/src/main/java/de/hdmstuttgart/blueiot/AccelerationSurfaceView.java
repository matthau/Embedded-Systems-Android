package de.hdmstuttgart.blueiot;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Custom View-Class that is drawn onto by a separate background-thread
 */
public class AccelerationSurfaceView extends SurfaceView {
    private Context context;

    private AccelerationSurfaceThread thread;
    public AccelerationSurfaceThread getThread() {
        return this.thread;
    }

    /**
     * Constructor
     * @param context ApplicationContext used to inflate layout components
     */
    public AccelerationSurfaceView(Context context) {
        super(context);
        this.context = context;
    }

    /**
     * Initializes the SurfaceView by implementing all of its lifecycle-callback-methods that are used in order to draw (surfaceCreated|surfaceChanged|surfaceDestroyed)
     * @param device The BluetoothDevice that is passed over to the background-thread in order to connect to it
     */
    public void initialize(final BluetoothDevice device) {
        SurfaceHolder surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //Start new 'Drawing'-Thread, pass over the SurfaceHolder, the Context and the BluetoothDevice to connect to
                thread = new AccelerationSurfaceThread(holder, context, device);
                thread.setRunning(true);
                thread.start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                //Update the Surface Size
                thread.setSurfaceSize(width, height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //Stop the Thread when the Surface is destroyed
                boolean retry = true;
                thread.setRunning(false);
                while (retry) {
                    try {
                        thread.join();
                        retry = false;
                    } catch (InterruptedException ex) {}
                }
            }
        });
    }
}