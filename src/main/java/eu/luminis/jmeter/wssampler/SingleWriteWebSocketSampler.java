/*
 * Copyright 2016, 2017 Peter Doornbosch
 *
 * This file is part of JMeter-WebSocket-Samplers, a JMeter add-on for load-testing WebSocket applications.
 *
 * JMeter-WebSocket-Samplers is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * JMeter-WebSocket-Samplers is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.luminis.jmeter.wssampler;

import eu.luminis.websocket.UnexpectedFrameException;
import eu.luminis.websocket.WebSocketClient;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class SingleWriteWebSocketSampler extends WebsocketSampler {

    private static final Logger log = LoggingManager.getLoggerForClass();


    public SingleWriteWebSocketSampler() {
        super.setName("Single Write WebSocket Sampler");
    }

    @Override
    protected WebSocketClient prepareWebSocketClient(SampleResult result) {
        if (getCreateNewConnection()) {
            dispose(threadLocalCachedConnection.get());
            try {
                URL url = new URL(getTLS()? "https": "http", getServer(), Integer.parseInt(getPort()), getPath());   // java.net.URL does not support "ws" protocol....
                return new WebSocketClient(url);
            } catch (MalformedURLException e) {
                // Impossible
                throw new RuntimeException();
            }
        }
        else {
            WebSocketClient wsClient = threadLocalCachedConnection.get();
            if (wsClient != null) {
                return wsClient;
            }
            else {
                log.error("Sampler '"+ getName() + "': there is no connection to re-use");
                result.setResponseCode("Sampler error");
                result.setResponseMessage("Sampler configured for using existing connection, but there is no connection");
                return null;
            }
        }
    }

    @Override
    protected Object doSample(WebSocketClient wsClient, SampleResult result) throws IOException, UnexpectedFrameException, SamplingAbortedException {
        if (getBinary()) {
            byte[] requestData;
            try {
                requestData = BinaryUtils.parseBinaryString(getRequestData());
            } catch (NumberFormatException noNumber) {
                // Thrown by BinaryUtils.parseBinaryString
                result.sampleEnd(); // End timimg
                log.error("Sampler '" + getName() + "': request data is not binary: " + getRequestData());
                result.setResponseCode("Sampler Error");
                result.setResponseMessage("Request data is not binary: " + getRequestData());
                throw new SamplingAbortedException();
            }
            // If the sendBinaryFrame method throws an IOException, some data may have been send, so we'd better register all request data
            result.setSamplerData(result.getSamplerData() + "\nRequest data:\n" + getRequestData() + "\n");
            wsClient.sendBinaryFrame(requestData);
        }
        else {
            result.setSamplerData(result.getSamplerData() + "\nRequest data:\n" + getRequestData() + "\n");
            wsClient.sendTextFrame(getRequestData());
        }

        return null;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    protected String validateArguments() {
        String errorMsg = null;
        if (getCreateNewConnection()) {
            errorMsg = validatePortNumber(getPort());
            if (errorMsg == null)
                errorMsg = validateConnectionTimeout(getConnectTimeout());
        }
        return errorMsg;
    }

    public String getServer() {
        return getPropertyAsString("server");
    }

    public void setServer(String server) {
        setProperty("server", server);
    }

    public String getPort() {
        return getPropertyAsString("port", "" + DEFAULT_WS_PORT).trim();
    }

    public void setPort(String port) {
        setProperty("port", port);
    }

    public String getPath() {
        return getPropertyAsString("path");
    }

    public void setPath(String path) {
        setProperty("path", path);
    }

    public String getRequestData() {
        return getPropertyAsString("requestData");
    }

    public void setRequestData(String requestData) {
        setProperty("requestData", requestData);
    }

    public boolean getBinary() {
        return getPropertyAsBoolean("binaryPayload");
    }

    public void setBinary(boolean binary) {
        setProperty("binaryPayload", binary);
    }

    public String toString() {
        return "WS Single Write sampler: " + getServer() + ":" + getPort() + getPath() + " - '" + getRequestData() + "'";
    }

    public boolean getCreateNewConnection() {
        return getPropertyAsBoolean("createNewConnection");
    }

    public void setCreateNewConnection(boolean value) {
        setProperty("createNewConnection", value);
    }


}
