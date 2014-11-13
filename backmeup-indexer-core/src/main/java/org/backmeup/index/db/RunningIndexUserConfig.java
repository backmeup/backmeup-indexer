package org.backmeup.index.db;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class RunningIndexUserConfig {

	@Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false)
	private Long userId;

	// ElasticSearch information
	private Integer tcpPort;
	private Integer httpPort;
	// hostaddress = host.getProtocol() + "://" + host.getHost()
	private String hostaddress;
	private String clusterName;

	// Truecrypt information
	private String mountedDriveLetter;
	private String mountedContainerLocation;

	// Timestamp created and last updated
	@Temporal(TemporalType.TIMESTAMP)
	private Date timestamp;

	public RunningIndexUserConfig() {
		this.timestamp = new Date();
	}

	public RunningIndexUserConfig(Long userId, URL hostaddress,
			Integer tcpPort, Integer httpPort, String clusterName,
			String mountedDrive, String mountedContainerLocation) {
		this.setUserID(userId);
		this.setTcpPort(tcpPort);
		this.setHttpPort(httpPort);
		this.setClusterName(clusterName);
		this.setMountedDriveLetter(mountedDrive);
		this.setMountedContainerLocation(mountedContainerLocation);
		this.setHostAddress(hostaddress);
		this.timestamp = new Date();
	}

	public String getMountedTCDriveLetter() {
		return this.mountedDriveLetter;
	}

	public void setMountedDriveLetter(String mountedDriveLetter) {
		this.mountedDriveLetter = mountedDriveLetter;
		this.timestamp = new Date();
	}

	public String getMountedContainerLocation() {
		return this.mountedContainerLocation;
	}

	public void setMountedContainerLocation(String mountedContainerLocation) {
		this.mountedContainerLocation = mountedContainerLocation;
		this.timestamp = new Date();
	}

	public Integer getTcpPort() {
		return this.tcpPort;
	}

	public void setTcpPort(Integer tcpPort) {
		this.tcpPort = tcpPort;
		this.timestamp = new Date();
	}

	public Integer getHttpPort() {
		return this.httpPort;
	}

	public void setHttpPort(Integer httpPort) {
		this.httpPort = httpPort;
		this.timestamp = new Date();
	}

	public URL getHostAddress() {
		try {
			return new URL(this.hostaddress);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public void setHostAddress(URL host) {
		if ((host.getProtocol() != null) && (host.getHost() != null)) {
			this.hostaddress = host.getProtocol() + "://" + host.getHost();
			this.timestamp = new Date();
		}
		if ((host.getPort() > -1)) {
			this.httpPort = host.getPort();
			this.timestamp = new Date();
		}

	}

	public String getClusterName() {
		return this.clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
		this.timestamp = new Date();
	}

	public Long getUserID() {
		return this.userId;
	}

	public void setUserID(Long userID) {
		this.userId = userID;
		this.timestamp = new Date();
	}

}
