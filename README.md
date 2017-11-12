# DigitalOcean-Console
Unwraps DigitalOcean web console into raw VNC

### Input required
- Droplet ID
- Cookie header
- Accept-Language header
- User-Agent header

The headers can be extracted from the web browser on the DigitalOcean console page.

### Files
`TigerVNCGUI` - A simple GUI made to launch TigerVNC on most Linux systems when connected to the DigitalOcean.

`TunnelServer` - A simple CLI to host a server that tunnels all incoming connections to DigitalOcean console.

`ConnectionTunnel` - The part that actually connects with DigitalOcean.
