package org.jitsi.videobridge.openfire;

import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.service.neomedia.DefaultStreamConnector;
import org.jitsi.videobridge.IceUdpTransportManager;

/**
 * Exposes various bits of Jitsi configuration.
 */
public class RuntimeConfiguration
{
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
     * The default setting for _disabling_ the TCP connectivity.
     */
    public static final boolean DISABLE_TCP_HARVESTER_DEFAULT_VALUE = false; // should be equal to the default behavior as implemented in org.jitsi.videobridge.IceUdpTransportManager

    /**
     * Changes to port number configuration require a restart of the plugin to take affect.
     * The single port number value that is currently in use is equal to the port number
     * that was configured when this plugin got initialized, which is what is stored in this field.
     */
    private static final int SINGLE_PORT_AT_STARTUP = RuntimeConfiguration.getSinglePort();

    /**
     * Changes to port number configuration require a restart of the plugin to take affect.
     * The minimum port number value that is currently in use is equal to the port number
     * that was configured when this plugin got initialized, which is what is stored in this field.
     */
    private static final int MIN_PORT_AT_STARTUP = RuntimeConfiguration.getMinPort();

    /**
     * Changes to port number configuration require a restart of the plugin to take affect.
     * The maximum port number value that is currently in use is equal to the port number
     * that was configured when this plugin got initialized, which is what is stored in this field.
     */
    private static final int MAX_PORT_AT_STARTUP = RuntimeConfiguration.getMaxPort();

    /**
     * Changes to TCP harvester availability require a restart of the plugin to take affect.
     * The current availability is equal to the availability  that was configured when this
     * plugin got initialized, which is what is stored in this field.
     */
    private static final boolean TCP_PORT_ENABLED_AT_STARTUP = RuntimeConfiguration.isTcpEnabled();

    /**
     * Changes to TCP harvester availability require a restart of the plugin to take affect.
     * The current TCP port number used is equal to the port number that was configured when this
     * plugin got initialized, which is what is stored in this field. Note: can be null.
     */
    private static final Integer TCP_PORT_AT_STARTUP = RuntimeConfiguration.getTcpPort();

    /**
     * Returns the UDP port number that is used for multiplexing multiple media streams.
     *
     * Note that this method returns the configured value, which might differ from the configuration that is
     * in effect (as configuration changes require a restart to be taken into effect).
     *
     * @return a UDP port number value.
     */
    public static int getSinglePort()
    {
        return LibJitsi.getConfigurationService().getInt( IceUdpTransportManager.SINGLE_PORT_HARVESTER_PORT, SINGLE_PORT_DEFAULT_VALUE );
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
    public static int getMaxPort()
    {
        return LibJitsi.getConfigurationService().getInt( DefaultStreamConnector.MAX_PORT_NUMBER_PROPERTY_NAME, MAX_PORT_DEFAULT_VALUE );
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
    public static int getMinPort()
    {
        return LibJitsi.getConfigurationService().getInt( DefaultStreamConnector.MIN_PORT_NUMBER_PROPERTY_NAME, MIN_PORT_DEFAULT_VALUE );
    }

    /**
     * Jitsi Videobridge can accept and route RTP traffic over TCP. If enabled, TCP addresses will automatically be
     * returned as ICE candidates via COLIBRI. Typically, the point of using TCP instead of UDP is to simulate HTTP
     * traffic in a number of environments where it is the only allowed form of communication.
     *
     * Note that this method returns the configured value, which might differ from the configuration that is
     * in effect (as configuration changes require a restart to be taken into effect).
     *
     * @return A boolean value that indicates if the videobridge is configured to allow RTP traffic over TCP.
     */
    public static boolean isTcpEnabled()
    {
        // Jitsi uses a 'disable' option here. We should negate their setting.
        return !LibJitsi.getConfigurationService().getBoolean( IceUdpTransportManager.DISABLE_TCP_HARVESTER, DISABLE_TCP_HARVESTER_DEFAULT_VALUE );
    }

    /**
     * Returns the TCP port number that is used for multiplexing multiple media streams over TCP, or null if the default
     * is to be used..
     *
     * Note that this method returns the configured value, which might differ from the configuration that is
     * in effect (as configuration changes require a restart to be taken into effect).
     *
     * @return a TCP port number value, possibly null.
     */
    public static Integer getTcpPort()
    {
        final int value = LibJitsi.getConfigurationService().getInt( IceUdpTransportManager.TCP_HARVESTER_PORT, -1 );

        if ( value == -1 )
        {
            return null;
        }

        return value;
    }

    /**
     * Checks if the plugin requires a restart to apply pending configuration changes.
     *
     * @return true if a restart is needed to apply pending changes, otherwise false.
     */
    public static boolean restartNeeded()
    {
        return
        SINGLE_PORT_AT_STARTUP != RuntimeConfiguration.getSinglePort()
        || MAX_PORT_AT_STARTUP != RuntimeConfiguration.getMaxPort()
        || MIN_PORT_AT_STARTUP != RuntimeConfiguration.getMinPort()
        || TCP_PORT_ENABLED_AT_STARTUP != RuntimeConfiguration.isTcpEnabled()
        || ( TCP_PORT_AT_STARTUP == null && RuntimeConfiguration.getTcpPort() != null) || ( TCP_PORT_AT_STARTUP != null && !TCP_PORT_AT_STARTUP.equals( RuntimeConfiguration.getTcpPort() ) );
    }
}
