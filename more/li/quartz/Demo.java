package li.quartz;

import li.ioc.Ioc;

public class Demo {
    /**
     * JUnit不支持多线程的测试,不能用于测试Quartz
     */
    public static void main(String[] args) {
        System.out.println(Ioc.get(Quartz.class));// 在Xml中配置li.quartz.Quartz为一个Bean,可以使Ioc启动时启动Quartz
    }
}