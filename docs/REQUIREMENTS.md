# Tor-phone — Product Requirements

## Defaults
- Android-first, native Java.
- Device-wide routing is the target, subject to Android VPN and app-profile restrictions.
- VPN and Tor are separate modes; Tor-over-VPN is a later explicit option.
- Manual server/circuit changes by default.
- Prefer verified handover; if unavailable, keep the old tunnel or block traffic rather than silently bypass protection.
- Ask only for permissions needed by an enabled feature.

## VPN servers
Use a real provider with a documented Android SDK/API or a user-imported WireGuard profile. Do not ship invented endpoints, scraped public servers, shared credentials, or private keys. Free plans may limit countries, bandwidth, protocol, or simultaneous devices; never promise a free server in every country.

## Tor
Integrate a maintained Tor Android service/library through its supported interface. A Tor exit address is not a guaranteed selectable country. Circuit changes can interrupt sessions and app compatibility must be tested.

## Acceptance criteria
- UI connection state reflects the actual tunnel, never a button press.
- Clear error state on disconnect, failed handover, or provider failure.
- Tests cover IPv4, IPv6, DNS, sleep/wake, network changes and tunnel loss.
- No credentials or traffic contents in logs.
- No GPS permission for core VPN functionality.
