package org.backmeup.index.config;

import java.util.ArrayList;
import java.util.List;

public class AvailableESInstanceState {

	private List<Integer> availableTCPPorts = new ArrayList<>();
	private List<Integer> availableHttpPorts = new ArrayList<>();

	public AvailableESInstanceState(List<Integer> availableTCPPorts,
			List<Integer> availableHttpPorts) {
		if (availableHttpPorts != null) {
			this.availableHttpPorts = availableHttpPorts;
		}
		if (availableTCPPorts != null) {
			this.availableTCPPorts = availableTCPPorts;
		}
	}

	public void addAvailableTCPPort(int i) {
		if (!this.availableTCPPorts.contains(i)) {
			this.availableTCPPorts.add(i);
		}
	}

	public void removeAvailableTCPPort(int i) {
		if (this.availableTCPPorts.contains(i)) {
			int pos = this.availableTCPPorts.indexOf(i);
			this.availableTCPPorts.remove(pos);
		}
	}

	public void addAvailableHTTPPort(int i) {
		if (!this.availableHttpPorts.contains(i)) {
			this.availableHttpPorts.add(i);
		}
	}

	public void removeAvailableHTTPPort(int i) {
		if (this.availableHttpPorts.contains(i)) {
			int pos = this.availableHttpPorts.indexOf(i);
			this.availableHttpPorts.remove(pos);
		}
	}

	public int useNextHTTPPort() {
		if (this.availableHttpPorts != null
				&& this.availableHttpPorts.size() > 0) {
			int i = this.availableHttpPorts.get(0);
			removeAvailableHTTPPort(i);
			return i;
		}
		return -1;
	}

	public int useNextTCPPort() {
		if (this.availableTCPPorts != null && this.availableTCPPorts.size() > 0) {
			int i = this.availableTCPPorts.get(0);
			removeAvailableTCPPort(i);
			return i;
		}
		return -1;
	}
}
