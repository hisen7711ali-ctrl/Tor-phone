# Tor-phone — Security Plan

## Threat model
Reduce exposure of the public IP to destination services and protect traffic from local-network observers. This does not protect against a malicious provider, compromised device, account tracking, malware, or all traffic analysis.

## Trust boundaries
- VPN operators may observe metadata and, depending on service/protocol, destinations.
- Tor relays have different visibility; non-HTTPS exit traffic can be observed or modified.
- Android controls VPN activation and runtime permissions.
- Unknown public VPN endpoints are untrusted and must not be bundled as recommended infrastructure.

## Required controls
- TLS for provider/control APIs.
- No hard-coded keys, passwords, tokens, or server credentials.
- No traffic-content logging; redact identifiers in diagnostics.
- Validate imported profiles and reject unsupported configurations.
- Store app-managed secrets using platform-backed secure storage where applicable.
- Obtain explicit consent before VPN activation and profile import.
- Fail closed only where implemented and tested; do not advertise it prematurely.
- Minimize permissions and keep cleartext traffic disabled.

## GPS and location
An ordinary Android app cannot silently change other apps' permissions, disable global location services, or force apps to report the VPN server's location. Offer system-settings guidance and approximate-location education. Any mock-location option must be explicit, user-controlled, and labeled for testing; it is not a privacy guarantee.

## Verification checklist
1. Review manifest permissions and exported components.
2. Test VPN consent, revocation, process death, reboot, and network transitions.
3. Test IPv4, IPv6, DNS and app exclusion behavior.
4. Verify handover and tunnel-loss behavior.
5. Review provider ownership, privacy policy, jurisdiction and independent audits.
6. Publish no security claim until its tests pass.
