backmeup-indexer
================
updated: 30.10.2014

BackMeUp v2 "Themis" Index Component. Provides a central interface for handling user specific index operations (mounting, sharing, ingesting, querying)

0) Required Software
====================
 - Linux / Windows support. [Tested under Ubuntu 12.04/14.04 (LTS) and Windows 7 64-bit]
 - Tomcat [Tested under v7.0.56]
 - Maven [Tested under v3.0.4]
 - PostgresSQL [Tested under  v9.3 x86]
 - JDK v1.7 [Tested under Oracle JDK 1.7 and openJDK]
 
 Specific to the index-core
 - Truecrypt v7.1a [from https://www.grc.com/misc/truecrypt/truecrypt.htm]
 - Elasticsearch 1.2.0 [http://www.elasticsearch.org/downloads/1-2-0/]
 
For implicitly required software artefacts and version see the project's pom.xml files
   
TODO continue...

Hints
=====
Set sudo without password for the current user
-----------------------------------------------
sudo echo "$USER ALL=(ALL) NOPASSWD:ALL" | sudo tee -a /etc/sudoers
Verify if you can use sudo without password ...
sudo cat /etc/sudoers | grep "$USER"
