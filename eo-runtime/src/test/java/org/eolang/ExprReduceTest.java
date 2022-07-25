/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2022 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.eolang;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for VarargsExpr.
 * @since 1.0
 */
class ExprReduceTest {
    @Test
    void exprTest() throws Exception {
        final ExprReduce<Long> expr = new ExprReduce<>(
            "plus",
            "x",
            Long.class,
            Long::sum);
        Phi phi = new Data.ToPhi(100L);
        phi = phi.attr("plus").get();
        phi = new PhWith(phi, 0, new Data.ToPhi(10L));
        phi = new PhWith(phi, 1, new Data.ToPhi(20L));
        phi = new PhWith(phi, 2, new Data.ToPhi(-5L));
        MatcherAssert.assertThat(
            new Dataized(expr.get(phi)).take(Long.class),
            Matchers.equalTo(125L)
        );
    }

    @Test
    void wrongTypeTest() throws Exception {
        final ExprReduce<Long> expr = new ExprReduce<>(
            "plus",
            "x",
            Long.class,
            Long::sum);
        Phi phi = new Data.ToPhi(100L);
        phi = phi.attr("plus").get();
        phi = new PhWith(phi, 0, new Data.ToPhi(10L));
        phi = new PhWith(phi, 1, new Data.ToPhi(20.0));
        phi = new PhWith(phi, 2, new Data.ToPhi(-5L));
        final Phi ret = phi;
        final ExError error = Assertions.assertThrows(
            ExError.class,
            () -> new Dataized(ret).take(String.class)
        );
        MatcherAssert.assertThat(
            new Dataized(error.enclosure()).take(String.class),
            Matchers.equalTo("The 2th argument of 'int.plus' is not a(n) Long: 20.0")
        );
    }
}