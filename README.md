# Tor-phone

Android privacy connection manager prototype built with native Java.

## Current status

**Phase 1 UI and documentation scaffold.** The repository contains an Android Gradle project, a privacy dashboard shell, product requirements, architecture notes, and a security plan.

**Not yet implemented:** VPN tunnel, Tor service, device-wide traffic routing, provider/server catalog, DNS/IP leak protection, automatic kill switch, or verified fail-closed behavior. The Connect button is intentionally informational and does not claim to protect traffic.

## Product decisions

- Android first; native Java.
- VPN and Tor are separate modes, with Tor-over-VPN considered as a later explicit mode.
- Device-wide routing is the goal, subject to Android's VPN/profile limits.
- Manual server/circuit changes by default.
- Safe handover where supported; otherwise keep the old tunnel or block rather than silently bypass protection.
- Minimal permissions. No location permission is required for core VPN routing.

## Servers and providers

Tor uses the public Tor network; it does not require us to invent or operate private Tor servers.

VPN requires a real provider endpoint and valid user/provider configuration. The app will support a documented provider integration or user-imported WireGuard profile. Free VPN plans commonly restrict countries, bandwidth, protocol, or devices. Unknown public VPN servers are not trusted and will not be bundled as secure. Never commit credentials or private keys.

## Privacy limits

A VPN changes public network egress, not GPS, cell/Wi-Fi observations, device identifiers, account identity, or browser fingerprint. Android does not allow an ordinary app to silently grant/revoke other apps' permissions or force them to report the VPN server's location.

## Repository guide

- `app/src/main/java/org/torphone/app/MainActivity.java` — dashboard UI shell
- `docs/REQUIREMENTS.md` — product requirements
- `docs/ARCHITECTURE.md` — target connection architecture
- `docs/SECURITY-PLAN.md` — threat model and verification plan
- `docs/PROVIDER-OPTIONS.md` — review of free VPN and Tor infrastructure

## Build

Open the repository in Android Studio and sync the Gradle project. Build/debug on a real Android device before treating any feature as operational. This repository has not yet been built or device-tested in this development step.
