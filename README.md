# DigitalOcean-Console
Unwraps DigitalOcean web console into raw VNC

### Input required
- Link to the console page
- Cookie header
- Accept-Language header
- User-Agent header

The headers can be extracted from the DigitalOcean console page with the web browser's developer tools.

### Files
`TigerVNCGUI` - A simple GUI made to launch TigerVNC on most Linux systems when connected to the DigitalOcean.

`TunnelServer` - A simple CLI to host a server that tunnels all incoming connections to DigitalOcean console.

`ConnectionTunnel` - The part that actually connects with DigitalOcean.
