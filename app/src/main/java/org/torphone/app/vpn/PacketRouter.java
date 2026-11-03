package org.torphone.app.vpn;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

/** Contract for a router that owns the VPN TUN descriptor after start(). */
public interface PacketRouter {
    int init(Context context);
    void start(ParcelFileDescriptor tunInterface) throws IOException;
    void stop();
    boolean isRunning();
}
