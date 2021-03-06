backmeup-indexer
================
updated: 03.12.2014

BackMeUp v2 "Themis" Index Component. Provides a central interface for handling user specific index operations (mounting, sharing, ingesting, querying)

0) Required Software
====================
 - Linux / Windows support. [Tested under Ubuntu 12.04/14.04 (LTS) and Windows 7 64-bit]
 - Tomcat [Tested under v7.0.56]
 - Maven [Tested under v3.0.4]
 - PostgresSQL [Tested under v9.3 x86]
 - JDK v1.7 [Tested under Oracle JDK 1.7 and openJDK]

Specific to index-core
 - Truecrypt v7.1a [from https://www.grc.com/misc/truecrypt/truecrypt.htm]
 - Elasticsearch 1.4.0 [http://www.elasticsearch.org/downloads/1-4-0/]
 -- Elasticsearch Marvel Dashboard [https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.4.0.zip]

Elasticsearch + Marvel beide in Version 1.2.0 zum download zur Verfügung gestellt:
http://www.share-online.biz/dl/Q2AY2ZEN8ZP

For implicitly required software artifacts and version see the project's pom.xml files

1) Installing Elasticsearch 1.2.0 on Debian
===========================================

Download and install the Public Signing Key

wget -qO - http://packages.elasticsearch.org/GPG-KEY-elasticsearch | sudo apt-key add -

Add the following to your /etc/apt/sources.list to enable the repository

deb http://packages.elasticsearch.org/elasticsearch/1.4/debian stable main

Run apt-get update and the repository is ready for use. You can install it with :

apt-get update && apt-get install elasticsearch


Complete installer docu available at:
http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/setup-repositories.html#_yum

2) Installing Truecrypt
=======================
Get latest x64 or x32 component
wget https://www.grc.com/misc/truecrypt/truecrypt-7.1a-linux-console-x64.tar.gz

Unpack the archive
tar xzvf truecrypt-7.1a-linux-console-x64.tar.gz

Execute installer
sudo ./truecrypt-7.1a-setup-console-x64

3) Configuration
================
Make sure to install TrueCrypt v7.1a and ElasticSearch v1.4.0 on your operating system.
Go to backmeup-indexer-core>src>main>resources>backmeup-indexer_windows.properties or 
backmeup-indexer_linux.properties depending on your OS and edit according to your setup

Truecrypt
 - Linux:
   * installation defaults to /usr/bin - no settings required
   * volumes are mounted to /media/themis/ - make sure this mounting point is accessible
   * allow sudo to run without password for the current user (required for mounting)
 - Windows
   * truecrypt.home.dir = C:/Program Files/TrueCrypt
   * truecrypt.mountable.drives=I,J,K,L - a comma seperated list of mountable drives to use
 
 Elasticsearch
 - elasticsearch.home.dir = C:/Program Files/elasticsearch-1.4.0

 #a directory where Truecrypt container files are copied to when mounting them
 #as well as Elasticsearch yml files when starting the ES user instances
 index.temp.data.home.dir = /data/index-core/temp/indexuserspace

 #the root directory of the themis-datasink (dummy implementation)
 themis-datasink.home.dir = /data/index-core/datasink
 
----------------
Database Configuration: 
Connect to postgres on port 5432 and create the index-core database + db user
create a database user called 'dbu_indexcore' (pw 'dbu_indexcore')
create a database called 'bmuindexcore' and assign the dbu_indexcore user as owner

Info:
Make sure this information is reflected within src>main>resources>META-INF/persistence.xml

4.1) Deployment to Tomcat
=======================
To deploy backmeup-indexer call
* mvn clean install -DintegrationTests

Note:
The Maven integration test profile is automatically executed for this component when Truecrypt is detected in
C:/Program Files/TrueCrypt/TrueCrypt.exe or /usr/bin/truecrypt
To manually execute the backmeup-indexer integration tests call maven with '-P IntegrationTestsLinux' or '-P IntegrationTestsWindows'

4.2) Deployment of osgi bundles
=============================
* within backmeup-index/autodeploy directory you'll find the created osgi bundles + their dependencies which are required to run within the plugin-framework. 
Copy all bundle jars into /data/backmeup-service/autodeploy and /data/backmeup-worker/autodeploy [according to the configuration of backmeup-service manual section F2] 


Hints
=====
Set sudo without password for the current user
-----------------------------------------------
sudo echo "$USER ALL=(ALL) NOPASSWD:ALL" | sudo tee -a /etc/sudoers
Verify if you can use sudo without password ...
sudo cat /etc/sudoers | grep "$USER"
