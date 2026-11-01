# Tor integration — Phase 2

## Implemented in this phase
Tor-phone can detect whether the Orbot Android package is installed and launch it. If it is missing, the dashboard opens Orbot's official Google Play listing.

This is a **companion-app integration**, not an embedded Tor engine. Orbot owns Android's single active VPN interface and performs the Tor routing. Tor-phone does not read Orbot's private status, control its service, or claim that the device is protected merely because Orbot was opened.

## Enable Tor for the phone
1. Install Orbot from the official source: https://orbot.app/en/download/
2. In Tor-phone, tap **OPEN ORBOT · ENABLE TOR**.
3. In Orbot, approve Android's VPN connection request.
4. Start Tor and wait until Orbot reports that it is connected/ready.
5. Open Orbot's VPN/app routing settings. Enable **VPN mode** and **Protect all apps** (wording may vary by release).
6. Check that Android shows the VPN key/status indicator. Verify the public IP from more than one app/browser before relying on it.
7. If desired, configure Android Settings → Network & internet → VPN → Orbot → Always-on VPN and **Block connections without VPN**. Test first: lockdown can cut off all network access if Tor fails.

Orbot's documentation describes routing all installed applications by default, with per-app selection available. Some apps, protocols, and connectivity features may not work through Tor.

## Critical Android limitation
Android generally allows one active VPN service per user/profile. Starting Orbot can replace another VPN app's tunnel. Tor-phone cannot run its own independent VpnService at the same time as Orbot. A future fully embedded Tor-phone engine would need to own one VpnService and integrate the Tor transport/routing stack inside that single service.

## No false security claims
- The current Tor-phone UI launches Orbot; it does not verify Orbot's live connection state.
- Do not show a green "protected" indicator until a trusted status integration and leak tests exist.
- Tor changes network routing and the exit IP visible to sites; it does not change GPS, device IDs, account identity, or all location signals.
- Tor is not a general-purpose speed VPN. Captchas, blocked services, and app incompatibility can occur.
