package org.torphone.app.vpn;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import org.torproject.onionmasq.OnionMasq;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Adapter for the pinned, upstream Onionmasq Android API.
 * OnionMasq.start() takes ownership of the detached TUN fd and performs packet
 * processing in native code; Java must not read the same TUN descriptor.
 */
public final class OnionmasqRouter implements PacketRouter {
    public static final int INIT_OK = 0;
    public static final int INIT_FAILED = -1;

    private static final long START_TIMEOUT_MS = 30_000L;
    private static volatile boolean nativeLibraryLoaded;
    private static volatile Throwable nativeLoadFailure;

    static {
        try {
            System.loadLibrary("onionmasq_mobile");
            nativeLibraryLoaded = true;
        } catch (Throwable error) {
            nativeLoadFailure = error;
        }
    }

    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private volatile boolean initialized;
    private volatile boolean running;
    private volatile Throwable failure;
    private volatile Thread engineThread;
    private volatile Consumer<Throwable> failureListener = error -> { };

    public static int prepare(Context context) {
        return new OnionmasqRouter().init(context);
    }

    @Override
    public synchronized int init(Context context) {
        if (!nativeLibraryLoaded || context == null) return INIT_FAILED;
        try {
            OnionMasq.init(context.getApplicationContext());
            initialized = true;
            return INIT_OK;
        } catch (Throwable error) {
            failure = error;
            initialized = false;
            return INIT_FAILED;
        }
    }

    public void setFailureListener(Consumer<Throwable> listener) {
        failureListener = listener == null ? error -> { } : listener;
    }

    @Override
    public void start(ParcelFileDescriptor tunInterface) throws IOException {
        start(tunInterface, null);
    }

    @Override
    public void start(ParcelFileDescriptor tunInterface, String bridgeLines) throws IOException {
        if (!initialized) throw new IOException("Onionmasq is not initialized");
        if (tunInterface == null) throw new IOException("VPN TUN descriptor is missing");
        if (OnionMasq.isRunning()) {
            running = true;
            return;
        }

        final String normalizedBridgeLines =
                bridgeLines == null || bridgeLines.trim().isEmpty() ? null : bridgeLines.trim();
        stopping.set(false);
        failure = null;
        final int ownedFd = tunInterface.detachFd();
        engineThread = new Thread(() -> {
            try {
                OnionMasq.start(ownedFd, normalizedBridgeLines);
                if (!stopping.get()) reportFailure(new IOException("Onionmasq proxy stopped unexpectedly"));
            } catch (Throwable error) {
                if (!stopping.get()) reportFailure(error);
            }
        }, "TorPhone-Onionmasq");
        engineThread.start();

        long deadline = System.currentTimeMillis() + START_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Throwable startFailure = failure;
            if (startFailure != null) throw new IOException("Onionmasq failed to start", startFailure);
            try {
                if (OnionMasq.isRunning()) {
                    running = true;
                    return;
                }
            } catch (Throwable error) {
                reportFailure(error);
                throw new IOException("Unable to query Onionmasq state", error);
            }
            Thread thread = engineThread;
            if (thread == null || !thread.isAlive()) throw new IOException("Onionmasq exited before becoming ready");
            try {
                Thread.sleep(100L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                stop();
                throw new IOException("Interrupted while starting Onionmasq", interrupted);
            }
        }
        stop();
        throw new IOException("Onionmasq did not become ready before the startup timeout");
    }

    private void reportFailure(Throwable error) {
        failure = error;
        running = false;
        failureListener.accept(error);
    }

    @Override
    public synchronized void stop() {
        stopping.set(true);
        running = false;
        if (initialized) {
            try { OnionMasq.stop(); } catch (Throwable ignored) { }
        }
        Thread thread = engineThread;
        engineThread = null;
        if (thread != null && thread != Thread.currentThread()) thread.interrupt();
    }

    @Override
    public boolean isRunning() {
        if (!initialized || !nativeLibraryLoaded) return false;
        try {
            running = OnionMasq.isRunning();
        } catch (Throwable error) {
            reportFailure(error);
            return false;
        }
        return running;
    }

    public static String loadFailureMessage() {
        Throwable error = nativeLoadFailure;
        return error == null ? "تعذر تهيئة محرك Onionmasq" : error.getClass().getSimpleName();
    }
}
