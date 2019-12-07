package doggy.jedis;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.IOUtils;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class WrappedConnection {
    private static final byte[][] EMPTY_ARGS = new byte[0][];
    private String host = "localhost";
    private int port = 6379;
    private Socket socket;
    private RedisOutputStream outputStream;
    private RedisInputStream inputStream;
    private int pipelinedCommands = 0;
    private int connectionTimeout = 5000;
    private int soTimeout = 10000;
    private boolean broken = false;
    private boolean ssl;
    private SSLSocketFactory sslSocketFactory;
    private SSLParameters sslParameters;
    private HostnameVerifier hostnameVerifier;

    public WrappedConnection() {
    }

    /**
     *
     * @param host
     */
    public WrappedConnection(String host) {
        this.host = host;
    }

    /**
     *
     * @param host
     * @param port
     */
    public WrappedConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     *
     * @param host
     * @param port
     * @param ssl
     */
    public WrappedConnection(String host, int port, boolean ssl) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
    }

    /**
     *
     * @param host
     * @param port
     * @param ssl
     * @param sslSocketFactory
     * @param sslParameters
     * @param hostnameVerifier
     */
    public WrappedConnection(String host, int port, boolean ssl, SSLSocketFactory sslSocketFactory, SSLParameters sslParameters, HostnameVerifier hostnameVerifier) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.sslSocketFactory = sslSocketFactory;
        this.sslParameters = sslParameters;
        this.hostnameVerifier = hostnameVerifier;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public int getConnectionTimeout() {
        return this.connectionTimeout;
    }

    public int getSoTimeout() {
        return this.soTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public void setTimeoutInfinite() {
        try {
            if (!isConnected()) {
                connect();
            }

            this.socket.setSoTimeout(0);
        } catch (SocketException var2) {
            this.broken = true;
            throw new JedisConnectionException(var2);
        }
    }

    public void rollbackTimeout() {
        try {
            this.socket.setSoTimeout(this.soTimeout);
        } catch (SocketException var2) {
            this.broken = true;
            throw new JedisConnectionException(var2);
        }
    }

    public WrappedConnection excuteCmd(String cmd, String[] args) {
        return sendCommand(SafeEncoder.encode(cmd), args);
    }

    public WrappedConnection excuteCmd(String cmd) {
        return sendCommand(SafeEncoder.encode(cmd));
    }

    protected WrappedConnection sendCommand(byte[] cmd, String[] args) {
        byte[][] bargs = new byte[args.length][];

        for (int i = 0; i < args.length; i++) {
            bargs[i] = SafeEncoder.encode(args[i]);
        }

        return sendCommand(cmd, bargs);
    }

    protected WrappedConnection sendCommand(byte[] cmd) {
        return sendCommand(cmd, EMPTY_ARGS);
    }

    private WrappedConnection sendCommand(byte[] cmd, byte[][] args) {
        try {
            connect();
            WrappedProtocol.sendCommand(this.outputStream, cmd, args);
            this.pipelinedCommands += 1;
            return this;
        } catch (JedisConnectionException var6) {
            JedisConnectionException ex = var6;
            try {
                String errorMessage = WrappedProtocol.readErrorLineIfPossible(this.inputStream);
                if ((errorMessage != null) && (errorMessage.length() > 0))
                    ex = new JedisConnectionException(errorMessage, ex.getCause());
            } catch (Exception localException) {
            }
            this.broken = true;
            throw ex;
        }
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     *
     */
    public void connect() {
        if (!isConnected())
            try {
                this.socket = new Socket();
                this.socket.setReuseAddress(true);
                this.socket.setKeepAlive(true);
                this.socket.setTcpNoDelay(true);
                this.socket.setSoLinger(true, 0);
                this.socket.connect(new InetSocketAddress(this.host, this.port), this.connectionTimeout);
                this.socket.setSoTimeout(this.soTimeout);
                if (this.ssl) {
                    if (null == this.sslSocketFactory) {
                        this.sslSocketFactory = ((SSLSocketFactory) SSLSocketFactory.getDefault());
                    }

                    this.socket = ((SSLSocket) this.sslSocketFactory.createSocket(this.socket, this.host, this.port, true));
                    if (null != this.sslParameters) {
                        ((SSLSocket) this.socket).setSSLParameters(this.sslParameters);
                    }

                    if ((null != this.hostnameVerifier) && (!this.hostnameVerifier.verify(this.host, ((SSLSocket) this.socket).getSession()))) {
                        String message = String.format("The connection to '%s' failed ssl/tls hostname verification.", new Object[]{this.host});
                        throw new JedisConnectionException(message);
                    }
                }

                this.outputStream = new RedisOutputStream(this.socket.getOutputStream());
                this.inputStream = new RedisInputStream(this.socket.getInputStream());
            } catch (IOException var2) {
                this.broken = true;
                throw new JedisConnectionException(var2);
            }
    }

    public void close() {
        disconnect();
    }

    /**
     *
     */
    public void disconnect() {
        if (isConnected())
            try {
                this.outputStream.flush();
                this.socket.close();
            } catch (IOException var5) {
                this.broken = true;
                throw new JedisConnectionException(var5);
            } finally {
                IOUtils.closeQuietly(this.socket);
            }
    }

    /**
     *
     * @return
     */
    public boolean isConnected() {
        return (this.socket != null) && (this.socket.isBound()) && (!this.socket.isClosed()) && (this.socket.isConnected()) && (!this.socket.isInputShutdown()) && (!this.socket.isOutputShutdown());
    }

    public String getStatusCodeReply() {
        flush();
        this.pipelinedCommands -= 1;
        byte[] resp = (byte[]) (byte[]) readProtocolWithCheckingBroken();
        return null == resp ? null : SafeEncoder.encode(resp);
    }

    public String getBulkReply() {
        byte[] result = getBinaryBulkReply();
        return null != result ? SafeEncoder.encode(result) : null;
    }

    public byte[] getBinaryBulkReply() {
        flush();
        this.pipelinedCommands -= 1;
        return (byte[]) (byte[]) readProtocolWithCheckingBroken();
    }

    public Long getIntegerReply() {
        flush();
        this.pipelinedCommands -= 1;
        return (Long) readProtocolWithCheckingBroken();
    }

    public List<String> getMultiBulkReply() {
        return (List) BuilderFactory.STRING_LIST.build(getBinaryMultiBulkReply());
    }

    public List<byte[]> getBinaryMultiBulkReply() {
        flush();
        this.pipelinedCommands -= 1;
        return (List) readProtocolWithCheckingBroken();
    }

    public void resetPipelinedCount() {
        this.pipelinedCommands = 0;
    }

    public List<Object> getRawObjectMultiBulkReply() {
        return (List) readProtocolWithCheckingBroken();
    }

    public List<Object> getObjectMultiBulkReply() {
        flush();
        this.pipelinedCommands -= 1;
        return getRawObjectMultiBulkReply();
    }

    public List<Long> getIntegerMultiBulkReply() {
        flush();
        this.pipelinedCommands -= 1;
        return (List) readProtocolWithCheckingBroken();
    }

    public List<Object> getAll() {
        return getAll(0);
    }

    public List<Object> getAll(int except) {
        List all = new ArrayList();
        flush();

        for (; this.pipelinedCommands > except; this.pipelinedCommands -= 1) {
            try {
                all.add(readProtocolWithCheckingBroken());
            } catch (JedisDataException var4) {
                all.add(var4);
            }
        }

        return all;
    }

    public Object getOne() {
        flush();
        this.pipelinedCommands -= 1;
        return readProtocolWithCheckingBroken();
    }

    public boolean isBroken() {
        return this.broken;
    }

    public Object readObject() {
        return readProtocolWithCheckingBroken();
    }

    protected void flush() {
        try {
            this.outputStream.flush();
        } catch (IOException var2) {
            this.broken = true;
            throw new JedisConnectionException(var2);
        }
    }

    protected Object readProtocolWithCheckingBroken() {
        try {
            return WrappedProtocol.read(this.inputStream);
        } catch (JedisConnectionException var2) {
            this.broken = true;
            throw var2;
        }
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getMapResponse() {
        try {
            flush();
            this.pipelinedCommands -= 1;
            Map map = WrappedProtocol.processMap(this.inputStream);
            String dataType = map.get("dataType").toString();
            Object data = map.get("data");
            switch (dataType) {
                case "STATUS_CODE":
                    byte[] resp = (byte[]) (byte[]) data;
                    String statusCode = null == resp ? "null" : SafeEncoder.encode(resp);
                    map.put("data", statusCode);
                    return map;
                case "INTEGER":
                    Long dt = (Long) data;
                    map.put("data", dt);
                    return map;
                case "LIST":
                    List list = (List) data;
                    if ((list.size() > 0) && ((list.get(0) instanceof byte[])))
                        map.put("data", BuilderFactory.STRING_LIST.build(data));
                    else if ((list.size() > 0) && ((list.get(0) instanceof ArrayList)))
                        map.put("data", wrapList(list));
                    else {
                        map.put("data", list);
                    }
                    return map;
                case "STRING":
                    byte[] bts = (byte[]) data;
                    map.put("data", null == bts ? "" : SafeEncoder.encode(bts));
                    return map;
            }
            return map;
        } catch (JedisConnectionException co) {
            Map err = new HashMap();
            err.put("dataType", "STRING");
            err.put("data", co.getLocalizedMessage());
            return err;
        }
    }

    /**
     *
     * @param data
     * @return
     */
    private List<List<Object>> wrapList(List data) {
        List listList = new ArrayList();

        for (int i = 0; i < data.size(); i++) {
            List innerList = (List) data.get(i);
            List valueList = new ArrayList();
            for (Iterator localIterator = innerList.iterator(); localIterator.hasNext(); ) {
                Object object = localIterator.next();
                if ((object instanceof Long)) {
                    valueList.add(((Long) object).toString());
                } else if ((object instanceof byte[])) {
                    byte[] bts = (byte[]) object;
                    valueList.add(null == bts ? "" : SafeEncoder.encode(bts));
                } else if ((object instanceof ArrayList)) {
                    List a = (List) BuilderFactory.STRING_LIST.build(object);
                    valueList.add(a);
                }
            }
            listList.add(valueList);
        }
        return listList;
    }
}
