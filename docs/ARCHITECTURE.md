# Tor-phone — Architecture

## Current status
The repository has an Android Gradle project and a dashboard shell. VPN, Tor, traffic routing, DNS protection, kill switch, and server catalog are not implemented or active.

## Target components
1. MainActivity: presentation only.
2. ConnectionCoordinator: one state machine for OFF, CONNECTING, VPN, TOR, HANDOVER and BLOCKED.
3. One Android VpnService: Android normally permits one active VPN interface per user/profile. VPN and Tor must be coordinated behind one owner rather than competing VPN services.
4. VPN adapter: documented provider SDK/API or imported WireGuard profile.
5. Tor adapter: maintained Tor Android integration.
6. Diagnostics: explicit-consent IP/DNS/IPv6 tests with minimal retention.
7. Secret storage: Android Keystore-backed protection where appropriate; never commit secrets.

## Safe transitions
A replacement route is considered connected only after it is established and verified. If handover fails, retain the previous tunnel when safe; otherwise enter a blocked/error state. Never display protected status when the tunnel is down.

## Privacy boundaries
VPN changes public network egress, not GPS, cell/Wi-Fi observations, device identifiers, account identity, or browser fingerprint. Android permission boundaries remain controlled by the OS.
