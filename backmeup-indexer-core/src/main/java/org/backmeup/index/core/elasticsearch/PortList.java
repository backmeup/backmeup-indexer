package org.backmeup.index.core.elasticsearch;

import java.util.List;

class PortList {

    private final List<Integer> ports;

    public PortList(List<Integer> ports) {
        this.ports = ports;
    }

    public void add(int port) {
        if (!ports.contains(port)) {
            ports.add(port);
        }
    }

    public void remove(int port) {
        if (ports.contains(port)) {
            int pos = ports.indexOf(port);
            ports.remove(pos);
        }
    }

    public int next() {
        if (ports.size() > 0) {
            return ports.remove(0);
        }
        throw new IllegalStateException("no more free ports");
    }

}