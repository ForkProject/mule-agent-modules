/*
* (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
* law. All use of this software is subject to MuleSoft's Master Subscription Agreement
* (or other master license agreement) separately entered into in writing between you and
* MuleSoft. If such an agreement is not in place, you may not use the software.
*/

package com.mulesoft.agent.monitoring.publisher;

import com.mulesoft.agent.buffer.BufferedHandler;
import com.mulesoft.agent.configuration.Configurable;
import com.mulesoft.agent.domain.monitoring.Metric;
import com.mulesoft.agent.services.OnOffSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Handler that publishes JMX information obtained from the Monitoring Service to a running Graphite instance.
 * Utilizes Graphite plaintext protocol.
 * </p>
 */
@Named("mule.agent.graphite.jmx.internal.handler")
@Singleton
public class GraphiteMonitorPublisher extends BufferedHandler<List<Metric>>
{
    private final static Logger LOGGER = LoggerFactory.getLogger(GraphiteMonitorPublisher.class);

    /**
     * <p>
     * Prefix used to identify metrics as defined in Graphite's Carbon configuration.
     * </p>
     */
    @Configurable(value = "mule", description = "Prefix used to identify metrics as defined in Graphite's Carbon configuration.")
    String metricPrefix;

    /**
     * <p>
     * Address corresponding to Graphite's Carbon server.
     * </p>
     */
    @Configurable(value = "0.0.0.0", description = "Address corresponding to Graphite's Carbon server.")
    String graphiteServer;

    /**
     * <p>
     * Port corresponding to Graphite's Carbon server.
     * </p>
     */
    @Configurable(value = "2003", description = "Port corresponding to Graphite's Carbon server.")
    int graphitePort;

    @Inject
    public GraphiteMonitorPublisher()
    {
        super();
    }

    public GraphiteMonitorPublisher(OnOffSwitch enabledSwitch)
    {
        super();
        this.enabledSwitch = enabledSwitch;
    }

    @Override
    public boolean canHandle(@NotNull List<Metric> metrics)
    {
        return true;
    }

    @Override
    public boolean flush(@NotNull Collection<List<Metric>> listOfMetrics)
    {
        Socket graphiteConnection = null;
        OutputStreamWriter out = null;
        BufferedReader in = null;
        try
        {
            graphiteConnection = new Socket(graphiteServer, graphitePort);

            for (List<Metric> metrics : listOfMetrics)
            {

                for (Metric metric : metrics)
                {

                    StringBuilder message = new StringBuilder();
                    message.append(metricPrefix);
                    message.append(".");
                    message.append(metric.getName().replaceAll("\\s", "").replace(":", ""));
                    message.append(" ");
                    message.append(metric.getValue());
                    message.append(" ");
                    message.append((int) (metric.getTimestamp() / 1000L));
                    message.append("\n");

                    out = new OutputStreamWriter(graphiteConnection.getOutputStream());

                    out.write(message.toString());
                    out.flush();

                    in = new BufferedReader(new InputStreamReader(graphiteConnection.getInputStream()));
                    LOGGER.debug("Message sent to Graphite: " + message.toString());

                }
            }

            graphiteConnection.close();
        }
        catch (IOException e)
        {
            LOGGER.warn("Failed to establish connection to Graphite");
            return false;
        }
        finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                }
                if (out != null)
                {
                    out.close();
                }
                if (graphiteConnection != null)
                {
                    graphiteConnection.close();
                }
            } catch (IOException e)
            {

            }

        }
        return true;
    }
}
