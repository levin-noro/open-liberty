/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Logging;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
public class JSONFieldsTest {
    private static final String MESSAGE_LOG = "logs/messages.log";
    private static final String SERVER_NAME_XML = "com.ibm.ws.logging.fieldnamexml";
    //env and bootstrap server initial config
    private static final String SERVER_NAME_ENV = "com.ibm.ws.logging.fieldnameenv";
    private static final String SERVER_NAME_BOOTSTRAP = "com.ibm.ws.logging.fieldnamebootstrap";
    private static final String SERVER_NAME_OMIT_ENV = "com.ibm.ws.logging.fieldnameenv";
    private static final String SERVER_NAME_OMIT_BOOTSTRAP = "com.ibm.ws.logging.fieldnameomitbootstrap";

    private static LibertyServer server_xml;
    private static LibertyServer server_env_rename;
    private static LibertyServer server_bootstrap_rename;
    private static LibertyServer server_env_omit;
    private static LibertyServer server_bootstrap_omit;

    private static LibertyServer serverInUse; // hold on to the server currently used so cleanUp knows which server to stop

    @BeforeClass
    public static void initialSetup() throws Exception {
        server_xml = LibertyServerFactory.getLibertyServer(SERVER_NAME_XML);
        server_env_rename = LibertyServerFactory.getLibertyServer(SERVER_NAME_ENV);
        server_bootstrap_rename = LibertyServerFactory.getLibertyServer(SERVER_NAME_BOOTSTRAP);
        server_env_omit = LibertyServerFactory.getLibertyServer(SERVER_NAME_OMIT_ENV);
        server_bootstrap_omit = LibertyServerFactory.getLibertyServer(SERVER_NAME_OMIT_BOOTSTRAP);

        // Preserve the original server configuration
        server_xml.saveServerConfiguration();
        server_env_rename.saveServerConfiguration();
        server_bootstrap_rename.saveServerConfiguration();
        server_env_omit.saveServerConfiguration();
        server_bootstrap_omit.saveServerConfiguration();
    }

    public void setUp(LibertyServer server) throws Exception {
        serverInUse = server;
        if (server != null && !server.isStarted()) {
            // Restore the original server configuration, before starting the server for each test case.
            server.restoreServerConfiguration();
            server.startServer();
        }
    }

    @After
    public void cleanUp() throws Exception {
        if (serverInUse != null && serverInUse.isStarted()) {
            serverInUse.stopServer("com.ibm.ws.logging.fat.ffdc.servlet.FFDCServlet.doGet", "ArithmeticException",
                                   "CWWKG0081E", "CWWKG0083W");
        }
    }

    /*
     * This test sets the "WLP_LOGGING_JSON_FIELDS" attribute in the server.env and verifies the property in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesEnv() throws Exception {
        setUp(server_env_rename);
        List<String> lines = server_env_rename.findStringsInFileInLibertyServerRoot("log3", MESSAGE_LOG);
        assertTrue("The message field name was not formatted in the new configuration in messages.log.", lines.size() > 0);
    }

    /*
     * This test sets the "WLP_LOGGING_JSON_FIELDS" attribute in the server.env and verifies the omission in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesOmitEnv() throws Exception {
        setUp(server_env_omit);
        //List<String> lines = server_env_omit.findStringsInFileInLibertyServerRoot("\"message\"", MESSAGE_LOG);
        //assertTrue("The message field name was not omitted in messages.log.", lines.size() == 0);
        assertNull("The message field name was not omitted in messages.log.", server_xml.waitForStringInLogUsingMark("\"loglevel\""));
    }

    /*
     * This test sets the "com.ibm.ws.logging.json.fields" attribute in the bootstrap.properties and verifies the property in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesBootstrap() throws Exception {
        setUp(server_bootstrap_rename);
        List<String> lines = server_bootstrap_rename.findStringsInFileInLibertyServerRoot("log2", MESSAGE_LOG);
        assertTrue("The message field name was not formatted in the new configuration in messages.log.", lines.size() > 0);
    }

    /*
     * This test sets the "com.ibm.ws.logging.json.fields" attribute in the bootstrap.properties and verifies the omission in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesOmitBootstrap() throws Exception {
        setUp(server_bootstrap_omit);
        //List<String> lines = server_bootstrap_omit.findStringsInFileInLibertyServerRoot("\"loglevel\"", MESSAGE_LOG);
        //assertTrue("The message field name was not omitted in messages.log.", lines.size() == 0);
        assertNull("The message field name was not omitted in messages.log.", server_xml.waitForStringInLogUsingMark("\"loglevel\""));
    }

    /*
     * This test sets the "jsonFields" attribute in the server.xml and verifies the property in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesXML() throws Exception {
        setUp(server_xml);
        setServerConfiguration(true, "message:test1", server_xml);

        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("test1", MESSAGE_LOG);
        assertTrue("The message field name was not formatted in the new configuration in messages.log.", lines.size() > 0);
        setServerConfiguration(true, "message:test2", server_xml);
        lines = server_xml.findStringsInFileInLibertyServerRoot("test2", MESSAGE_LOG);
        assertTrue("The message field name was not updated in messages.log.", lines.size() > 0);
    }

    /*
     * This test sets the "jsonFields" attribute in the server.xml and verifies the omission in the messages.log file.
     */
    @Test
    public void testMessageFieldNamesOmitXML() throws Exception {
        setUp(server_xml);

        setServerConfiguration(true, "message:", server_xml);
        server_xml.setMarkToEndOfLog();
        assertNull("The message field name was not omitted in messages.log.", server_xml.waitForStringInLogUsingMark("\"message\""));

        setServerConfiguration(true, "", server_xml);
        assertNotNull("The message field name was not re-added in messages.log.", server_xml.waitForStringInLogUsingMark("\"message\""));

        setServerConfiguration(true, "ibm_datetime:", server_xml);
        server_xml.setMarkToEndOfLog();
        assertNull("The message field name was not omitted in messages.log.", server_xml.waitForStringInLogUsingMark("\"ibm_datetime\""));

        setServerConfiguration(true, "", server_xml);
        assertNotNull("The message field name was not re-added in messages.log.", server_xml.waitForStringInLogUsingMark("\"ibm_datetime\""));
    }

    @Test
    public void jsonFieldsUnknownKey() throws Exception {
        // Set jsonFields property in server.xml
        setUp(server_xml);
        //provide an unknown fieldname
        setServerConfiguration(true, "testing:error", server_xml);

        //a warning should be given when a non-recognized key is provided
        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("TRAS3009W", MESSAGE_LOG);
        assertTrue("The default field name was not returned", lines.size() > 0);
    }

    @Test
    public void jsonFieldsTooManyTokens() throws Exception {
        // Set jsonFields property in server.xml
        setUp(server_xml);
        //provide an unknown fieldname
        setServerConfiguration(true, "provide:too:many:tokens", server_xml);

        //a warning should be given when an entry with too many or too few tokens is provided
        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("TRAS3008W", MESSAGE_LOG);
        assertTrue("The default field name was not returned", lines.size() > 0);
    }

    @Test
    public void jsonFieldsWrongEventType() throws Exception {
        // Set jsonFields property in server.xml
        setUp(server_xml);
        //provide an unknown fieldname
        setServerConfiguration(true, "notevent:message:log", server_xml);

        //a warning should be given when an entry with too many or too few tokens is provided
        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("TRAS3010W", MESSAGE_LOG);
        assertTrue("The default field name was not returned", lines.size() > 0);
    }

    @Test
    public void jsonFieldsOmitWrongEventType() throws Exception {
        //test if xml config event is wrong event type
        setUp(server_xml);

        setServerConfiguration(true, "wrongtype:message:", server_xml);
        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("TRAS3010W", MESSAGE_LOG);
        assertTrue("The given event type does not exist.", lines.size() > 0);
    }

    @Test
    public void jsonFieldsOmitWrongFieldName() throws Exception {
        //test if xml config field is wrong field
        setUp(server_xml);

        setServerConfiguration(true, "message:wrongfield:", server_xml);
        List<String> lines = server_xml.findStringsInFileInLibertyServerRoot("TRAS3009W", MESSAGE_LOG);
        assertTrue("The field name does not exist in the given event type.", lines.size() > 0);
    }

    @Test
    public void testRenameAndOmit() throws Exception {
        //test renaming, omission, and renaming again
        setUp(server_xml);

        //rename
        setServerConfiguration(true, "message:log2", server_xml);

        //check successful renaming
        assertNotNull("The message field name was not formatted in the new configuration in messages.log.", server_xml.waitForStringInLogUsingMark("log2"));

        //omit
        setServerConfiguration(true, "message:", server_xml);

        //check omission
        assertNull("The message field names were not omitted in messages.log.", server_xml.waitForStringInLogUsingMark("\"message\""));

        //rename
        setServerConfiguration(true, "message:log2", server_xml);

        //check successful renaming
        assertNotNull("The message field name was not formatted in the new configuration in messages.log.", server_xml.waitForStringInLogUsingMark("log2"));
    }

    @Test
    public void testOmitMultipleFields() throws Exception {
        //omit 2 fields, add one back
        setUp(server_xml);

        //omit message and datetime
        setServerConfiguration(true, "message:, ibm_datetime:", server_xml);
        server_xml.setMarkToEndOfLog();

        //check both fields are omitted
        assertNull("The message field names were not omitted in messages.log.", server_xml.waitForStringInLogUsingMark("\"message\".*\"ibm_datetime\""));

        //add datetime back
        setServerConfiguration(true, "message:", server_xml);

        //check datetime is added back
        assertNotNull("The message field name was not re-added in messages.log.", server_xml.waitForStringInLogUsingMark("\"ibm_datetime\""));
    }

    @Test
    public void testOmitForMessageType() throws Exception {
        setUp(server_xml);

        //omit datetime field for liberty_message event type
        setServerConfiguration(true, "message:ibm_datetime:", server_xml);
        server_xml.setMarkToEndOfLog();

        //check datetime is removed for liberty_message
        assertNull("The message field name was not omitted in messages.log.", server_xml.waitForStringInLogUsingMark("\"type\":\"liberty_message\".*\"ibm_datetime\""));

    }

    private static void setServerConfiguration(boolean isjsonFields, String newFieldName, LibertyServer server) throws Exception {
        Logging loggingObj;
        ServerConfiguration serverConfig = server.getServerConfiguration();
        loggingObj = serverConfig.getLogging();
        if (isjsonFields) {
            loggingObj.setjsonFields(newFieldName);
        }
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(serverConfig);
        server.waitForConfigUpdateInLogUsingMark(null);
    }

}
