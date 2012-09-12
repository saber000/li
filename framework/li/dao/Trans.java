package li.dao;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import li.util.Log;

/**
 * 事务模版工具类
 * 
 * @author li (limw@w.cn)
 * @version 0.1.6 (2012-05-08)
 */
public abstract class Trans {
	private static final Log log = Log.init();

	/**
	 * 存储当前事务中使用到的Connection,为空意味不在事务中
	 */
	public static ThreadLocal<Map<Class<?>, Connection>> CONNECTION_MAP = new ThreadLocal<Map<Class<?>, Connection>>();

	/**
	 * 存储数据操作异常,不为null则代表出错,需要回滚
	 */
	public static ThreadLocal<Exception> EXCEPTION = new ThreadLocal<Exception>();

	/**
	 * 实例变量,用于存放一些值,可用于Trans内外通信
	 */
	private Map<String, Object> map = new HashMap<String, Object>();

	/**
	 * 实例变量,标记当前Trans是否被其他Trans包裹
	 */
	public Boolean inTrans = false;

	/**
	 * 定义一个事务,并执行run()中包裹的数据方法
	 */
	public Trans() {
		go();
	}

	/**
	 * 定义一个事务,可以指定是否自动执行,如果不,可以定义后调用go()以执行
	 * 
	 * @param auto_run 标记此事务是否自动执行
	 */
	public Trans(Boolean auto_run) {
		if (auto_run) {
			go();
		}
	}

	/**
	 * 执行这个事务,自动执行的Trans不需调用这个方法
	 */
	public Trans go() {
		try {
			begin(); // 开始事务
			run(); // 执行事务内方法
			if (null == EXCEPTION.get()) { // 如果没有出现错误
				commit(); // 提交事务
			} else {// 如果出现错误
				rollback(); // 回滚事务
			}
			end(); // 结束事务
		} catch (Exception e) {
			throw new RuntimeException("Exception in trans", e);
		}
		return this;
	}

	/**
	 * 抽象方法,包裹需要事务控制的Dao方法
	 */
	public abstract void run();

	/**
	 * 返回事务执行成功与否的标记
	 */
	public Boolean success() {
		return null == EXCEPTION.get();
	}

	/**
	 * 可以调用这个方法,向当前Trans的map中存入一些值
	 */
	public Trans set(String key, Object value) {
		map.put(key, value);
		return this;
	}

	/**
	 * 批量设置map,采用putAll方式
	 */
	public Trans set(Map<String, Object> map) {
		map.putAll(map);
		return this;
	}

	/**
	 * 可以调用这个方法,获取当前Trans的map中的值.
	 */
	public Object get(String key) {
		return map.get(key);
	}

	/**
	 * 返回当前Trans的map
	 */
	public Map<String, Object> get() {
		return map;
	}

	/**
	 * 开始事务,初始化CONNECTION_MAP,或者标记这个事务已被其他事务包裹融化
	 */
	private void begin() {
		if (null == CONNECTION_MAP.get()) { // Trans in Trans 时候不会重复执行
			log.debug(String.format("Trans.begin()  in %s.%s()  #%s", Thread.currentThread().getStackTrace()[5].getClassName(), Thread.currentThread().getStackTrace()[5].getMethodName(), Thread.currentThread().getStackTrace()[5].getLineNumber()));
			CONNECTION_MAP.set(new HashMap<Class<?>, Connection>());
		} else {
			this.inTrans = true;
			log.debug(String.format("Trans at %s.%s() #%s is melt", Thread.currentThread().getStackTrace()[5].getClassName(), Thread.currentThread().getStackTrace()[5].getMethodName(), Thread.currentThread().getStackTrace()[5].getLineNumber()));
		}
	}

	/**
	 * 捆绑提交当前事务中所有Connection中的事务,如果这个事务未在其他事务中的话
	 */
	private void commit() throws Exception {
		if (!inTrans && null != CONNECTION_MAP.get()) {
			for (Entry<Class<?>, Connection> connection : CONNECTION_MAP.get().entrySet()) {
				connection.getValue().commit();
				log.debug(String.format("Trans.commit() %s  in %s.%s()  #%s", connection.getValue(), Thread.currentThread().getStackTrace()[5].getClassName(), Thread.currentThread().getStackTrace()[5].getMethodName(), Thread.currentThread().getStackTrace()[5].getLineNumber()));
			}
		}
	}

	/**
	 * 回滚,捆绑回滚当前事务中所有Connection中的事务,如果这个事务未在其他事务中的话
	 */
	private void rollback() throws Exception {
		if (!inTrans && null != CONNECTION_MAP.get()) {
			for (Entry<Class<?>, Connection> connection : CONNECTION_MAP.get().entrySet()) {
				connection.getValue().rollback();
				log.error(String.format("Trans.rollback() %s  in %s.%s()  #%s", connection.getValue(), Thread.currentThread().getStackTrace()[5].getClassName(), Thread.currentThread().getStackTrace()[5].getMethodName(), Thread.currentThread().getStackTrace()[5].getLineNumber()));
			}
		}
	}

	/**
	 * 结束事务,并关闭当前事务中的所有Connection,如果这个事务未在其他事务中的话
	 */
	private void end() throws Exception {
		if (!inTrans && null != CONNECTION_MAP.get()) { // Trans in Trans 时候不会重复执行
			for (Entry<Class<?>, Connection> entry : CONNECTION_MAP.get().entrySet()) {
				entry.getValue().close();
				log.debug(String.format("Closing %s@%s", entry.getValue().getClass().getName(), Integer.toHexString(entry.getValue().hashCode())));
			}
			CONNECTION_MAP.set(null);
			log.debug(String.format("Trans.end()  in %s.%s()  #%s", Thread.currentThread().getStackTrace()[5].getClassName(), Thread.currentThread().getStackTrace()[5].getMethodName(), Thread.currentThread().getStackTrace()[5].getLineNumber()));
		}
	}
}