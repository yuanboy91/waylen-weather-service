# VPN Access Guide

The deployed service is **only** accessible through the OpenVPN tunnel configured in this directory. This document describes how to connect.

## Files in This Directory

| File                             | Purpose                                                                              |
|----------------------------------|--------------------------------------------------------------------------------------|
| `client01.ovpn`                  | OpenVPN client profile (server `49.51.242.10:1194`, UDP, `tun`, `tls-crypt`).       |
| `openvpn-install-2.4.7-I607.exe`| Windows installer for the OpenVPN GUI client.                                        |
| `README.md`                      | This file.                                                                           |

## How to Connect

### Windows (Recommended)

1. Install `openvpn-install-2.4.7-I607.exe`. Accept the defaults — the TAP adapter driver is required.
2. Copy `client01.ovpn` into `%USERPROFILE%\OpenVPN\config\` (or the directory configured in OpenVPN-GUI).
3. Right-click the OpenVPN-GUI tray icon → **Connect**. Wait for the icon to turn green.
4. Verify the route was pushed:

   ```bash
   ipconfig /all | findstr /C:"IPv4" /C:"VPN"
   route print | findstr 10.
   ```

   You should see a `10.x.x.x` adapter and a route to the service subnet.

### macOS / Linux

Use any OpenVPN 2.4+ client:

```bash
sudo openvpn --config client01.ovpn
```

On macOS, `Tunnelblick` works as well. On Linux, you can use `NetworkManager` / `nmcli` — just import `client01.ovpn`.

## Accessing the Service

Once the tunnel is up, the service is reachable at:

```
http://10.8.0.1:8099/
```

`10.8.0.1` is the OpenVPN server's tun interface IP — it serves as the **gateway** for all VPN clients. Each client is assigned a private IP (typically `10.8.0.x`), and all traffic routed through the VPN tunnel has `10.8.0.1` as its next hop.

## Security Notes

- The `.ovpn` file embeds a long-lived client certificate. Treat it like a password — anyone holding it can join the private network. Do not redistribute it beyond the assessment scope.
- `tls-auth` / `tls-crypt` is enabled; the channel is authenticated and encrypted end-to-end.
