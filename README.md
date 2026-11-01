# Tor-phone

Tor-phone is an Android privacy project intended to route device application traffic through a user-controlled VPN/Tor architecture.

## Project status

**Phase 1 — planning and project skeleton.** This repository is being initialized. No working VPN, Tor, traffic-routing, GPS-masking, or leak-protection engine is implemented yet.

## Initial requirements

- Android-only application, initially implemented in native Java.
- One coordinated Android `VpnService` for device-wide routing.
- User-selectable VPN and Tor modes, with a future combined Tor-over-VPN mode.
- Manual location/server rotation by default.
- Prefer safe handover when supported; otherwise retain the existing tunnel or fail closed rather than silently sending traffic outside the tunnel.
- Location privacy controls must respect Android permission boundaries and remain transparent and user-controlled.
- Never commit VPN credentials, private keys, or other secrets.

## Planned phases

1. Requirements, architecture, security model, and Android project skeleton.
2. Main application UI and settings.
3. VPN provider integration and tunnel lifecycle.
4. Tor integration and coordinated routing.
5. DNS/IP leak testing, fail-closed behavior, and device testing.

## Important limitations

A VPN changes the public network route; it does not change GPS, device identifiers, account identity, or every other location signal. Android does not let an ordinary app silently revoke other apps' location permissions or force all apps to report a VPN server's location.

## Development

The Android build configuration and implementation will be added in subsequent commits. Do not treat this planning repository as a functioning privacy or VPN application.
