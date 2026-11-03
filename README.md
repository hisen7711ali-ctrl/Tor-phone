# Tor-phone

Android privacy connection application prototype built with native Java.

## Current status

The external Tor companion integration has been removed. The application currently has no embedded Tor engine, no active VPN service, and no device-wide traffic routing. Its dashboard must not be interpreted as an active protection connection.

Android permits one active VPN service per user/profile. A standalone application therefore needs to own that VPN service and integrate its Tor transport and packet-routing components internally.

## Repository contents
- app/src/main/java/org/torphone/app/MainActivity.java — privacy dashboard
- docs/TOR-INTEGRATION.md — internal Tor integration requirements and current limitations
- docs/PROVIDER-OPTIONS.md — provider and user-owned server requirements
- docs/REQUIREMENTS.md — product requirements
- docs/ARCHITECTURE.md — target routing architecture
- docs/SECURITY-PLAN.md — threat model and verification plan
- .github/workflows/android-build.yml — automated debug build

## VPN providers and custom servers
A VPN service may only be integrated through an officially supported interface or a user-authorized configuration. User-owned/provider-issued WireGuard and OpenVPN profile import is not implemented. Never commit private keys, credentials, or server profiles. Do not invent server addresses or advertise unverified free servers.

## Privacy limits
VPN/Tor changes network routing, not GPS, cell/Wi-Fi observations, device identifiers, account identity, or browser fingerprint. Android does not let an ordinary app silently grant/revoke other apps' permissions or force them to report the VPN server's location.

## Build and verification
GitHub Actions builds a debug APK. A successful build is not device validation. No protection claim is valid until the internal tunnel, packet forwarding, DNS and IPv6 behavior, network transitions, and fail-closed behavior have been tested on real devices.
