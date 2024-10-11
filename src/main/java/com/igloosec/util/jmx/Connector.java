package com.igloosec.util.jmx;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

class Connector {

    private final Logger log = LogManager.getLogger( Connector.class );

    private JMXConnector connector;
    private MemoryMXBean memoryMXBean;
    private RuntimeMXBean runtimeMXBean;
    private com.sun.management.OperatingSystemMXBean peOperatingSystemMXBean;
    private OperatingSystemMXBean operatingSystemMXBean;
    private ThreadMXBean threadMXBean;
    private final String url;

    private final AtomicLong previousJVMProcessCPUTime = new AtomicLong( 0 );
    private final AtomicLong previousJVMUpTime = new AtomicLong( 0 );

    protected Connector( String jmxURL ) { this.url = jmxURL; }

    protected boolean connect() {
        try {
            this.connector = JMXConnectorFactory.connect( new JMXServiceURL( this.url ) );
            getProxyMXBean();
            return true;
        } catch ( IOException ex ) {
            log.error( ex );
            return false;
        }
    }

    protected void disconnect() {
        try {
            this.connector.close();
        } catch ( IOException ex ) {
            log.error( ex );
        }
    }

    protected String[] getDomains() throws IOException {
        if( isConnected() == null || isConnected().isEmpty() ) throw new IOException();
        return this.connector.getMBeanServerConnection().getDomains();
    }

    protected Set<?> getObjectNames( ObjectName objectName ) throws IOException {
        if( isConnected() == null || isConnected().isEmpty() ) throw new IOException();
        return Sets.newHashSet( Optional.ofNullable( this.connector.getMBeanServerConnection().queryNames( objectName, null ) ).orElse( Sets.newHashSet() ) );
    }

    protected List<?> getAttributes( ObjectName objectName ) throws IntrospectionException, ReflectionException, InstanceNotFoundException, IOException {
        if( isConnected() == null || isConnected().isEmpty() ) throw new IOException();
        return Arrays.stream( Optional.ofNullable( this.connector.getMBeanServerConnection().getMBeanInfo( objectName ) )
                .map( MBeanInfo::getAttributes ).orElse( new MBeanAttributeInfo[] {null} ) )
                .map( MBeanAttributeInfo::getName ).collect( Collectors.toList() );
    }

    protected Object getAttributeValue( ObjectName objectName, String attrName ) throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, IOException {
        if( isConnected() == null || isConnected().isEmpty() ) throw new IOException();
        return this.connector.getMBeanServerConnection().getAttribute( objectName, attrName );
    }

    protected float getCPUUsage() throws IOException {
        if( isConnected() == null || isConnected().isEmpty() ) throw new IOException();

        long elapsedProcessCPUTime = this.peOperatingSystemMXBean.getProcessCpuTime() - this.previousJVMProcessCPUTime.get();
        long elapsedJVMUpTime = this.runtimeMXBean.getUptime() - this.previousJVMUpTime.get();

        BigDecimal totalElapsedJVMUpTime = BigDecimal.valueOf( elapsedJVMUpTime ).multiply( BigDecimal.valueOf( operatingSystemMXBean.getAvailableProcessors() ) );
        BigDecimal cpuUsage = BigDecimal.valueOf( elapsedProcessCPUTime ).divide( totalElapsedJVMUpTime.multiply( BigDecimal.valueOf( 10000 ) ), 2, BigDecimal.ROUND_HALF_UP );

        this.previousJVMProcessCPUTime.set( this.peOperatingSystemMXBean.getProcessCpuTime() );
        this.previousJVMUpTime.set( this.runtimeMXBean.getUptime() );

        return cpuUsage.floatValue();
    }

    protected long getMemoryTotal() {
        return this.peOperatingSystemMXBean.getTotalPhysicalMemorySize();
    }

    protected float getMemoryUsage() {
        BigDecimal memoryUsage = BigDecimal.valueOf( this.memoryMXBean.getHeapMemoryUsage().getUsed() ).divide( BigDecimal.valueOf( getMemoryTotal() ), 2, BigDecimal.ROUND_HALF_UP ).multiply( BigDecimal.valueOf( 100 ) );
        return memoryUsage.floatValue();
    }

    protected int getThreadCount() throws IOException {
        if( isConnected() == null || isConnected().isEmpty() ) throw new IOException();
        return this.threadMXBean.getThreadCount();
    }

    protected void getProxyMXBean() throws IOException {
        MBeanServerConnection server = this.connector.getMBeanServerConnection();
        memoryMXBean = ManagementFactory.newPlatformMXBeanProxy( server, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class );
        runtimeMXBean = ManagementFactory.newPlatformMXBeanProxy( server, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class );
        peOperatingSystemMXBean = ManagementFactory.newPlatformMXBeanProxy( server, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, com.sun.management.OperatingSystemMXBean.class );
        operatingSystemMXBean = ManagementFactory.newPlatformMXBeanProxy( server, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class );
        threadMXBean = ManagementFactory.newPlatformMXBeanProxy( server, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class );
    }

    protected Object invoke( ObjectName objectName, String operationName, Object params[], String signature[] ) throws ReflectionException, MBeanException, InstanceNotFoundException, IOException {
        if( isConnected() == null || isConnected().isEmpty() ) throw new IOException();
        return this.connector.getMBeanServerConnection().invoke( objectName, operationName, params, signature );
    }

    protected String isConnected() {
        try {
            return this.connector.getConnectionId();
        } catch ( IOException ex ) {
            log.error( ex );
            return null;
        }
    }
}
