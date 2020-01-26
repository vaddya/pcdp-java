# Task Parallelism

## Task Creation and Termination (Async, Finish)

Now the whole idea behind parallel programming for multicore processors 
is to determine which of these steps can run in parallel with each 
other. And how their parallelism should be coordinated. 

```
finish {
    async {
        sum1 = sum of lower half
    }
    sum2 = sum of upper half
}
sum = sum1 + sum2
```

So if you look at the principle over here, we can take any sequential 
algorithm and prefix it by asyncs wherever we see opportunities for 
parallelism.

## Tasks in Java's Fork/Join Framework

Async and finish are general parallel programming pedagogic concepts. 
But now we need to figure out how to implement them using what's 
available to us in Java. And we will learn about the Java Fork/Join 
Framework, which is one of the most popular ways of exploiting 
multi-core parallelism in Java today.

```java
class ReciprocalArraySumTask extends RecursiveAction {
    private static final int THRESHOLD = 1000;
    private final int startIndexInclusive;
    private final int endIndexExclusive;
    private final double[] input;
    private double value;

    ReciprocalArraySumTask(final int setStartIndexInclusive,
                           final int setEndIndexExclusive,
                           final double[] setInput) {
        this.startIndexInclusive = setStartIndexInclusive;
        this.endIndexExclusive = setEndIndexExclusive;
        this.input = setInput;
    }

    public double getValue() {
        return value;
    }

    @Override
    protected void compute() {
        if (endIndexExclusive < startIndexInclusive) {
            value = 0;
        } else if (endIndexExclusive == startIndexInclusive) {
            value = input[startIndexInclusive];
        } else if (endIndexExclusive - startIndexInclusive < THRESHOLD) {
            for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                value += 1 / input[i];
            }
        } else {
            final int mid = startIndexInclusive + (endIndexExclusive - startIndexInclusive) / 2;
            final ReciprocalArraySumTask left = new ReciprocalArraySumTask(startIndexInclusive, mid, input);
            final ReciprocalArraySumTask right = new ReciprocalArraySumTask(mid, endIndexExclusive, input);

            left.fork(); // like async
            right.compute();
            left.join(); // like finish

            value = left.getValue() + right.getValue();
        }
    }
}
```

Now, this pattern of async, L dot compute, and R dot compute is a very 
common pattern in multi-core programming. So in the Java ForkJoin 
Framework they have another useful API called "invoke-all".

```java
final ReciprocalArraySumTask left = new ReciprocalArraySumTask(startIndexInclusive, mid, input);
final ReciprocalArraySumTask right = new ReciprocalArraySumTask(mid, endIndexExclusive, input);

ForkJoinTask.invokeAll(left, right); // instead of fork/join

value = left.getValue() + right.getValue()
```

## Computation Graphs, Work, Span

Now we want to introduce an interesting concept called the computation 
graph to model parallel programs such as this. And the idea is we think 
in terms of the program executing dynamically.

```
S1
finish {
    async {
        S2
    }
    S3
}
$4
```

This graph is purely a mental abstraction. It does not actually get 
constructed or built when your program is running. But logically, we 
can think of statement S1 executing. And then after that, it does a 
fork. So we have a fork over here of S2. And immediately after the fork, 
S1 can continue on to S3.

```
S1 __continue__> S3 __continue__> S4
|                                 ^
\_____fork_____> S2 ____join______/
```

Each vertex or node of this directed graph represents a sequential 
subcomputation, something we refer to as a step. And each edge refers 
to an ordering constraint. If you just had a normal sequential program 
with no fork and join, our graph would just be a straight line with 
continue edges. But with parallelism, we see we have these fork edges 
and the join edges. 

If you want to reason about which statements can execute in parallel, 
we ask "Is there a path of directed edges from one statement to 
another?". We can see that between S2 and S3 there's a parallel 
execution that's possible, because there's no path of directed edges 
between S2 and S3.

Another very interesting property of computation graphs is that we can 
use them to reason about the performance of your parallel program. So 
let's just say in abstract time units. There are two important metrics 
that we will work with to reason about the performance:

* __Work__: that's simply the sum of the execution times of all the 
nodes. 
* __Span__: that's the length of the longest path or critical path 
length.

And these two metrics help us reason about the parallelism in the 
program:
* __Ideal parallelism__ = Work / Span.

For a sequential program, it would just be 1, because the span would be 
the same as the work. But what we will see is for a rich set of parallel 
algorithms, the ideal parallelism can be really large.

