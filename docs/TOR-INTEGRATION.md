# Internal Tor integration

## Current implementation state
The external Tor companion integration has been removed from the application and manifest. The repository does not yet contain an embedded Tor engine, a packet-to-stream bridge, or a running Android VPN service. Therefore, Tor protection is currently unavailable and the application must display it as inactive.

## Required architecture
The application must own Android's single active VPN interface through its own android.net.VpnService. The service must establish a TUN interface, capture device traffic, and forward supported flows through an internally managed Tor transport. A SOCKS proxy alone is not sufficient to route arbitrary IP packets from a TUN interface; a maintained packet-to-stream bridge or an equivalent integrated routing implementation is required.

## Android VPN constraints
Android generally allows one active VPN service per user/profile. The application must request user approval through VpnService.prepare() before establishing its interface. Always-on VPN and the system's block-connections-without-VPN setting are controlled by Android settings and must not be represented as silently enabled by the application.

## No false security claims
- A button press is not evidence of a working tunnel.
- Do not show a protected state until the service, packet forwarding, Tor connectivity, DNS handling, IPv4/IPv6 behavior, and failure handling are verified.
- If the packet-processing loop fails, the service must close the TUN interface and report failure.
- Tor changes network routing and the exit IP visible to sites; it does not change GPS, device identifiers, account identity, or all location signals.
- Tor is not a general-purpose speed VPN. Captchas, blocked services, and app incompatibility can occur.
