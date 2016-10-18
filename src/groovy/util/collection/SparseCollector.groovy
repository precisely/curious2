package util.collection

/**
 *
 * SparseCollector
 *
 * This is a sparse matrix designed to collect up incremental association strength data
 *
 * For example, suppose you want to sum up a bunch of interactions between two products in a shopping site, for instance
 * these are products which were purchased by the same user at the same time. Every time this happens, we want to
 * increment a record in the matrix by a certain weight, to indicate that this event increases the weight of the association
 * between the products (the idea being that products purchased by the same user at the same time are likely to be related).
 *
 * However, it is inefficient to preallocate an array of every-product x every-product
 *
 * Also, you may wish to collect up this information in multiple dimensions, and try models that use different weights for
 * different types of associations. So instead of just summing up a single weight, we allow multiple dimensions of summing
 *
 * Since the association matrix should be symmetrical (an interaction between product A and B should be the same as an
 * interaction between product B and A, i.e., the relation is commutative), this sparse matrix will only be on the upper
 * diagonal. We enforce this in the increment method, below.
 *
 * Copyright (c) 2016 Mitsu Hadeishi <mitsu.hadeishi@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
class SparseCollector {
    int maxType
    int sizeIds

    Map<Integer, Map<Integer, CollectorNode>> maps

    SparseCollector(int maxType = 1, int sizeIds = 1000) {
        this.maxType = maxType
        this.sizeIds = sizeIds

        maps = new HashMap(sizeIds)
    }

    protected Map<Integer, CollectorNode> getMap(int id1) {
        Map map = maps.get(id1)

        if (map != null) return map

        map = new HashMap(sizeIds)

        maps.put(id1, map)

        return map
    }

    protected CollectorNode getNode(int id1, id2) {
        CollectorNode node = getMap(id1).get(id2)

        if (node != null) return node

        node = new CollectorNode(id2, maxType)

        return node
    }

    int increment(int id1, int id2, int type = 0, float amount = 1.0) {
        getNode(id1, id2).increment(type, amount)

        if (id1 != id2) // enforce symmetry
            getNode(id2, id1).increment(type, amount)
    }

    /**
     * Returns a sorted list of associations for this id
     */
    def getAssociations(int id1, float[] weights = null) {
        Map<Integer, CollectorNode> map = getMap(id1)

        return map.values().sort{ a, b -> a.weightedValue(weights) <=> b.weightedValue(weights) }
    }
}

class CollectorNode {
    int id2
    float[] values

    CollectorNode(int id2, int maxType) {
        this.id2 = id2

        values = new Integer[maxType]

        for (int i = 0; i < maxType; ++i) {
            values[i] = 0.0
        }
    }

    int increment(float amount, int type = 0) {
        return (values[type] += amount)
    }

    float weightedValue(float[] weights) {
        float weightedSum = 0.0

        if (weights == null) {
            for (int i = 0; i < values.length; ++i) {
                weightedSum += values[i]
            }
        } else for (int i = 0; i < values.length; ++i) {
            weightedSum += weights[i] * values[i]
        }

        return weightedSum
    }
}