The Jitsi Video Bridge Openfire Plugin is a plugin for the [Openfire XMPP server](https://www.igniterealtime.org/openfire), which integrates the [Jitsi Videobridge](https://github.com/jitsi/jitsi-videobridge) XMPP component.

Building
--------

This project is using the Maven-based Openfire build process, as introduced in Openfire 4.2.0. To build this plugin locally, ensure that the following are available on your local host:

* A Java Development Kit, version 7 or (preferably) 8;
* Apache Maven 3

To build this project, invoke on a command shell:

    $ mvn clean package

Upon completion, the openfire plugin will be available in `target/jitsivideobridge.jar`

Installation
------------

Copy `jitsivideobridge.jar` into the plugins directory of your Openfire server, or use the Openfire Admin Console to upload the plugin. The plugin will then be automatically deployed. To upgrade to a new version, copy the new `jitsivideobridge.jar` file over the existing file.

Configuration
-------------

When the plugin is installed in Openfire, a new configuraiton page will appear Under `Server settings > Jitsi Videobridge`.
