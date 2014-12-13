package org.backmeup.index.core.elasticsearch;

import java.util.List;

class AvailableESInstanceState {

    private final PortList availableTCPPorts;
    private final PortList availableHttpPorts;

    public AvailableESInstanceState(List<Integer> availableTCPPorts, List<Integer> availableHttpPorts) {
        this.availableHttpPorts = new PortList(availableHttpPorts);
        this.availableTCPPorts = new PortList(availableTCPPorts);
    }

    public void addAvailableTCPPort(int port) {
        this.availableTCPPorts.add(port);
    }

    public void removeAvailableTCPPort(int port) {
        this.availableTCPPorts.remove(port);
    }

    public int useNextTCPPort() {
        return this.availableTCPPorts.next();
    }

    public void addAvailableHTTPPort(int port) {
        this.availableHttpPorts.add(port);
    }

    public void removeAvailableHTTPPort(int port) {
        this.availableHttpPorts.remove(port);
    }

    public int useNextHTTPPort() {
        return this.availableHttpPorts.next();
    }

}
