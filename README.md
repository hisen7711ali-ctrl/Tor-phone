# Tor-phone

Android privacy connection dashboard prototype built with native Java.

## Current status

**Phase 2 — Orbot companion launch + Android CI scaffold.** Tor-phone can launch Orbot if installed, or open Orbot's official Play Store listing. Orbot performs the Tor VPN routing after the user approves Android's VPN prompt and enables VPN mode / Protect all apps.

**Important:** this is not an embedded Tor engine. Tor-phone does not currently own the VPN tunnel or verify Orbot's live state. No green "protected" claim is made. An Android user/profile generally has one active VPN service; Orbot and a future Tor-phone VpnService cannot run as independent simultaneous tunnels.

## What's in the repository
- app/src/main/java/org/torphone/app/MainActivity.java — privacy dashboard and Orbot launcher
- docs/TOR-INTEGRATION.md — enable Tor across apps using Orbot
- docs/PROVIDER-OPTIONS.md — Proton VPN, VPN Gate, and user-owned server notes
- docs/REQUIREMENTS.md — product requirements
- docs/ARCHITECTURE.md — target routing architecture
- docs/SECURITY-PLAN.md — threat model and verification plan
- .github/workflows/android-build.yml — automated debug build

## VPN providers and custom servers
Proton VPN Free is a separate provider with limited free server selection. VPN Gate is a volunteer relay network whose policy documents retention of connection logs for three or more months; it is not a trusted default. User-owned/provider-issued WireGuard and OpenVPN profile import is planned but not implemented yet. Never commit private keys, credentials, or server profiles.

## Privacy limits
VPN/Tor changes network routing, not GPS, cell/Wi-Fi observations, device identifiers, account identity, or browser fingerprint. Android does not let an ordinary app silently grant/revoke other apps' permissions or force them to report the VPN server's location.

## Build and verification
GitHub Actions now attempts a debug APK build on pushes and pull requests. A successful CI build is not the same as device validation. Before release, test Orbot launch, all-app routing, DNS/IPv4/IPv6 behavior, network changes, and Android lockdown failure behavior on real devices.
