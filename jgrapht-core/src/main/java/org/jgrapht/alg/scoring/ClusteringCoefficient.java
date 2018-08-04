/*
 * (C) Copyright 2018-2018, by Alexandru Valeanu and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package org.jgrapht.alg.scoring;

import org.jgrapht.Graph;
import org.jgrapht.GraphMetrics;
import org.jgrapht.alg.interfaces.VertexScoringAlgorithm;
import org.jgrapht.alg.util.NeighborCache;

import java.util.*;

/**
 * Clustering coefficient.
 *
 * <p>
 * Computes the <a href="https://en.wikipedia.org/wiki/Clustering_coefficient#Local_clustering_coefficient">local clustering coefficient</a>
 * of each vertex of a graph. The local clustering coefficient of a
 * node $v$ is given by the expression: $g(v)= \sum_{s \neq v \neq
 * t}\frac{\sigma_{st}(v)}{\sigma_{st}}$ where $\sigma_{st}$ is the total number of shortest paths
 * from node $s$ to node $t$ and $\sigma_{st}(v)$ is the number of those paths that pass through
 * $v$. For more details see
 * <a href="https://en.wikipedia.org/wiki/Clustering_coefficient">wikipedia</a>.
 *
 * <p>
 *     This implementation computes both the global, the local and the average clustering coefficient in an undirected
 *     or a directed network.
 * </p>
 *
 * <p>
 *     Global clustering coefficient was introduced in
 *     <i>R. D. Luce and A. D. Perry (1949). "A method of matrix analysis of group structure". Psychometrika. 14 (1):
 *     95–116. doi:10.1007/BF02289146</i>
 * </p>
 *
 * <p>
 *     Local clustering coefficient was introduced in
 *     <i>D. J. Watts and Steven Strogatz (June 1998). "Collective dynamics of 'small-world' networks". Nature.
 *     393 (6684): 440–442. doi:10.1038/30918</i>
 * </p>
 *
 * <p>
 * This implementation also computes the global clustering coefficient as well as the average clustering coefficient.
 *
 * <p>
 * The running time is $O(|V| + \Delta(G)^2)$ where $|V|$ is the number of vertices and $\Delta(G)$ is the maximum
 * degree of a vertex. The space complexity is $O(|V|)$.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Alexandru Valeanu
 */
public class ClusteringCoefficient<V, E> implements VertexScoringAlgorithm<V, Double> {

    /**
     * Underlying graph
     */
    private final Graph<V, E> graph;

    /**
     * The actual scores
     */
    private Map<V, Double> scores;

    /**
     * Global Clustering Coefficient
     */
    private boolean computed = false;
    private double globalClusteringCoefficient;

    /**
     * Average Clustering Coefficient
     */
    private boolean computedAverage = false;
    private double averageClusteringCoefficient;

    /**
     * Construct a new instance
     *
     * @param graph the input graph
     * @throws NullPointerException if {@code graph} is {@code null}
     */
    public ClusteringCoefficient(Graph<V, E> graph) {
        this.graph = Objects.requireNonNull(graph);
    }

    /**
     * Computes the global clustering coefficient. The global clustering coefficient $C$ is defined as
     * $C = 3 \times number\_of\_triangles / number\_of\_triplets$.
     *
     * <p>
     *     A triplet is three nodes that are connected by either two (open triplet) or
     *     three (closed triplet) undirected ties.
     * </p>
     *
     * @return the global clustering coefficient
     */
    public double getGlobalClusteringCoefficient() {
        if (!computed){
            computeGlobalClusteringCoefficient();
        }

        return globalClusteringCoefficient;
    }

    /**
     * Computes the average clustering coefficient. The average clustering coefficient $\={C}$ is defined as
     * $\={C} = \frac{\sum_{i=1}^{n} C_i}{n}$ where $n$ is the number of vertices.
     *
     * Note: the average is $0$ if the graph is empty
     *
     * @return the average clustering coefficient
     */
    public double getAverageClusteringCoefficient(){
        if (graph.vertexSet().size() == 0)
            return 0;

        if (!computedAverage){
            computeScoreMap();
            computedAverage = true;
            averageClusteringCoefficient = 0;

            for (double value: scores.values())
                averageClusteringCoefficient += value;

            averageClusteringCoefficient /= graph.vertexSet().size();
        }

        return averageClusteringCoefficient;
    }

    private void computeGlobalClusteringCoefficient(){
        NeighborCache<V, E> neighborCache = new NeighborCache<>(graph);
        computed = true;
        double numberTriplets = 0;

        for (V v: graph.vertexSet()){
            if (graph.getType().isUndirected()){
                numberTriplets += 1.0 * graph.degreeOf(v) * (graph.degreeOf(v) - 1) / 2;
            }
            else{
                numberTriplets += 1.0 * neighborCache.predecessorsOf(v).size() * neighborCache.successorsOf(v).size();
            }
        }

        globalClusteringCoefficient = 3 * GraphMetrics.getNumberOfTriangles(graph) / numberTriplets;
    }

    private void computeScoreMap(){
        if (scores != null)
            return;

        scores = new HashMap<>(graph.vertexSet().size());

        for (V v: graph.vertexSet()){
            NeighborCache<V, E> neighborCache = new NeighborCache<>(graph);
            Set<V> neighbourhood = neighborCache.neighborsOf(v);

            final double k = neighbourhood.size();
            double numberTriplets = 0;

            for (V p: neighbourhood)
                for (V q: neighbourhood)
                    if (graph.containsEdge(p, q))
                        numberTriplets++;

            if (k <= 1)
                scores.put(v, 0.0);
            else
                scores.put(v, numberTriplets / (k * (k - 1)));
        }
    }

    /**
     * Get a map with the local clustering coefficients of all vertices
     *
     * @return a map with all local clustering coefficients
     */
    @Override
    public Map<V, Double> getScores() {
        computeScoreMap();
        return Collections.unmodifiableMap(scores);
    }

    /**
     * Get a vertex's local clustering coefficient
     *
     * @param v the vertex
     * @return the local clustering coefficient
     */
    @Override
    public Double getVertexScore(V v) {
        if (!graph.containsVertex(v)) {
            throw new IllegalArgumentException("Cannot return score of unknown vertex");
        }

        computeScoreMap();
        return scores.get(v);
    }
}
