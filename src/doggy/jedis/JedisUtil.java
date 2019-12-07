package doggy.jedis;

import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.*;

public class JedisUtil {
    /**
     *
     * @param wrappedConnection
     * @return
     */
    public static String doPing(WrappedConnection wrappedConnection){
        return wrappedConnection.excuteCmd("PING").getMapResponse().get("data").toString();
    }

    /**
     *
     * @param wrappedConnection
     * @param password
     * @return
     */
    public static String doAuth(WrappedConnection wrappedConnection, String password){
        return wrappedConnection.excuteCmd("AUTH",new String[]{password}).getMapResponse().get("data").toString();
    }
    static String [] REDIS_COMMANDS = new String[]{
            "PING", "SET key value","GET key","QUIT","EXISTS key","DEL key","TYPE","FLUSHDB","KEYS *",
            "RANDOMKEY","RENAME","RENAMENX","RENAMEX","DBSIZE","EXPIRE","EXPIREAT",
            "TTL","SELECT","MOVE","FLUSHALL","GETSET","MGET","SETNX","SETEX",
            "MSET","MSETNX","DECRBY","DECR","INCRBY","INCR","APPEND","SUBSTR",
            "HSET","HGET","HSETNX","HMSET","HMGET","HINCRBY","HEXISTS","HDEL",
            "HLEN","HKEYS","HVALS","HGETALL","RPUSH","LPUSH","LLEN","LRANGE",
            "LTRIM","LINDEX","LSET","LREM","LPOP","RPOP","RPOPLPUSH","SADD",
            "SMEMBERS","SREM","SPOP","SMOVE","SCARD","SISMEMBER","SINTER","SINTERSTORE",
            "SUNION","SUNIONSTORE","SDIFF","SDIFFSTORE","SRANDMEMBER","ZADD","ZRANGE",
            "ZREM","ZINCRBY","ZRANK","ZREVRANK","ZREVRANGE","ZCARD","ZSCORE","MULTI",
            "DISCARD","EXEC","WATCH","UNWATCH","SORT","BLPOP","BRPOP","AUTH","SUBSCRIBE",
            "PUBLISH","UNSUBSCRIBE","PSUBSCRIBE","PUNSUBSCRIBE","PUBSUB","ZCOUNT","ZRANGEBYSCORE",
            "ZREVRANGEBYSCORE","ZREMRANGEBYRANK","ZREMRANGEBYSCORE","ZUNIONSTORE","ZINTERSTORE","ZLEXCOUNT",
            "ZRANGEBYLEX","ZREVRANGEBYLEX","ZREMRANGEBYLEX","SAVE","BGSAVE","BGREWRITEAOF","LASTSAVE",
            "SHUTDOWN","INFO","MONITOR","SLAVEOF","CONFIG","STRLEN","SYNC","LPUSHX",
            "PERSIST","RPUSHX","ECHO","LINSERT","DEBUG","BRPOPLPUSH","SETBIT","GETBIT",
            "BITPOS","SETRANGE","GETRANGE","EVAL","EVALSHA","SCRIPT","SLOWLOG","OBJECT",
            "BITCOUNT","BITOP","SENTINEL","DUMP","RESTORE","PEXPIRE","PEXPIREAT","PTTL",
            "INCRBYFLOAT","PSETEX","CLIENT","TIME","MIGRATE","HINCRBYFLOAT","SCAN","HSCAN",
            "SSCAN","ZSCAN","WAIT","CLUSTER","ASKING","PFADD","PFCOUNT","PFMERGE",
            "READONLY","GEOADD","GEODIST","GEOHASH","GEOPOS","GEORADIUS","GEORADIUSBYMEMBER",
            "BITFIELD"
    };
    public static SortedSet<String> CMD = new TreeSet<>();

    /**
     *
     * @return
     */
    public static SortedSet<String> getRedisCommand(){
        SortedSet<String> sortedSet = new TreeSet<>();
        sortedSet.addAll(Arrays.asList(REDIS_COMMANDS));
        CMD.addAll(sortedSet);
        return sortedSet;
    }

    /**
     *
     * @param wrappedConnection
     * @param cmds
     * @return
     */
    public static  List<String> executeCmd(WrappedConnection wrappedConnection, String cmds){
        List<String> result = new ArrayList<>();
        if (wrappedConnection.isBroken() || !wrappedConnection.isConnected()){
            return result;
        }
        String [] cmd = wrapCommands(cmds.toCharArray());
        Map<String, Object> responseMap;
        if (cmd.length == 1){
           responseMap =  wrappedConnection.excuteCmd(cmd[0]).getMapResponse();
        } else {
            String [] params = Arrays.copyOfRange(cmd,1, cmd.length);
            responseMap = wrappedConnection.excuteCmd(cmd[0], params).getMapResponse();
        }
        String dataType = responseMap.get("dataType").toString();
        if (WrappedProtocol.DATA_TYPE_LIST.equals(dataType)){//List类型的结果
            List data = (List)responseMap.get("data");
            if (data.size()==0){
                result.add("查无数据");
                return result;
            }
            if (data.get(0) instanceof ArrayList){
                for (int j=0;j<data.size();j++){
                    List list = (List)data.get(j);
                    result.add(j+" -> ");
                    axiba(result, list);
                }
            } else {
               axiba(result, data);
            }
        } else {
            result.add(cmd[0]+" -> "+responseMap.get("data").toString());
        }
        return result;
    }
    private static void axiba(List<String> result, List wuDaLang){
        String s = null;
        for (int i=0; i<wuDaLang.size(); i++){
            if ((i+1)%2 == 0){//value
                s = s.concat(wuDaLang.get(i).toString());
                result.add(s);
            } else {//key
                s = wuDaLang.get(i).toString()+" -> ";
            }
        }
    }

    /**
     *
     * @param bts
     * @return
     */
    private static String[] wrapCommands(char[] bts){
        List<String> list = new ArrayList<>();
        StringBuffer buffer = new StringBuffer();
        for (char bs: bts) {
            if (bs != 32) {
                buffer.append(bs);
            } else {
                list.add(buffer.toString());
                buffer = new StringBuffer();
            }
        }
        if (buffer.length()>0)
            list.add(buffer.toString());
        String [] strings = new String[list.size()];
        list.toArray(strings);
        return strings;
    }

    /**
     *
     * @param host
     * @param port
     * @return
     */
    public static WrappedConnection establishConnection(String host,String port) throws JedisConnectionException{
        WrappedConnection connection = new WrappedConnection(host,Integer.parseInt(port));
        connection.connect();
        return connection;

    }
}
