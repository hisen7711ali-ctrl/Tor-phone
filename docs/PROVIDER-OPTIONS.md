# External VPN provider and server options

Reviewed 2026-11-03. This document describes provider constraints; it does not configure or certify a live server.

## Tor
Tor does not require Tor-phone to rent private VPN servers. The external companion-app integration has been removed. The target design is for Tor-phone to own one Android VPN service and integrate a maintained Tor engine and packet-routing bridge internally. That engine and bridge have not yet been selected or integrated.

## Proton VPN Free
Proton VPN Free is a separate provider service and account. Tor-phone does not have permission to embed Proton's private server endpoints or reuse its app credentials. Any future integration must use a method explicitly supported by the provider.

Official plan details: https://protonvpn.com/support/proton-vpn-plans

## VPN Gate public volunteer relays
VPN Gate is a public volunteer relay project, not a managed privacy provider. Relay operators are independent, availability can change without notice, and its anti-abuse policy says VPN connection logs are kept for three or more months. Treat it as experimental and untrusted, not as the app's default. Do not promise anonymity or use it for sensitive traffic.

Policy: https://www.vpngate.net/en/about_abuse.aspx
FAQ: https://www.vpngate.net/en/about_faq.aspx

## Add your own VPN server
A user may import a profile issued by a provider or their own server after a real VPN engine is selected:
- WireGuard: a valid .conf profile and its private key.
- OpenVPN: a valid .ovpn profile and any required certificate/key files.

The importer must validate format, store secrets locally using Android-backed secure storage where possible, exclude profiles from logs/backups, and never upload them to GitHub. A profile is not proof that a server is trustworthy. The app must verify connection state and perform DNS/IPv4/IPv6 leak tests before displaying protection.

## Decision constraints
- No external Tor companion app.
- No private provider endpoints or credentials copied from another provider's application.
- Public volunteer relays must be explicitly opt-in and clearly marked untrusted.
- Support user-owned/provider-issued server profiles only through a real VPN engine and secure importer.
- Never scrape random endpoints, embed fake addresses, or bundle credentials.
