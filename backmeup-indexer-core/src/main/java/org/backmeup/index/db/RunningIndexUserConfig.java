package org.backmeup.index.db;

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
	private String transportaddress;
	private String clusterName;

	// Truecrypt information
	private String mountedDriveLetter;

	// Timestamp created and last updated
	@Temporal(TemporalType.TIMESTAMP)
	private Date timestamp;

	public RunningIndexUserConfig() {
		this.timestamp = new Date();
	}

	public RunningIndexUserConfig(Long userId, Integer tcpPort,
			Integer httpPort, String transportaddress, String clusterName,
			String mountedDrive) {
		this.setUserID(userId);
		this.setTcpPort(tcpPort);
		this.setHttpPort(httpPort);
		this.setClusterName(clusterName);
		this.setMountedDriveLetter(mountedDrive);
		this.timestamp = new Date();
	}

	public String getMountedDriveLetter() {
		return this.mountedDriveLetter;
	}

	public void setMountedDriveLetter(String mountedDriveLetter) {
		this.mountedDriveLetter = mountedDriveLetter;
	}

	public Integer getTcpPort() {
		return this.tcpPort;
	}

	public void setTcpPort(Integer tcpPort) {
		this.tcpPort = tcpPort;
	}

	public Integer getHttpPort() {
		return this.httpPort;
	}

	public void setHttpPort(Integer httpPort) {
		this.httpPort = httpPort;
	}

	public String getTransportaddress() {
		return this.transportaddress;
	}

	public void setTransportaddress(String transportaddress) {
		this.transportaddress = transportaddress;
	}

	public String getClusterName() {
		return this.clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public Long getUserID() {
		return this.userId;
	}

	public void setUserID(Long userID) {
		this.userId = userID;
	}

}
