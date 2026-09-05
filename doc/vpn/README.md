# VPN Access & Isolation Proof

The deployment is reachable **only** through the OpenVPN tunnel configured in
this directory. This document explains how to connect, and how to demonstrate
that the service is unreachable from the public internet (the assignment's
"isolation proof" requirement).

## Artifacts in this directory

| File                                       | Purpose                                                                                       |
|--------------------------------------------|-----------------------------------------------------------------------------------------------|
| `client01.ovpn`                            | OpenVPN client profile for the reviewer (server `49.51.242.10:1194`, UDP, `tun`, `tls-crypt`). |
| `openvpn-install-2.4.7-I607.exe`           | Windows installer for the OpenVPN GUI client.                                                 |
| `README.md`                                | This file.                                                                                    |
| `unreachable-proof.sh`                     | One-shot bash script that proves the service is not reachable from a non-VPN egress. Captures the result of a curl attempt that should fail with `Connection timed out` / `No route to host`. |

## Connecting

### Windows (recommended for the reviewer)

1. Install `openvpn-install-2.4.7-I607.exe`. Accept the defaults — the TAP
   adapter driver is required.
2. Copy `client01.ovpn` into `%USERPROFILE%\OpenVPN\config\` (or wherever
   OpenVPN-GUI is configured to look for profiles).
3. Right-click the OpenVPN-GUI tray icon → **Connect**. Wait for the icon to
   turn green.
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

`Tunnelblick` (macOS) or `NetworkManager`/`nmcli` (Linux) also work — just
import `client01.ovpn`.

## Reaching the service

Once the tunnel is up, the service is reachable at:

```
http://<vpn-internal-host>:8099/
```

`<vpn-internal-host>` is the private IP assigned by the OpenVPN server (typically
`10.8.0.x`). The reviewer is given the exact value separately.

A quick reachability probe:

```bash
curl -fsS --max-time 5 http://<vpn-internal-host>:8099/actuator/health
# {"status":"UP"}
```

## Isolation proof

The assignment explicitly requires evidence that the service is **not**
reachable from the public internet. The reviewer's host is the right place to
generate that evidence — it is the same network from which a curious attacker
or a careless deploy would try to reach the service.

### Scripted check

`unreachable-proof.sh` automates the negative test. Run it on any host that
**is not** connected to the VPN:

```bash
./unreachable-proof.sh <vpn-internal-host>
# or with the public IP fallback
./unreachable-proof.sh <vpn-internal-host> <public-ip-of-deploy-server>
```

Expected output:

```
[unreachable-proof] Target: <vpn-internal-host>:8099
[unreachable-proof] Probing without VPN ...
[unreachable-proof] PASS: connection refused / timed out as expected
        curl exit code: 7  (Couldn't connect to server)
        stderr: curl: (7) Failed to connect to <vpn-internal-host> port 8099: Connection timed out
```

A failure mode that would *not* satisfy the proof:

- `curl` returns `{"status":"UP"}` → the service is reachable without the
  VPN → the deployment is **not** isolated.
- `curl` returns any HTTP response (even an error from a public gateway) →
  the port is exposed to the public internet.

### Manual screenshot proof

In addition to the script, the reviewer is encouraged to capture two
screenshots side-by-side:

1. **VPN up** → `curl --max-time 5 http://<vpn-internal-host>:8099/actuator/health`
   returns `{"status":"UP"}`.
2. **VPN down** → same command fails with `Connection timed out` /
   `No route to host`, and `route print` / `ip route` show no entry to the
   service subnet.

These two screenshots together are the strongest evidence the assignment
asks for.

## Security notes

- The `.ovpn` file embeds a long-lived client certificate. Treat it like a
  password — anyone holding it can join the private network. The reviewer
  should not redistribute it beyond the assessment scope.
- `tls-auth` / `tls-crypt` is in place; the channel is authenticated and
  encrypted end-to-end.
- The OpenVPN server itself is not exposed by this repository — only the
  client-side artifacts needed for a reviewer to connect.