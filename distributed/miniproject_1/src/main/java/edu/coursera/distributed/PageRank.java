package edu.coursera.distributed;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.spark.api.java.JavaPairRDD;
import scala.Tuple2;

/**
 * A wrapper class for the implementation of a single iteration of the iterative
 * PageRank algorithm.
 */
public final class PageRank {
    /**
     * Default constructor.
     */
    private PageRank() {
    }

    /**
     * TODO Given an RDD of websites and their ranks, compute new ranks for all
     * websites and return a new RDD containing the updated ranks.
     *
     * Recall from lectures that given a website B with many other websites
     * linking to it, the updated rank for B is the sum over all source websites
     * of the rank of the source website divided by the number of outbound links
     * from the source website. This new rank is damped by multiplying it by
     * 0.85 and adding that to 0.15. Put more simply:
     *
     *   new_rank(B) = 0.15 + 0.85 * sum(rank(A) / out_count(A)) for all A linking to B
     *
     * For this assignment, you are responsible for implementing this PageRank
     * algorithm using the Spark Java APIs.
     *
     * The reference solution of sparkPageRank uses the following Spark RDD
     * APIs. However, you are free to develop whatever solution makes the most
     * sense to you which also demonstrates speedup on multiple threads.
     *
     *   1) JavaPairRDD.join
     *   2) JavaRDD.flatMapToPair
     *   3) JavaPairRDD.reduceByKey
     *   4) JavaRDD.mapValues
     *
     * @param sites The connectivity of the website graph, keyed on unique
     *              website IDs.
     * @param ranks The current ranks of each website, keyed on unique website
     *              IDs.
     * @return The new ranks of the websites graph, using the PageRank
     *         algorithm to update site ranks.
     */
    public static JavaPairRDD<Integer, Double> sparkPageRank(
            final JavaPairRDD<Integer, Website> sites,
            final JavaPairRDD<Integer, Double> ranks) {
        return sites
                .join(ranks)
                .flatMapToPair(kv -> {
                    final Website site = kv._2._1;
                    final Double rank = kv._2._2;
                    return toStream(site.edgeIterator())
                            .map(id -> new Tuple2<>(id, rank / site.getNEdges()))
                            .collect(Collectors.toList());
                })
                .reduceByKey(Double::sum)
                .mapValues(v -> 0.15 + 0.85 * v);
    }

    private static <T> Stream<T> toStream(final Iterator<T> it) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false);
    }
}