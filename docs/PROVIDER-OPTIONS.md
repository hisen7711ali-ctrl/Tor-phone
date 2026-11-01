# External VPN provider and server options

Reviewed 2026-11-01. This document describes providers; it does not configure or certify a live server.

## Tor: Orbot companion
Tor does not require Tor-phone to rent private servers. For this phase the app launches Orbot, which can route all apps through Tor using Android's VPN mode. Follow TOR-INTEGRATION.md. Tor-phone and Orbot cannot each hold a separate active Android VPN interface at the same time.

Official Orbot download: https://orbot.app/en/download/

## Proton VPN Free
Proton currently advertises unlimited data/no time limits, one connected device, and free servers in 10 countries: Netherlands, Japan, Romania, Poland, Norway, Switzerland, Singapore, Mexico, Canada, and the United States. Free server selection is restricted compared with paid plans. This is a separate provider service and account; Tor-phone does not have permission to embed Proton's private server endpoints or reuse its app credentials. Users can install and use Proton VPN separately. If Tor-phone later integrates Proton, it must use an integration method explicitly supported by Proton.

Official plan details: https://protonvpn.com/support/proton-vpn-plans

## VPN Gate public volunteer relays
VPN Gate is a public volunteer relay project, not a managed privacy provider. Relay operators are independent, availability can change without notice, and its anti-abuse policy says VPN connection logs are kept for three or more months. The documented log fields include source IP/hostname, connection timestamps, server details, protocol and traffic metadata. Its FAQ notes that logging policies can also vary by server operator.

Treat VPN Gate as EXPERIMENTAL AND UNTRUSTED, not as the app's default. Do not promise anonymity or use it for sensitive traffic. If a user imports a VPN Gate OpenVPN profile in a future release, show a warning and require explicit confirmation.

Policy: https://www.vpngate.net/en/about_abuse.aspx
FAQ: https://www.vpngate.net/en/about_faq.aspx

## Add your own VPN server
A user may later import a profile issued by a provider or their own server:
- WireGuard: a valid .conf profile and its private key.
- OpenVPN: a valid .ovpn profile and any required certificate/key files.

The importer must validate format, store secrets locally using Android-backed secure storage where possible, exclude profiles from logs/backups, and never upload them to GitHub. A profile is not proof that a server is trustworthy. Tor-phone must verify connection state and perform DNS/IPv4/IPv6 leak tests before displaying protection.

## Decision
- Use Orbot for the current device-wide Tor companion flow.
- Proton VPN Free is a reasonable separate free VPN option for users who accept its limited server selection.
- VPN Gate is only an explicitly opt-in experimental relay source with a prominent logging/trust warning.
- Support user-owned/provider-issued server profiles after the actual VPN engine and secure importer are implemented.
- Never scrape random endpoints, embed fake addresses, or bundle credentials.
