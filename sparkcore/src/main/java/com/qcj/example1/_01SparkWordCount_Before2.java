package com.qcj.example1;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import scala.Tuple2;

import java.util.Arrays;
import java.util.Iterator;

/*
 * 基于Java的Spark代码开发：
 * 使用不同的Spark版本，在api上面略有差异(主要的差异就是SparkContext的构建的差异)：
 *  spark2.x之前：
 *      需要手动new SparkContext
 *  spark2.x之后：
 *      需要构建SparkSession，使用sparksession来进行构建sparkcontext
 *      这个sparksession管理了sparkcontext，sqlcontext等等的创建。
 *  注意：
 *      java的api比scala的api一般多以java
 *  步骤：
 *      1、创建SparkContext对象
 *          加载关联的SparkConf对象
 *              A master URL must be set in your configuration
 *                  必须要制定一个spark作业的运行方式
 *              An application name must be set in your configuration
 *                  必须要制定一个spark作业的应用名称
 *          spark作业的运行方式：
 *              local，在本地运行，在本地创建SparkContext对象
 *                  local：给当前Spark作业只分配一个cpu core，一个线程运行,并行度是1
 *                  local[N]：给当前Spark作业只分配N个cpu core，N个线程运行,并行度是N
 *                  local[*]: 根据当前机器，自动分配线程个数
 *                  local[N, R]：比上述多了一个允许失败的次数，R次
 *              standalone: 并行度的设置需要在spark-submit脚本中进行设置
 *                  spark://bigdata01:7077
 *              yarn:   基于yarn的方式运行spark作业
 *                  yarn-cluster:
 *                      SparkContext的创建在yarn集群中
 *                  yarn-client:
 *                      SparkContext的创建在本地
 *                  在测试环境下，一般使用yarn-client，生产环境中一般使用yarn-cluster
 *              mesos（略）
 *                  mesos-cluster:
 *                  mesos-client:
 *              。。。
 *      2、加载外部数据源，产生对应RDD
 *      3、对该rdd进行各种操作
 *      4、关闭sparkContext
 */

/**
 * spark2.x之前：需要手动new SparkContext
 * 步骤：
 *      加载
 *      切分
 *      压平
 *      返回新数据（word,1）
 *      聚合(word,3)
 */
public class _01SparkWordCount_Before2 {
    public static void main(String[] args) {
        //SparkConf可以理解为mr中Configuration
        SparkConf conf = new SparkConf();
        //得到类的简写名称 https://www.cnblogs.com/bluestorm/p/6323391.html
        conf.setAppName(_01SparkWordCount_Before2.class.getSimpleName());
        conf.setMaster("local[2]");
        /*
         * 使用老的api方式进行创建
         * 在一个Spark Application中，只能有一个active的SparkContext
         */
        JavaSparkContext jsc = new JavaSparkContext(conf);
        /*
         * 加载外部数据源，产生对应的RDD
         * textFile：加载外部普通的文本文件，
         * 报错：
         *     Exception in thread "main" java.lang.IllegalArgumentException: java.net.URISyntaxException: Relative path in absolute URI: data:%5C1.txt
         * 解决方法：
         *     改为绝对路径
         */
        JavaRDD<String> linesRDD = jsc.textFile("D:/1.txt");
        //打印
       linesRDD.foreach(new VoidFunction<String>() {
            @Override
            public void call(String line) throws Exception {
                System.out.println(line);
            }
        });

        /*
         * 这些xxxFunction中的参数，有两部分，
         *  第一部分为输入，参考函数的调用者
         *  第二部分为输出，看我们想要什么类型
         */
        JavaRDD<String> wordsRDD = linesRDD.flatMap(new FlatMapFunction<String, String>() {
            @Override
            public Iterator<String> call(String line) throws Exception {
                //正则表达式 一个或者多个空格： + 一个或者多个 \s空格
                String[] fields = line.split("\\s+");
                return Arrays.asList(fields).iterator();
            }
        });
        //打印
        wordsRDD.foreach(new VoidFunction<String>() {
            @Override
            public void call(String word) throws Exception {
                System.out.println(word);
            }
        });
        //mapToPair:通过向该RDD的所有元素应用函数返回新的RDD
       JavaPairRDD<String,Integer> pairRDD =  wordsRDD.mapToPair(new PairFunction<String, String, Integer>() {
            @Override
            public Tuple2<String, Integer> call(String word) throws Exception {
                return new Tuple2<String,Integer>(word,1);//(word,1)
            }
        });
       //打印
        pairRDD.foreach(new VoidFunction<Tuple2<String, Integer>>() {
            @Override
            public void call(Tuple2<String, Integer> pair) throws Exception {
                System.out.println(pair._1+":"+pair._2);
            }
        });

        //聚合统计
        JavaPairRDD<String,Integer> rbkRDD = pairRDD.reduceByKey(new Function2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer v1, Integer v2) throws Exception {
                System.out.println("v1:" + v1 + "v2:" + v2);
                return v1 + v2;
            }
        });
        //打印
        rbkRDD.foreach(new VoidFunction<Tuple2<String, Integer>>() {
            @Override
            public void call(Tuple2<String, Integer> pair) throws Exception {
                System.out.println(pair._1+":"+pair._2);
            }
        });



        //关闭
        jsc.close();
    }
}
