# External server/provider assessment

Reviewed on 2026-11-01. This is a selection note, not an endorsement or an active server configuration.

## Tor
Tor is a public relay network. Tor-phone should integrate a maintained Android Tor service/library; it does not need a private Tor server list or user-owned Tor VPS.

## Free VPN options

### Proton VPN Free
Proton advertises a free plan with unlimited data and free locations in the Netherlands, Japan, Romania, Poland, Norway, Switzerland, Singapore, Mexico, Canada, and the United States. Free users are assigned a fastest/free server and server changes are randomized with cooldown restrictions. This is a provider account/service, not a list of public WireGuard endpoints that this project may freely embed. The integration must use an officially supported provider interface or user-authorized configuration. See the provider's current free-plan documentation before implementation.

### VPN Gate public volunteer relays
VPN Gate publishes volunteer-operated public relays in many countries and provides OpenVPN profiles. It is free and can be useful for connectivity experiments, but it is not an appropriate default for a privacy product: relay operators are independent volunteers, availability changes, and the project's anti-abuse policy describes collection and retention of connection logs for three or more months, including source IP and connection metadata. Never label these relays trusted or anonymous. If ever added, put them behind a separate opt-in **Experimental / untrusted relay** warning and do not promise privacy.

## Decision
- Default recommendation: Tor via maintained Tor integration; VPN via a reputable provider's supported integration or a profile supplied by the user.
- Do not scrape random VPN endpoints or hard-code VPN Gate relays as trusted infrastructure.
- Do not promise free servers in every country. Free providers limit locations and server choice.
- Before implementing any provider adapter, verify its current Android SDK/API, terms, authentication model, and permission to integrate it into a third-party app.
- No live server credentials or endpoints are included in this repository.
