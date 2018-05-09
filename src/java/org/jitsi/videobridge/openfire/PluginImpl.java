/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.videobridge.openfire;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

import org.jitsi.meet.OSGi;
import org.jitsi.meet.OSGiBundleConfig;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.service.neomedia.*;
import org.jitsi.util.*;
import org.jitsi.videobridge.IceUdpTransportManager;
import org.jitsi.videobridge.xmpp.*;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.XMPPServerInfo;
import org.jivesoftware.openfire.container.*;
import org.jivesoftware.util.*;
import org.slf4j.*;
import org.slf4j.Logger;
import org.xmpp.component.*;

/**
 * Implements <tt>org.jivesoftware.openfire.container.Plugin</tt> to integrate
 * Jitsi Video Bridge into Openfire.
 *
 * @author Lyubomir Marinov
 * @author Damian Minkov
 */
public class PluginImpl
    implements Plugin,
               PropertyEventListener
{
    /**
     * The logger.
     */
    private static final Logger Log = LoggerFactory.getLogger(PluginImpl.class);

    /**
     * The name of the Openfire property that contains the single UDP port number that we'd
     * like our RTP managers to bind upon when using multiplexing of media streams.
     */
    public static final String SINGLE_PORT_NUMBER_PROPERTY_NAME
        = "org.jitsi.videobridge.media.SINGLE_PORT_HARVESTER_PORT";

    /**
     * The name of the Openfire property that contains the maximum UDP port number that we'd
     * like our RTP managers to bind upon when dynamically allocating ports for media streams.
     */
    public static final String MAX_PORT_NUMBER_PROPERTY_NAME
        = "org.jitsi.videobridge.media.MAX_PORT_NUMBER";

    /**
     * The name of the Openfire property that contains the minimum UDP port number that we'd
     * like our RTP managers to bind upon when dynamically allocating ports for media streams.
     */
    public static final String MIN_PORT_NUMBER_PROPERTY_NAME
        = "org.jitsi.videobridge.media.MIN_PORT_NUMBER";

    /**
     * The default UDP port value used when multiplexing multiple media streams.
     */
    public static final int SINGLE_PORT_DEFAULT_VALUE = 10000; // should be equal to org.jitsi.videobridge.IceUdpTransportManager.SINGLE_PORT_DEFAULT_VALUE

    /**
     * The minimum port number default value.
     */
    public static final int MIN_PORT_DEFAULT_VALUE = 10001;

    /**
     * The maximum port number default value.
     */
    public static final int MAX_PORT_DEFAULT_VALUE = 20000;

    /**
     * The <tt>ComponentManager</tt> to which the component of this
     * <tt>Plugin</tt> has been added.
     */
    private ComponentManager componentManager;

    /**
     * The <tt>Component</tt> that has been registered by this plugin. This
     * wraps the Videobridge service.
     */
    private ComponentImpl component;

    /**
     * The subdomain of the address of component with which it has been
     * added to {@link #componentManager}.
     */
    private String subdomain;

    /**
     * Changes to port number configuration require a restart of the plugin to take affect.
     * The single port number value that is currently in use is equal to the port number
     * that was configured when this plugin got initialized.
     */
    private int singlePortAtStartup = -1;

    /**
     * Changes to port number configuration require a restart of the plugin to take affect.
     * The minimum port number value that is currently in use is equal to the port number
     * that was configured when this plugin got initialized.
     */
    private int minPortAtStartup = -1;

    /**
     * Changes to port number configuration require a restart of the plugin to take affect.
     * The maximum port number value that is currently in use is equal to the port number
     * that was configured when this plugin got initialized.
     */
    private int maxPortAtStartup = -1;

    /**
     * Destroys this <tt>Plugin</tt> i.e. releases the resources acquired by
     * this <tt>Plugin</tt> throughout its life up until now and prepares it for
     * garbage collection.
     *
     * @see Plugin#destroyPlugin()
     */
    public void destroyPlugin()
    {
        PropertyEventDispatcher.removeListener(this);

        if ((componentManager != null) && (subdomain != null))
        {
            try
            {
                componentManager.removeComponent(subdomain);
            }
            catch (ComponentException ce)
            {
                Log.warn( "An unexpected exception occurred while " +
                          "destroying the plugin.", ce );
            }
            componentManager = null;
            component = null;
            subdomain = null;
        }
    }

    /**
     * Initializes this <tt>Plugin</tt>.
     *
     * @param manager the <tt>PluginManager</tt> which loads and manages this
     * <tt>Plugin</tt>
     * @param pluginDirectory the directory into which this <tt>Plugin</tt> is
     * located
     * @see Plugin#initializePlugin(PluginManager, File)
     */
    public void initializePlugin(PluginManager manager, File pluginDirectory)
    {
        PropertyEventDispatcher.addListener(this);

        try
        {
            checkNatives();
        }
        catch ( Exception e )
        {
            Log.warn( "An unexpected error occurred while checking the " +
                "native libraries.", e );
        }

        ComponentManager componentManager
            = ComponentManagerFactory.getComponentManager();
        String subdomain = ComponentImpl.SUBDOMAIN;

        // The ComponentImpl implementation expects to be an External Component,
        // which in the case of an Openfire plugin is untrue. As a result, most
        // of its constructor arguments are unneeded when the instance is
        // deployed as an Openfire plugin. None of the values below are expected
        // to be used (but where possible, valid values are provided for good
        // measure).
        final XMPPServerInfo info = XMPPServer.getInstance().getServerInfo();
        final String hostname = info.getHostname();
        final int port = -1;
        final String domain = info.getXMPPDomain();
        final String secret = null;

        // The ComponentImpl implementation depends on OSGI-based loading of
        // Components, which is prepared for here. Note that a configuration
        // is used that is slightly different from the default configuration
        // for Jitsi Videobridge: the REST API is not loaded.
        final OSGiBundleConfig osgiBundles = new JvbOpenfireBundleConfig();
        OSGi.setBundleConfig(osgiBundles);
        OSGi.setClassLoader(manager.getPluginClassloader(this));

        ComponentImpl component =
            new ComponentImpl( hostname, port, domain, subdomain, secret );

        try
        {
            componentManager.addComponent(subdomain, component);
            this.componentManager = componentManager;
            this.component = component;
            this.subdomain = subdomain;

            // Note that property setting uses an OSGi service that's only available after the component is started.
            //
            // TODO I suspect that there's a race condition here. When a client requests a socket before the changes
            //      below are applied, these new values might be ignored and an implementation default might be used
            //      instead.
            setPortProperty(
                IceUdpTransportManager.SINGLE_PORT_HARVESTER_PORT,
                JiveGlobals.getIntProperty(SINGLE_PORT_NUMBER_PROPERTY_NAME, SINGLE_PORT_DEFAULT_VALUE)
            );

            setPortProperty(
                DefaultStreamConnector.MAX_PORT_NUMBER_PROPERTY_NAME,
                JiveGlobals.getIntProperty(MAX_PORT_NUMBER_PROPERTY_NAME, MAX_PORT_DEFAULT_VALUE)
            );

            setPortProperty(
                DefaultStreamConnector.MIN_PORT_NUMBER_PROPERTY_NAME,
                JiveGlobals.getIntProperty(MIN_PORT_NUMBER_PROPERTY_NAME, MIN_PORT_DEFAULT_VALUE)
            );

            // Register this once
            singlePortAtStartup = getSinglePort();
            maxPortAtStartup = getMaxPort();
            minPortAtStartup = getMinPort();
        }
        catch (ComponentException ce)
        {
            Log.error( "An exception occurred when loading the plugin: " +
                "the component could not be added.", ce );
            this.componentManager = null;
            this.component = null;
            this.subdomain = null;
        }
    }

    /**
     * Returns the <tt>Component</tt> that has been registered by this plugin.
     * This wraps the Videobridge service.
     *
     * When the plugin is not running, <tt>null</tt> will be returned.
     *
     * @return The Videobridge component, or <tt>null</tt> when not running.
     */
    public ComponentImpl getComponent()
    {
        return component;
    }

    /**
     * Checks whether we have folder with extracted natives, if missing
     * find the appropriate jar file and extract them. Normally this is
     * done once when plugin is installed or updated.
     * If folder with natives exist add it to the java.library.path so
     * libjitsi can use those native libs.
     */
    private void checkNatives() throws Exception
    {
        // Find the root path of the class that will be our plugin lib folder.
        String binaryPath =
            (new URL(ComponentImpl.class.getProtectionDomain()
                .getCodeSource().getLocation(), ".")).openConnection()
                .getPermission().getName();

        File pluginJarfile = new File(binaryPath);
        File nativeLibFolder =
            new File(pluginJarfile.getParentFile(), "native");

        if(!nativeLibFolder.exists())
        {
            // lets find the appropriate jar file to extract and
            // extract it
            String jarFileSuffix = null;
            if ( OSUtils.IS_LINUX32 )
            {
                jarFileSuffix = "-native-linux-32.jar";
            }
            else if ( OSUtils.IS_LINUX64 )
            {
                jarFileSuffix = "-native-linux-64.jar";
            }
            else if ( OSUtils.IS_WINDOWS32 )
            {
                jarFileSuffix = "-native-windows-32.jar";
            }
            else if ( OSUtils.IS_WINDOWS64 )
            {
                jarFileSuffix = "-native-windows-64.jar";
            }
            else if ( OSUtils.IS_MAC )
            {
                jarFileSuffix = "-native-macosx.jar";
            }

            if ( jarFileSuffix == null )
            {
                Log.warn( "Unable to determine what the native libraries are " +
                    "for this OS." );
            }
            else if ( nativeLibFolder.mkdirs() )
            {
                String nativeLibsJarPath = pluginJarfile.getCanonicalPath();
                nativeLibsJarPath = nativeLibsJarPath.replaceFirst( "\\.jar",
                    jarFileSuffix );
                Log.debug("Applicable native jar: '{}'", nativeLibsJarPath);
                JarFile jar = new JarFile( nativeLibsJarPath );
                Enumeration en = jar.entries();
                while ( en.hasMoreElements() )
                {
                    try
                    {
                        JarEntry jarEntry = (JarEntry) en.nextElement();
                        if ( jarEntry.isDirectory() || jarEntry.getName()
                                                            .contains( "/" ) )
                        {
                            // Skip everything that's not in the root of the
                            // jar-file.
                            continue;
                        }
                        final File extractedFile = new File( nativeLibFolder,
                                                        jarEntry.getName() );
                        Log.debug( "Copying file '{}' from native library " +
                            "into '{}'.", jarEntry, extractedFile );

                        try ( InputStream is = jar.getInputStream( jarEntry );
                              FileOutputStream fos = new FileOutputStream(
                                  extractedFile ) )
                        {
                            while ( is.available() > 0 )
                            {
                                fos.write( is.read() );
                            }
                        }
                    }
                    catch ( Throwable t )
                    {
                        Log.warn( "An unexpected error occurred while copying" +
                            " native libraries.", t );
                    }
                }
                Log.info( "Native lib folder created and natives extracted" );
            }
            else
            {
                Log.warn( "Unable to create native lib folder." );
            }
        }
        else
            Log.info("Native lib folder already exist.");

        String newLibPath =
            nativeLibFolder.getCanonicalPath() + File.pathSeparator +
                System.getProperty("java.library.path");

        System.setProperty("java.library.path", newLibPath);

        // this will reload the new setting
        Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
        fieldSysPath.setAccessible(true);
        fieldSysPath.set(System.class.getClassLoader(), null);
    }

    /**
     * Returns the UDP port number that is used for multiplexing multiple media streams.
     *
     * Note that this method returns the configured value, which might differ from the configuration that is
     * in effect (as configuration changes require a restart to be taken into effect).
     *
     * @return a UDP port number value.
     */
    public int getSinglePort()
    {
        return LibJitsi.getConfigurationService().getInt(
            IceUdpTransportManager.SINGLE_PORT_HARVESTER_PORT,
            SINGLE_PORT_DEFAULT_VALUE
        );
    }

    /**
     * When multiplexing of media streams is not possible, the videobridge will automatically fallback to using
     * dynamically allocated UDP ports in a specific range. This method returns the upper-bound of that range.
     *
     * Note that this method returns the configured value, which might differ from the configuration that is
     * in effect (as configuration changes require a restart to be taken into effect).
     *
     * @return A UDP port number value.
     */
    public int getMaxPort()
    {
        return LibJitsi.getConfigurationService().getInt(
            DefaultStreamConnector.MAX_PORT_NUMBER_PROPERTY_NAME,
            MAX_PORT_DEFAULT_VALUE);
    }

    /**
     * When multiplexing of media streams is not possible, the videobridge will automatically fallback to using
     * dynamically allocated UDP ports in a specific range. This method returns the lower-bound of that range.
     *
     * Note that this method returns the configured value, which might differ from the configuration that is
     * in effect (as configuration changes require a restart to be taken into effect).
     *
     * @return A UDP port number value.
     */
    public int getMinPort()
    {
        return LibJitsi.getConfigurationService().getInt(
            DefaultStreamConnector.MIN_PORT_NUMBER_PROPERTY_NAME,
            MIN_PORT_DEFAULT_VALUE);
    }

    /**
     * A property was set. The parameter map <tt>params</tt> will contain the
     * the value of the property under the key <tt>value</tt>.
     *
     * @param property the name of the property.
     * @param params event parameters.
     */
    public void propertySet(String property, Map params)
    {
        if(property.equals(SINGLE_PORT_NUMBER_PROPERTY_NAME))
        {
            setPortProperty(
                IceUdpTransportManager.SINGLE_PORT_HARVESTER_PORT,
                (String)params.get("value"));
        }
        else if(property.equals(MAX_PORT_NUMBER_PROPERTY_NAME))
        {
            setPortProperty(
                DefaultStreamConnector.MAX_PORT_NUMBER_PROPERTY_NAME,
                (String)params.get("value"));
        }
        else if(property.equals(MIN_PORT_NUMBER_PROPERTY_NAME))
        {
            setPortProperty(
                DefaultStreamConnector.MIN_PORT_NUMBER_PROPERTY_NAME,
                (String)params.get("value"));
        }
    }

    /**
     * Sets int property.
     * @param property the property name.
     * @param value the value to change.
     */
    private void setPortProperty(String property, String value)
    {
        try
        {
            // let's just check that value is integer
            int port = Integer.valueOf(value);

            setPortProperty( property, port );
        }
        catch(NumberFormatException ex)
        {
            Log.error("Error setting port", ex);
        }
    }

    /**
     * Sets int property.
     * @param property the property name.
     * @param value the value to change.
     */
    private void setPortProperty(String property, int value)
    {
        if(value >= 1 && value <= 65535)
            LibJitsi.getConfigurationService().setProperty(property, Integer.toString( value ));
    }

    /**
     * A property was deleted.
     *
     * @param property the name of the property deleted.
     * @param params event parameters.
     */
    public void propertyDeleted(String property, Map params)
    {
        if(property.equals(SINGLE_PORT_NUMBER_PROPERTY_NAME))
        {
            LibJitsi.getConfigurationService().setProperty(
                IceUdpTransportManager.SINGLE_PORT_HARVESTER_PORT,
                String.valueOf(SINGLE_PORT_DEFAULT_VALUE));
        }
        else if(property.equals(MAX_PORT_NUMBER_PROPERTY_NAME))
        {
            LibJitsi.getConfigurationService().setProperty(
                DefaultStreamConnector.MAX_PORT_NUMBER_PROPERTY_NAME,
                String.valueOf(MAX_PORT_DEFAULT_VALUE));
        }
        else if(property.equals(MIN_PORT_NUMBER_PROPERTY_NAME))
        {
            LibJitsi.getConfigurationService().setProperty(
                DefaultStreamConnector.MIN_PORT_NUMBER_PROPERTY_NAME,
                String.valueOf(MIN_PORT_DEFAULT_VALUE));
        }
    }

    /**
     * An XML property was set. The parameter map <tt>params</tt> will contain
     * the value of the property under the key <tt>value</tt>.
     *
     * @param property the name of the property.
     * @param params event parameters.
     */
    public void xmlPropertySet(String property, Map params)
    {
        propertySet(property, params);
    }

    /**
     * An XML property was deleted.
     *
     * @param property the name of the property.
     * @param params event parameters.
     */
    public void xmlPropertyDeleted(String property, Map params)
    {
        propertyDeleted(property, params);
    }

    /**
     * Checks if the plugin requires a restart to apply pending configuration changes.
     *
     * @return true if a restart is needed to apply pending changes, otherwise false.
     */
    public boolean restartNeeded()
    {
        return singlePortAtStartup != getSinglePort() || maxPortAtStartup != getMaxPort() || minPortAtStartup != getMinPort();
    }
}
