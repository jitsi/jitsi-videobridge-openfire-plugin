<%--
 _ Copyright @ 2015 Atlassian Pty Ltd
 _
 _ Licensed under the Apache License, Version 2.0 (the "License");
 _ you may not use this file except in compliance with the License.
 _ You may obtain a copy of the License at
 _
 _     http://www.apache.org/licenses/LICENSE-2.0
 _
 _ Unless required by applicable law or agreed to in writing, software
 _ distributed under the License is distributed on an "AS IS" BASIS,
 _ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 _ See the License for the specific language governing permissions and
 _ limitations under the License.
--%>
<%@ page import="org.jitsi.videobridge.openfire.*" %>
<%@ page import="org.jivesoftware.openfire.*" %>
<%@ page import="org.jivesoftware.util.*" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%

    boolean update = request.getParameter("update") != null;
    String errorMessage = null;
    Map<String, String> errors = new HashMap<>();
    String tcpPort;

    // Get handle on the plugin
    PluginImpl plugin = (PluginImpl) XMPPServer.getInstance().getPluginManager().getPlugin("jitsivideobridge");

    if (update)
    {
        String singlePort = request.getParameter("singleport");
        if (singlePort != null) {
            singlePort = singlePort.trim();
            try
            {
                int port = Integer.valueOf(singlePort);

                if(port >= 1 && port <= 65535)
                    JiveGlobals.setProperty(
                            PluginImpl.SINGLE_PORT_NUMBER_PROPERTY_NAME, singlePort);
                else
                    throw new NumberFormatException("out of range port");

            }
            catch (Exception e)
            {
                errorMessage = LocaleUtils.getLocalizedString(
                        "config.page.configuration.error.singleport",
                        "jitsivideobridge");
            }
        }

        String minPort = request.getParameter("minport");
        if (minPort != null) {
            minPort = minPort.trim();
            try
            {
                int port = Integer.valueOf(minPort);

                if(port >= 1 && port <= 65535)
                    JiveGlobals.setProperty(
                        PluginImpl.MIN_PORT_NUMBER_PROPERTY_NAME, minPort);
                else
                    throw new NumberFormatException("out of range port");

            }
            catch (Exception e)
            {
                errorMessage = LocaleUtils.getLocalizedString(
                    "config.page.configuration.error.minport",
                    "jitsivideobridge");
            }
        }

        String maxPort = request.getParameter("maxport");
        if (maxPort != null) {
            maxPort = maxPort.trim();
            try
            {
                int port = Integer.valueOf(maxPort);

                if(port >= 1 && port <= 65535)
                    JiveGlobals.setProperty(
                        PluginImpl.MAX_PORT_NUMBER_PROPERTY_NAME, maxPort);
                else
                    throw new NumberFormatException("out of range port");
            }
            catch (Exception e)
            {
                errorMessage = LocaleUtils.getLocalizedString(
                    "config.page.configuration.error.maxport",
                    "jitsivideobridge");
            }
        }

        String tcpEnabled = request.getParameter( "tcpEnabled" );
        if (tcpEnabled != null)
        {
            try
            {
                boolean enabled = Boolean.valueOf( tcpEnabled );
                JiveGlobals.setProperty( PluginImpl.TCP_ENABLED_PROPERTY_NAME, Boolean.toString( enabled ));
            }
            catch (Exception e)
            {
                errorMessage = "Unexpected value for tcpEnabled.";
            }
        }

        tcpPort = request.getParameter( "tcpPort" );
        if ( tcpPort == null || tcpPort.trim().isEmpty() )
        {
            JiveGlobals.deleteProperty( PluginImpl.TCP_PORT_PROPERTY_NAME );
        }
        else
        {
            try
            {
                int port = Integer.valueOf(maxPort.trim());
                if(port >= 1 && port <= 65535)
                {
                    JiveGlobals.setProperty( PluginImpl.TCP_PORT_PROPERTY_NAME, tcpPort.trim() );
                }
                else
                {
                    throw new NumberFormatException( "out of range port" );
                }
            }
            catch( Exception e )
            {
                errors.put( "tcpPort", "Invalid port value" );
            }
        }
    }
    else
    {
        tcpPort = plugin.getTcpPort() == null ? null : plugin.getTcpPort().toString();
    }

    Integer mappedTcpPort = null;
    boolean sslTcpEnabled = false;
%>
<html>
<head>
   <title><fmt:message key="config.page.title" /></title>

   <meta name="pageID" content="jitsi-videobridge-settings"/>
</head>
<body>
<% if (errorMessage != null) { %>
<div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
        <tbody>
        <tr>
            <td class="jive-icon"><img src="/images/error-16x16.gif" width="16" height="16" border="0" alt=""/></td>
            <td class="jive-icon-label">
                <%= errorMessage%>
            </td>
        </tr>
        </tbody>
    </table>
</div>
<br/>
<% } %>

<% if ( plugin.restartNeeded() ) { %>
<div class="jive-warning">
    <table cellpadding="0" cellspacing="0" border="0">
        <tbody>
        <tr>
            <td class="jive-icon"><img src="/images/warning-16x16.gif" width="16" height="16" border="0" alt=""/></td>
            <td class="jive-icon-label">
                <fmt:message key="config.page.configuration.restart-needed"/>
            </td>
        </tr>
        </tbody>
    </table>
</div>
<br/>
<% } %>

<p>
    <fmt:message key="config.page.description"/>
</p>
<form action="jitsi-videobridge.jsp" method="post">
    <div class="jive-contentBoxHeader">
        <fmt:message key="config.page.configuration.title"/>
    </div>
    <div class="jive-contentBox">
        <table cellpadding="0" cellspacing="0" border="0">
            <tbody>
            <tr>
                <td colspan="3" align="left"><fmt:message key="config.page.configuration.single.port.description"/></td>
            </tr>
            <tr>
                <td width="1%">&nbsp;</td>
                <td width="10%"><label class="jive-label" ><fmt:message key="config.page.configuration.single.port"/>:</label></td>
                <td align="left">
                    <input name="singleport" type="number" min="1" max="65535"
                           value="<%=plugin.getSinglePort()%>"/> <fmt:message key="config.page.configuration.udp"/>
                </td>
            </tr>
            <tr>
                <td colspan="3" align="left">&nbsp;</td>
            </tr>
            <tr>
                <td colspan="3" align="left"><fmt:message key="config.page.configuration.minmax.port.description"/></td>
            </tr>
            <tr>
                <td>&nbsp;</td>
                <td><label class="jive-label"><fmt:message key="config.page.configuration.min.port"/>:</label>
                </td>
                <td align="left">
                    <input name="minport" type="number" min="1" max="65535"
                           value="<%=plugin.getMinPort()%>"/> <fmt:message key="config.page.configuration.udp"/>
                </td>
            </tr>
            <tr>
                <td>&nbsp;</td>
                <td><label class="jive-label"><fmt:message key="config.page.configuration.max.port"/>:</label>
                </td>
                <td align="left">
                    <input name="maxport" type="number" min="1" max="65535"
                           value="<%=plugin.getMaxPort()%>"/> <fmt:message key="config.page.configuration.udp"/>
                </td>
            </tr>
            </tbody>
        </table>
    </div>

    <div class="jive-contentBoxHeader">
        <fmt:message key="config.page.configuration.tcp.title" />
    </div>
    <div class="jive-contentBox">
        <p>
            <fmt:message key="config.page.configuration.tcp.info"/>
        </p>
        <table cellpadding="3" cellspacing="0" border="0">
            <tbody>
            <tr>
                <td width="1%" nowrap>
                    <input type="radio" name="tcpEnabled" value="false" id="rb01" <%= (plugin.isTcpEnabled() ? "" : "checked") %>>
                </td>
                <td width="99%">
                    <label for="rb01">
                        <b><fmt:message key="config.page.configuration.tcp.disabled" /></b> <fmt:message key="config.page.configuration.tcp.disabled_info" />
                    </label>
                </td>
            </tr>
            <tr>
                <td width="1%" nowrap>
                    <input type="radio" name="tcpEnabled" value="true" id="rb02" <%= (plugin.isTcpEnabled()  ? "checked" : "") %>>
                </td>
                <td width="99%">
                    <label for="rb02">
                        <b><fmt:message key="config.page.configuration.tcp.enabled" /></b> <fmt:message key="config.page.configuration.tcp.enabled_info" />
                    </label>
                </td>
            </tr>
            <tr>
                <td width="1%" nowrap>
                    &nbsp;
                </td>
                <td width="99%">
                    <table cellpadding="3" cellspacing="0" border="0">
                        <tr>
                            <td colspan="2" style="padding-top: 1em;">
                                <fmt:message key="config.page.configuration.tcp.port_info"/>
                            </td>
                        </tr>
                        <tr>
                            <td width="1%" nowrap class="c1" style="padding-left: 1em;">
                                <b><fmt:message key="config.page.configuration.tcp.port" />:</b>
                            </td>
                            <td width="99%">
                                <input type="number" min="1" max="65535" name="tcpPort" value="<%= ((tcpPort != null) ? tcpPort : "") %>"> <fmt:message key="config.page.configuration.tcp" />

                                <%  if (errors.get("tcpPort") != null) { %>
                                <span class="jive-error-text">
                                    <fmt:message key="config.page.configuration.error.valid_port" />
                                </span>
                                <%  } %>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2" style="padding-top: 1em;">
                                <fmt:message key="config.page.configuration.tcp.mapped.port_info"/>
                            </td>
                        </tr>
                        <tr>
                            <td width="1%" nowrap class="c1" style="padding-left: 1em;">
                                <b><fmt:message key="config.page.configuration.tcp.mapped.port" />:</b>
                            </td>
                            <td width="99%">
                                <input type="number" min="1" max="65535" name="mappedTcpPort" value="<%= ((mappedTcpPort != null) ? mappedTcpPort : "") %>"> <fmt:message key="config.page.configuration.tcp" />

                                <%  if (errors.get("mappedPort") != null) { %>
                                <span class="jive-error-text">
                                    <fmt:message key="config.page.configuration.error.valid_port" />
                                </span>
                                <%  } %>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2" style="padding-top: 1em;">
                                <fmt:message key="config.page.configuration.ssltcp_info"/>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2" style="padding-left: 1em;">
                                <input type="radio" name="ssltcpEnabled" value="false" id="rb03" <%= (!sslTcpEnabled ? "checked" : "") %>>
                                <label for="rb03">
                                    <b><fmt:message key="config.page.configuration.ssltcp.disabled" /></b> <fmt:message key="config.page.configuration.ssltcp.disabled_info" />
                                </label>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2" style="padding-left: 1em;">
                                <input type="radio" name="ssltcpEnabled" value="true" id="rb04" <%= (sslTcpEnabled ? "checked" : "") %>>
                                <label for="rb04">
                                    <b><fmt:message key="config.page.configuration.ssltcp.enabled" /></b> <fmt:message key="config.page.configuration.ssltcp.enabled_info" />
                                </label>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
            </tbody>
        </table>

    </div>

    <input type="submit" name="update" value="<fmt:message key="config.page.configuration.submit" />">
</form>

</body>
</html>
