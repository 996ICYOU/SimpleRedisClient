package doggy.jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisBusyException;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.exceptions.JedisNoScriptException;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

public class WrappedProtocol {
    private static final String ASK_RESPONSE = "ASK";
    private static final String MOVED_RESPONSE = "MOVED";
    private static final String CLUSTERDOWN_RESPONSE = "CLUSTERDOWN";
    private static final String BUSY_RESPONSE = "BUSY";
    private static final String NOSCRIPT_RESPONSE = "NOSCRIPT";
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 6379;
    public static final int DEFAULT_SENTINEL_PORT = 26379;
    public static final int DEFAULT_TIMEOUT = 2000;
    public static final int DEFAULT_DATABASE = 0;
    public static final String CHARSET = "UTF-8";
    public static final byte DOLLAR_BYTE = 36;
    public static final byte ASTERISK_BYTE = 42;
    public static final byte PLUS_BYTE = 43;
    public static final byte MINUS_BYTE = 45;
    public static final byte COLON_BYTE = 58;
    public static final String SENTINEL_MASTERS = "masters";
    public static final String SENTINEL_GET_MASTER_ADDR_BY_NAME = "get-master-addr-by-name";
    public static final String SENTINEL_RESET = "reset";
    public static final String SENTINEL_SLAVES = "slaves";
    public static final String SENTINEL_FAILOVER = "failover";
    public static final String SENTINEL_MONITOR = "monitor";
    public static final String SENTINEL_REMOVE = "remove";
    public static final String SENTINEL_SET = "set";
    public static final String CLUSTER_NODES = "nodes";
    public static final String CLUSTER_MEET = "meet";
    public static final String CLUSTER_RESET = "reset";
    public static final String CLUSTER_ADDSLOTS = "addslots";
    public static final String CLUSTER_DELSLOTS = "delslots";
    public static final String CLUSTER_INFO = "info";
    public static final String CLUSTER_GETKEYSINSLOT = "getkeysinslot";
    public static final String CLUSTER_SETSLOT = "setslot";
    public static final String CLUSTER_SETSLOT_NODE = "node";
    public static final String CLUSTER_SETSLOT_MIGRATING = "migrating";
    public static final String CLUSTER_SETSLOT_IMPORTING = "importing";
    public static final String CLUSTER_SETSLOT_STABLE = "stable";
    public static final String CLUSTER_FORGET = "forget";
    public static final String CLUSTER_FLUSHSLOT = "flushslots";
    public static final String CLUSTER_KEYSLOT = "keyslot";
    public static final String CLUSTER_COUNTKEYINSLOT = "countkeysinslot";
    public static final String CLUSTER_SAVECONFIG = "saveconfig";
    public static final String CLUSTER_REPLICATE = "replicate";
    public static final String CLUSTER_SLAVES = "slaves";
    public static final String CLUSTER_FAILOVER = "failover";
    public static final String CLUSTER_SLOTS = "slots";
    public static final String PUBSUB_CHANNELS = "channels";
    public static final String PUBSUB_NUMSUB = "numsub";
    public static final String PUBSUB_NUM_PAT = "numpat";
    public static final byte[] BYTES_TRUE = toByteArray(1);
    public static final byte[] BYTES_FALSE = toByteArray(0);

    public static final String DATA_TYPE_STATUS_CODE = "STATUS_CODE";
    public static final String DATA_TYPE_Object = "STRING";
    public static final String DATA_TYPE_LIST = "LIST";
    public static final String DATA_TYPE_INTERGER = "INTEGER";
    public static final String DATA_TYPE_ERR = "ERROR";
    public static final String DATA_TYPE_UNKNOW = "UNKNOW";

    public static void sendCommand(RedisOutputStream os, byte[] command, byte[][] args) {
        try {
            os.write((byte) 42);
            os.writeIntCrLf(args.length + 1);
            os.write((byte) 36);
            os.writeIntCrLf(command.length);
            os.write(command);
            os.writeCrLf();
            byte[][] var3 = args;
            int var4 = args.length;

            for (int var5 = 0; var5 < var4; var5++) {
                byte[] arg = var3[var5];
                os.write((byte) 36);
                os.writeIntCrLf(arg.length);
                os.write(arg);
                os.writeCrLf();
            }
        } catch (IOException var7) {
            throw new JedisConnectionException(var7);
        }
    }

    private static void processError(RedisInputStream is) {
        String message = is.readLine();

        if (message.startsWith("MOVED")) {
            String[] askInfo = parseTargetHostAndSlot(message);
            throw new JedisMovedDataException(message, new HostAndPort(askInfo[1], Integer.valueOf(askInfo[2]).intValue()), Integer.valueOf(askInfo[0]).intValue());
        }
        if (message.startsWith("ASK")) {
            String[] askInfo = parseTargetHostAndSlot(message);
            throw new JedisAskDataException(message, new HostAndPort(askInfo[1], Integer.valueOf(askInfo[2]).intValue()), Integer.valueOf(askInfo[0]).intValue());
        }
        if (message.startsWith("CLUSTERDOWN"))
            throw new JedisClusterException(message);
        if (message.startsWith("BUSY"))
            throw new JedisBusyException(message);
        if (message.startsWith("NOSCRIPT")) {
            throw new JedisNoScriptException(message);
        }
        throw new JedisDataException(message);
    }

    private static JedisDataException processResponseError(RedisInputStream is) {
        String message = is.readLine();

        if (message.startsWith("MOVED")) {
            String[] askInfo = parseTargetHostAndSlot(message);
            return new JedisMovedDataException(message, new HostAndPort(askInfo[1], Integer.valueOf(askInfo[2]).intValue()), Integer.valueOf(askInfo[0]).intValue());
        } else if (message.startsWith("ASK")) {
            String[] askInfo = parseTargetHostAndSlot(message);
            return new JedisAskDataException(message, new HostAndPort(askInfo[1], Integer.valueOf(askInfo[2]).intValue()), Integer.valueOf(askInfo[0]).intValue());
        } else if (message.startsWith("CLUSTERDOWN"))
            return new JedisClusterException(message);
        else if (message.startsWith("BUSY"))
            return new JedisBusyException(message);
        else if (message.startsWith("NOSCRIPT")) {
            return new JedisNoScriptException(message);
        }
        return new JedisDataException(message);
    }

    public static String readErrorLineIfPossible(RedisInputStream is) {
        byte b = is.readByte();
        return b != 45 ? null : is.readLine();
    }

    private static String[] parseTargetHostAndSlot(String clusterRedirectResponse) {
        String[] response = new String[3];
        String[] messageInfo = clusterRedirectResponse.split(" ");
        String[] targetHostAndPort = HostAndPort.extractParts(messageInfo[2]);
        response[0] = messageInfo[1];
        response[1] = targetHostAndPort[0];
        response[2] = targetHostAndPort[1];
        return response;
    }

    /**
     *
     * @param is
     * @return
     */
    private static Object process(RedisInputStream is) {
        byte b = is.readByte();
       /* if (b == 43)
            return processStatusCodeReply(is);
        if (b == 36)
            return processBulkReply(is);
        if (b == 42)
            return processMultiBulkReply(is);
        if (b == 58)
            return processInteger(is);
        if (b == 45) {
            processError(is);
            return null;
        }*/
        switch (b){
            case 43:
                return processStatusCodeReply(is);
            case 36:
                return processBulkReply(is);
            case 42:
                return processMultiBulkReply(is);
            case 58:
                return processInteger(is);
            case 45:
                processError(is);
                return null;

        }
        throw new JedisConnectionException("Unknown reply: " + (char) b);
    }

    /**
     * @param inputStream 从Redis的输入流把命令返回的结果统一包装为<br/>HashMap->{dataType:String,data:Object}
     * @return
     * @author 007
     */
    public static Map<String, Object> processMap(RedisInputStream inputStream) {
        byte bt = inputStream.readByte();
        Map response = new HashMap(2);
        switch (bt) {
            case 43:
                response.put("dataType", "STATUS_CODE");
                response.put("data", processStatusCodeReply(inputStream));
                return response;
            case 36:
                response.put("dataType", "STRING");
                response.put("data", processBulkReply(inputStream));
                return response;
            case 42:
                response.put("dataType", "LIST");
                response.put("data", processMultiBulkReply(inputStream));
                return response;
            case 58:
                response.put("dataType", "INTEGER");
                response.put("data", processInteger(inputStream));
                return response;
            case 45:
                response.put("dataType", "ERROR");
                response.put("data", processResponseError(inputStream).getMessage());
                return response;
        }
        response.put("dataType", "UNKNOW");
        response.put("data", "Unknow reply:" + (char) bt);
        return response;
    }

    private static byte[] processStatusCodeReply(RedisInputStream is) {
        return is.readLineBytes();
    }

    private static byte[] processBulkReply(RedisInputStream is) {
        int len = is.readIntCrLf();
        if (len == -1) {
            return null;
        }
        byte[] read = new byte[len];
        int size;
        for (int offset = 0; offset < len; offset += size) {
            size = is.read(read, offset, len - offset);
            if (size == -1) {
                throw new JedisConnectionException("It seems like server has closed the connection.");
            }
        }

        is.readByte();
        is.readByte();
        return read;
    }

    private static Long processInteger(RedisInputStream is) {
        return Long.valueOf(is.readLongCrLf());
    }

    private static List<Object> processMultiBulkReply(RedisInputStream is) {
        int num = is.readIntCrLf();
        if (num == -1) {
            return null;
        }
        List ret = new ArrayList(num);

        for (int i = 0; i < num; i++) {
            try {
                ret.add(process(is));
            } catch (JedisDataException var5) {
                ret.add(var5);
            }
        }

        return ret;
    }

    public static Object read(RedisInputStream is) {
        return process(is);
    }

    public static final byte[] toByteArray(boolean value) {
        return value ? BYTES_TRUE : BYTES_FALSE;
    }

    public static final byte[] toByteArray(int value) {
        return SafeEncoder.encode(String.valueOf(value));
    }

    public static final byte[] toByteArray(long value) {
        return SafeEncoder.encode(String.valueOf(value));
    }

    public static final byte[] toByteArray(double value) {
        if (Double.isInfinite(value)) {
            return value == (1.0D / 0.0D) ? "+inf".getBytes() : "-inf".getBytes();
        }
        return SafeEncoder.encode(String.valueOf(value));
    }

    public static enum Command {
        PING, SET, GET, QUIT, EXISTS, DEL, TYPE, FLUSHDB, KEYS,
        RANDOMKEY, RENAME, RENAMENX, RENAMEX, DBSIZE, EXPIRE, EXPIREAT,
        TTL, SELECT, MOVE, FLUSHALL, GETSET, MGET, SETNX, SETEX,
        MSET, MSETNX, DECRBY, DECR, INCRBY, INCR, APPEND, SUBSTR,
        HSET, HGET, HSETNX, HMSET, HMGET, HINCRBY, HEXISTS, HDEL,
        HLEN, HKEYS, HVALS, HGETALL, RPUSH, LPUSH, LLEN, LRANGE,
        LTRIM, LINDEX, LSET, LREM, LPOP, RPOP, RPOPLPUSH, SADD,
        SMEMBERS, SREM, SPOP, SMOVE, SCARD, SISMEMBER, SINTER, SINTERSTORE,
        SUNION, SUNIONSTORE, SDIFF, SDIFFSTORE, SRANDMEMBER, ZADD, ZRANGE,
        ZREM, ZINCRBY, ZRANK, ZREVRANK, ZREVRANGE, ZCARD, ZSCORE, MULTI,
        DISCARD, EXEC, WATCH, UNWATCH, SORT, BLPOP, BRPOP, AUTH, SUBSCRIBE,
        PUBLISH, UNSUBSCRIBE, PSUBSCRIBE, PUNSUBSCRIBE, PUBSUB, ZCOUNT, ZRANGEBYSCORE,
        ZREVRANGEBYSCORE, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZUNIONSTORE, ZINTERSTORE, ZLEXCOUNT,
        ZRANGEBYLEX, ZREVRANGEBYLEX, ZREMRANGEBYLEX, SAVE, BGSAVE, BGREWRITEAOF, LASTSAVE,
        SHUTDOWN, INFO, MONITOR, SLAVEOF, CONFIG, STRLEN, SYNC, LPUSHX,
        PERSIST, RPUSHX, ECHO, LINSERT, DEBUG, BRPOPLPUSH, SETBIT, GETBIT,
        BITPOS, SETRANGE, GETRANGE, EVAL, EVALSHA, SCRIPT, SLOWLOG, OBJECT,
        BITCOUNT, BITOP, SENTINEL, DUMP, RESTORE, PEXPIRE, PEXPIREAT, PTTL,
        INCRBYFLOAT, PSETEX, CLIENT, TIME, MIGRATE, HINCRBYFLOAT, SCAN, HSCAN,
        SSCAN, ZSCAN, WAIT, CLUSTER, ASKING, PFADD, PFCOUNT, PFMERGE,
        READONLY, GEOADD, GEODIST, GEOHASH, GEOPOS, GEORADIUS, GEORADIUSBYMEMBER,
        BITFIELD;
        public final byte[] raw = SafeEncoder.encode(name());
    }

    public static enum Keyword {
        AGGREGATE, ALPHA, ASC, BY, DESC, GET, LIMIT, MESSAGE, NO,
        NOSORT, PMESSAGE, PSUBSCRIBE, PUNSUBSCRIBE, OK, ONE, QUEUED, SET,
        STORE, SUBSCRIBE, UNSUBSCRIBE, WEIGHTS, WITHSCORES, RESETSTAT, RESET,
        FLUSH, EXISTS, LOAD, KILL, LEN, REFCOUNT, ENCODING, IDLETIME,
        AND, OR, XOR, NOT, GETNAME, SETNAME, LIST, MATCH, COUNT,
        PING, PONG;
        public final byte[] raw;

        private Keyword() {
            this.raw = SafeEncoder.encode(name().toLowerCase(Locale.ENGLISH));
        }
    }
}
